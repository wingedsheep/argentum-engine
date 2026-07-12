package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Cruel Witness (VOW #55) — {2}{U}{U} Creature — Bird Horror, 3/3, Flying.
 *
 *   Whenever you cast a noncreature spell, surveil 1.
 *
 * Exercises the noncreature-cast trigger firing a Surveil 1 (a card-selection decision over the
 * top card of the library), and confirms a creature spell does NOT trigger it.
 */
class CruelWitnessScenarioTest : ScenarioTestBase() {

    init {
        context("Cruel Witness") {

            test("casting a noncreature spell triggers surveil 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cruel Witness", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Mountain") // surveil fodder (top card)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                withClue("casting a noncreature spell triggers a surveil 1 look at the top card") {
                    val decision = game.getPendingDecision()
                    decision.shouldBeInstanceOf<SelectCardsDecision>()
                    (decision as SelectCardsDecision).options.size shouldBe 1
                }
                game.skipSelection() // keep the card out of the graveyard...
                // ...which leaves it to be put back on top; the surveil pipeline's "put the rest on
                // top" step (CardOrder.ControllerChooses) then raises a ReorderLibraryDecision.
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()

                withClue("the surveil resolved without further decisions") {
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("does not trigger when casting a creature spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cruel Witness", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("a creature spell must not trigger a surveil") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
