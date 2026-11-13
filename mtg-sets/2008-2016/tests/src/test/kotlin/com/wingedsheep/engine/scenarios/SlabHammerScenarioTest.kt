package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Slab Hammer (BFZ #227) — {2} Artifact — Equipment.
 *
 *   Whenever equipped creature attacks, you may return a land you control to its owner's hand.
 *   If you do, the creature gets +2/+2 until end of turn.
 *   Equip {2}
 *
 * Pins the "if you do" gate across the land choice: returning a land pumps the attacking equipped
 * creature, declining leaves both the lands and the creature alone.
 */
class SlabHammerScenarioTest : ScenarioTestBase() {

    init {
        fun game() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardAttachedTo(1, "Slab Hammer", "Grizzly Bears")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withLandsOnBattlefield(2, "Mountain", 1)
            .apply { repeat(5) { withCardInLibrary(1, "Forest") } }
            .apply { repeat(5) { withCardInLibrary(2, "Mountain") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Slab Hammer's attack trigger") {

            test("returning a land you control gives the equipped attacker +2/+2 until end of turn") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!
                val forests = game.findPermanents("Forest")
                val mountain = game.findPermanent("Mountain")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                game.answerYesNo(true).error shouldBe null
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only lands its controller controls are offered") {
                    decision.options.toSet() shouldBe forests.toSet()
                    decision.options.contains(mountain) shouldBe false
                }
                game.selectCards(listOf(forests.first())).error shouldBe null
                game.resolveStack()

                withClue("a Forest went back to hand and the Bears got +2/+2") {
                    game.isInHand(1, "Forest") shouldBe true
                    game.findPermanents("Forest").size shouldBe 1
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the pump ends at end of turn") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
            }

            test("declining returns no land and gives no bonus") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("both Forests stay and the Bears stay 2/2") {
                    game.findPermanents("Forest").size shouldBe 2
                    game.isInHand(1, "Forest") shouldBe false
                    game.state.projectedState.getPower(bears) shouldBe 2
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
            }
        }
    }
}
