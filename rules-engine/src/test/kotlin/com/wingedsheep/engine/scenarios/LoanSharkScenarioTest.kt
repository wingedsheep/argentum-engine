package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.CastSpellRecord
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Loan Shark (OTJ #55) — {3}{U} 3/4 Creature — Shark Rogue.
 *
 *   When this creature enters, if you've cast two or more spells this turn, draw a card.
 *   Plot {3}{U}
 *
 * The enters trigger is an intervening-if (CR 603.4) gated on
 * YouCastSpellsThisTurn(atLeast = 2). The cast of Loan Shark itself counts as a spell, so
 * the draw happens only when at least one other spell was cast earlier in the turn.
 */
class LoanSharkScenarioTest : ScenarioTestBase() {

    private fun seedPriorSpell(game: TestGame) {
        game.state = game.state.copy(
            spellsCastThisTurnByPlayer = mapOf(
                game.player1Id to listOf(
                    CastSpellRecord(
                        typeLine = TypeLine.parse("Instant"),
                        manaValue = 1,
                        colors = emptySet(),
                        isFaceDown = false,
                        castFromZone = Zone.HAND,
                    )
                )
            )
        )
    }

    init {
        context("Loan Shark enters trigger") {

            test("draws a card when a second spell was cast this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Loan Shark")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // One spell already cast this turn; casting Loan Shark makes it the second.
                seedPriorSpell(game)

                val handBefore = game.handSize(1)
                game.castSpell(1, "Loan Shark").error shouldBe null
                game.resolveStack()

                withClue("Loan Shark (2nd spell this turn) enters and draws a card") {
                    // -1 Loan Shark leaves hand, +1 drawn → net unchanged.
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("does not draw when Loan Shark is the only spell cast this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Loan Shark")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Loan Shark").error shouldBe null
                game.resolveStack()

                withClue("Loan Shark (only spell this turn) enters but draws nothing") {
                    // -1 Loan Shark leaves hand, no draw.
                    game.handSize(1) shouldBe (handBefore - 1)
                }
            }
        }
    }
}
