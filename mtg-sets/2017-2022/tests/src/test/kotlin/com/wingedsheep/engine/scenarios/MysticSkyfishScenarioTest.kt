package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mystic Skyfish (M21) — {2}{U} 3/1 Fish.
 * "Whenever you draw your second card each turn, this creature gains flying until end of turn."
 */
class MysticSkyfishScenarioTest : ScenarioTestBase() {

    private fun hasFlying(game: TestGame): Boolean {
        val id = game.findPermanent("Mystic Skyfish") ?: error("Mystic Skyfish not on battlefield")
        return StateProjector().project(game.state).hasKeyword(id, Keyword.FLYING)
    }

    init {
        context("Mystic Skyfish's second-draw trigger") {

            test("drawing a second card in a turn grants flying until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mystic Skyfish")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                hasFlying(game) shouldBe false

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()

                withClue("second draw of the turn should grant flying") {
                    hasFlying(game) shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
                withClue("flying should wear off at end of turn") {
                    hasFlying(game) shouldBe false
                }
            }

            test("the first card drawn in a turn does not grant flying; the second one does") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mystic Skyfish")
                    .withCardInHand(1, "Think Twice")
                    .withCardInHand(1, "Think Twice")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardsDrawnThisTurn(1, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Think Twice").error shouldBe null
                game.resolveStack()
                withClue("the first draw of the turn should not trigger") {
                    hasFlying(game) shouldBe false
                }

                game.castSpell(1, "Think Twice").error shouldBe null
                game.resolveStack()
                withClue("the second single draw should grant flying") {
                    hasFlying(game) shouldBe true
                }
            }

            test("draws after the second card of the turn do not grant flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mystic Skyfish")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()

                withClue("third and fourth draws should not trigger") {
                    hasFlying(game) shouldBe false
                }
            }
        }
    }
}
