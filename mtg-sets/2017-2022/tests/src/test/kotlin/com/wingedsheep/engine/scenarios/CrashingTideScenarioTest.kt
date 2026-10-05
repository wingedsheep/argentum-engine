package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Crashing Tide (RIX #34) — {2}{U} Sorcery.
 *
 *   This spell has flash as long as you control a Merfolk.
 *   Return target creature to its owner's hand.
 *   Draw a card.
 */
class CrashingTideScenarioTest : ScenarioTestBase() {

    private fun canCastFromHand(game: TestGame, playerNumber: Int, cardName: String): Boolean {
        val cardIds = game.findCardsInHand(playerNumber, cardName).toSet()
        return game.getLegalActions(playerNumber).any { (it.action as? CastSpell)?.cardId in cardIds }
    }

    init {
        context("Crashing Tide") {

            test("without a Merfolk it is sorcery-speed on the opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Crashing Tide")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                canCastFromHand(game, 1, "Crashing Tide") shouldBe false
            }

            test("controlling a Merfolk grants flash; it bounces the target and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Crashing Tide")
                    .withCardOnBattlefield(1, "Merfolk of the Pearl Trident", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("a Merfolk gives the sorcery flash on the opponent's turn") {
                    canCastFromHand(game, 1, "Crashing Tide") shouldBe true
                }

                val giant = game.findPermanent("Hill Giant")!!
                val handBefore = game.handSize(1)
                game.castSpell(1, "Crashing Tide", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant returned to its owner's hand") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInHand(2, "Hill Giant") shouldBe true
                }
                withClue("caster drew a card (cast one, drew one)") {
                    game.handSize(1) shouldBe handBefore
                }
                game.isInGraveyard(1, "Crashing Tide") shouldBe true
            }

            test("an illegal target on resolution means no card is drawn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Crashing Tide")
                    .withCardInHand(2, "Crashing Tide")
                    .withCardOnBattlefield(1, "Merfolk of the Pearl Trident", summoningSickness = false)
                    .withCardOnBattlefield(2, "Merfolk of the Pearl Trident", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Crashing Tide", targetId = giant).error shouldBe null
                val handAfterCast = game.handSize(1)

                // Opponent responds at instant speed (they control a Merfolk) and bounces their own Giant.
                game.passPriority()
                game.castSpell(2, "Crashing Tide", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("Player 1's Crashing Tide fizzled — no card drawn") {
                    game.handSize(1) shouldBe handAfterCast
                }
                game.isInHand(2, "Hill Giant") shouldBe true
            }
        }
    }
}
