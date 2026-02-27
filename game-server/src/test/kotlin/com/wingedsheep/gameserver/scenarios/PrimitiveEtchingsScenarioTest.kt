package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class PrimitiveEtchingsScenarioTest : ScenarioTestBase() {

    init {
        context("Primitive Etchings") {
            // Library ordering: withCardInLibrary appends to end of list.
            // drawCards draws from first() (index 0 = top).
            // So the FIRST card added is the top of library.

            test("reveals creature card on first draw and draws an extra card") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Primitive Etchings")
                    // Library: Grizzly Bears on top (drawn first), then fillers
                    .withCardInLibrary(1, "Grizzly Bears") // top - creature!
                    .withCardInLibrary(1, "Forest") // second - drawn by trigger
                    .withCardInLibrary(1, "Forest") // third - filler
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .withTurnNumber(2)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val handSizeBefore = game.handSize(1)

                // Pass through upkeep to reach draw step
                // Draw step draws Grizzly Bears (creature) -> triggers reveal -> draws extra card (Forest)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Player should have drawn 2 cards: 1 from draw step + 1 from Primitive Etchings trigger
                game.handSize(1) shouldBe handSizeBefore + 2
            }

            test("reveals non-creature card on first draw and does not draw extra") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Primitive Etchings")
                    // Library: non-creature on top
                    .withCardInLibrary(1, "Forest") // top - land, not a creature
                    .withCardInLibrary(1, "Forest") // filler
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .withTurnNumber(2)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val handSizeBefore = game.handSize(1)

                // Pass through upkeep to draw step
                // Draws Forest (not a creature) -> reveal but no extra draw
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Player should have drawn only 1 card (normal draw step)
                game.handSize(1) shouldBe handSizeBefore + 1
            }

            test("does not trigger on second draw of the turn") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Primitive Etchings")
                    // Library: non-creature on top (first draw), then creature (second)
                    .withCardInLibrary(1, "Forest") // top - non-creature (first draw)
                    .withCardInLibrary(1, "Grizzly Bears") // second - creature, but not first draw
                    .withCardInLibrary(1, "Forest") // filler
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .withTurnNumber(2)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val handSizeBefore = game.handSize(1)

                // Advance past draw step - first draw is Forest (non-creature), no extra draw
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Only 1 card drawn (the normal draw step draw)
                // The creature (Grizzly Bears) is still in library, not drawn
                game.handSize(1) shouldBe handSizeBefore + 1
            }
        }
    }
}
