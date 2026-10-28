package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Relic Vial (ZNR #250) — {3} Artifact.
 *
 *   {2}, {T}, Sacrifice a creature: Draw a card.
 *   As long as you control a Cleric, this artifact has "Whenever a creature you control dies, each
 *   opponent loses 1 life and you gain 1 life."
 *
 * The granted ability is a leaves-the-battlefield ability, so whether the Vial has it is decided
 * by the state just before the death (CR 603.10a) — the 2020-09-25 rulings: sacrificing the only
 * Cleric to the Vial's own cost still drains, and so does a wipe that takes the Vial with it.
 */
class RelicVialScenarioTest : ScenarioTestBase() {

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    private fun board(vararg creatures: String, spell: String? = null) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Relic Vial")
        .apply { creatures.forEach { withCardOnBattlefield(1, it) } }
        .withLandsOnBattlefield(1, "Plains", 6)
        .apply { if (spell != null) withCardInHand(1, spell) }
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.sacrificeToVial(creature: String) {
        val abilityId = cardRegistry.getCard("Relic Vial")!!.activatedAbilities.first().id
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Relic Vial")!!,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(findPermanent(creature)!!)),
            )
        )
        withClue("activation: ${result.error}") { result.error shouldBe null }
        payIfAsked()
        resolveStack()
    }

    init {
        context("Relic Vial") {

            test("sacrificing a non-Cleric while you control a Cleric draws and drains") {
                val game = board("Kitesail Cleric", "Grizzly Bears")
                val hand = game.handSize(1)

                game.sacrificeToVial("Grizzly Bears")

                game.handSize(1) shouldBe hand + 1
                game.getLifeTotal(1) shouldBe 21
                game.getLifeTotal(2) shouldBe 19
            }

            test("without a Cleric the Vial only draws") {
                val game = board("Grizzly Bears")
                val hand = game.handSize(1)

                game.sacrificeToVial("Grizzly Bears")

                game.handSize(1) shouldBe hand + 1
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }

            test("sacrificing the only Cleric you control still drains (ruling)") {
                val game = board("Kitesail Cleric")

                game.sacrificeToVial("Kitesail Cleric")

                game.isInGraveyard(1, "Kitesail Cleric") shouldBe true
                game.getLifeTotal(1) shouldBe 21
                game.getLifeTotal(2) shouldBe 19
            }

            test("Wrath of God taking the only Cleric drains once per creature you controlled") {
                val game = board("Kitesail Cleric", "Grizzly Bears", spell = "Wrath of God")

                game.castSpell(1, "Wrath of God").error shouldBe null
                game.payIfAsked()
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 22
                game.getLifeTotal(2) shouldBe 18
            }

            test("the Vial leaving alongside each Cleric still drains for them (ruling)") {
                val game = board("Kitesail Cleric", "Grizzly Bears", spell = "Akroma's Vengeance")

                game.castSpell(1, "Akroma's Vengeance").error shouldBe null
                game.payIfAsked()
                game.resolveStack()

                game.isInGraveyard(1, "Relic Vial") shouldBe true
                game.getLifeTotal(1) shouldBe 22
                game.getLifeTotal(2) shouldBe 18
            }
        }
    }
}
