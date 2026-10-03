package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ori.cards.WhirlerRogue
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Whirler Rogue (ORI #83) — two Thopters on entry, and tapping two artifacts makes a creature
 * unblockable this turn.
 */
class WhirlerRogueScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = WhirlerRogue.activatedAbilities.first().id

        test("entering creates two flying 1/1 Thopter artifact tokens") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Whirler Rogue")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Whirler Rogue").error shouldBe null
            game.resolveStack()

            val thopters = game.state.getZone(game.player1Id, Zone.BATTLEFIELD)
                .filter { game.state.projectedState.hasSubtype(it, "Thopter") }
            thopters.size shouldBe 2
            val projected = game.state.projectedState
            thopters.forEach {
                projected.hasType(it, "ARTIFACT") shouldBe true
                projected.hasKeyword(it, Keyword.FLYING) shouldBe true
                projected.getPower(it) shouldBe 1
            }
        }

        test("tapping two artifacts makes the target creature unblockable") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Whirler Rogue", summoningSickness = true)
                .withCardOnBattlefield(1, "Memnite")
                .withCardOnBattlefield(1, "Sol Ring")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val rogue = game.findPermanent("Whirler Rogue")!!
            val memnite = game.findPermanent("Memnite")!!
            val ring = game.findPermanent("Sol Ring")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            val r = game.execute(
                ActivateAbility(
                    game.player1Id, rogue, abilityId,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(memnite, ring)),
                )
            )
            withClue("${r.error}") { r.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(memnite)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(ring)!!.has<TappedComponent>() shouldBe true
            withClue("the Rogue itself isn't part of the cost") {
                game.state.getEntity(rogue)!!.has<TappedComponent>() shouldBe false
            }
            game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        }

        test("a non-artifact can't pay the tap cost") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Whirler Rogue")
                .withCardOnBattlefield(1, "Memnite")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                ActivateAbility(
                    game.player1Id, game.findPermanent("Whirler Rogue")!!, abilityId,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    costPayment = AdditionalCostPayment(
                        tappedPermanents = listOf(game.findPermanent("Memnite")!!, bears)
                    ),
                )
            ).error shouldNotBe null
        }
    }
}
