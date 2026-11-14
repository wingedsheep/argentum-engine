package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ori.cards.PiaAndKiranNalaar
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Pia and Kiran Nalaar (ORI #157) — {2}{R}{R} Legendary Creature — Human
 * Artificer, 2/2.
 *
 *   When Pia and Kiran Nalaar enters, create two 1/1 colorless Thopter artifact creature tokens
 *   with flying.
 *   {2}{R}, Sacrifice an artifact: Pia and Kiran Nalaar deals 2 damage to any target.
 */
class PiaAndKiranNalaarScenarioTest : ScenarioTestBase() {

    private val abilityId = PiaAndKiranNalaar.activatedAbilities.single().id

    private fun castPiaAndKiran(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Pia and Kiran Nalaar")
            .withLandsOnBattlefield(1, "Mountain", 7)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val cast = game.castSpell(1, "Pia and Kiran Nalaar")
        withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
        game.resolveStack()
        return game
    }

    init {
        context("Pia and Kiran Nalaar") {

            test("enters with two 1/1 flying Thopter artifact creature tokens") {
                val game = castPiaAndKiran()

                val thopters = game.findPermanents("Thopter Token")
                withClue("two Thopter tokens were created") { thopters.size shouldBe 2 }
                val projected = game.state.projectedState
                thopters.forEach { thopter ->
                    projected.getPower(thopter) shouldBe 1
                    projected.getToughness(thopter) shouldBe 1
                    projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
                    projected.hasType(thopter, "ARTIFACT") shouldBe true
                }
            }

            test("sacrificing a Thopter deals 2 damage to a player") {
                val game = castPiaAndKiran()
                val pia = game.findPermanent("Pia and Kiran Nalaar")!!
                val thopter = game.findPermanents("Thopter Token").first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = pia,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(thopter)),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("the sacrificed Thopter is gone, the other remains") {
                    game.findPermanents("Thopter Token").size shouldBe 1
                }
                withClue("the opponent took 2 damage") { game.getLifeTotal(2) shouldBe 18 }
            }

            test("sacrificing a Thopter deals 2 damage to a creature, killing a 2/2") {
                val game = castPiaAndKiran()
                val pia = game.findPermanent("Pia and Kiran Nalaar")!!
                val thopter = game.findPermanents("Thopter Token").first()
                val bears = game.findPermanent("Grizzly Bears")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = pia,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(thopter)),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Grizzly Bears died") { game.isInGraveyard(2, "Grizzly Bears") shouldBe true }
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
