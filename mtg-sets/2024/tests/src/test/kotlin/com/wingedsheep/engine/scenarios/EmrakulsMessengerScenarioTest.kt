package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Emrakul's Messenger (MH3) — "Whenever you draw your second card each turn, create a 0/1
 * colorless Eldrazi Spawn creature token…"
 */
class EmrakulsMessengerScenarioTest : ScenarioTestBase() {

    init {
        context("Emrakul's Messenger") {
            test("drawing the second and third cards of the turn makes exactly one Spawn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Emrakul's Messenger")
                    .withCardInHand(1, "Divination")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Divination").error shouldBe null
                game.resolveStack()

                withClue("only the second card drawn this turn triggers") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 1
                }
            }

            test("the first draw of the turn and an opponent's second draw do not trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Emrakul's Messenger")
                    .withCardInHand(2, "Divination")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(2, 0)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Divination").error shouldBe null
                game.resolveStack()

                game.findPermanents("Eldrazi Spawn") shouldHaveSize 0
            }
        }
    }
}
