package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Marang River Regent // Coil and Catch (TDM #51).
 *
 * Marang River Regent — {4}{U}{U} Dragon, 6/7, Flying.
 *   "When this creature enters, return up to two other target nonland permanents to their owners' hands."
 * Coil and Catch — {3}{U} Instant — Omen.
 *   "Draw three cards, then discard a card."
 */
class MarangRiverRegentScenarioTest : ScenarioTestBase() {

    init {
        context("Marang River Regent creature face") {

            test("ETB returns up to two other nonland permanents to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Marang River Regent")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardOnBattlefield(1, "Centaur Courser") // own nonland permanent
                    .withCardOnBattlefield(2, "Phantom Warrior")  // opponent nonland permanent
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Marang River Regent").error shouldBe null
                game.resolveStack() // creature enters → ETB asks for up to two targets

                val mine = game.findPermanent("Centaur Courser")!!
                val theirs = game.findPermanent("Phantom Warrior")!!
                val result = game.selectTargets(listOf(mine, theirs))
                withClue("Two other nonland permanents is legal: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Both targeted permanents are bounced to their owners' hands") {
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                    game.isOnBattlefield("Phantom Warrior") shouldBe false
                    game.findCardsInHand(1, "Centaur Courser").size shouldBe 1
                    game.findCardsInHand(2, "Phantom Warrior").size shouldBe 1
                }
                withClue("Marang River Regent (the source) is still on the battlefield — \"other\"") {
                    game.isOnBattlefield("Marang River Regent") shouldBe true
                }
            }

            test("the same permanent can't be chosen for both of the two targets") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Marang River Regent")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardOnBattlefield(2, "Phantom Warrior")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Marang River Regent").error shouldBe null
                game.resolveStack() // creature enters → ETB asks for up to two targets

                // "up to two other target nonland permanents" is one instance of "target"
                // (CR 601.2c), so the two chosen permanents must be different — picking the same
                // one twice is rejected.
                val warrior = game.findPermanent("Phantom Warrior")!!
                val result = game.selectTargets(listOf(warrior, warrior))
                withClue("Choosing the same permanent twice is illegal: ${result.error}") {
                    result.error shouldNotBe null
                }
            }
        }

        context("Coil and Catch Omen face") {

            test("draw three, discard one, then shuffle the Omen back into the library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Marang River Regent")
                    .withLandsOnBattlefield(1, "Island", 4) // {3}{U}
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Marang River Regent"
                }
                val libraryBefore = game.librarySize(1) // 4

                // Cast the Omen face (faceIndex = 0).
                val cast = game.execute(
                    CastSpell(playerId = game.player1Id, cardId = cardId, faceIndex = 0)
                )
                withClue("Casting Coil and Catch should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack() // draw three, then discard prompt

                if (game.hasPendingDecision()) {
                    val discard = game.state.getHand(game.player1Id).first {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Island"
                    }
                    game.selectCards(listOf(discard))
                    game.resolveStack()
                }

                withClue("One card was discarded to the graveyard") {
                    game.graveyardSize(1) shouldBe 1
                }
                // Library: started at 4, drew 3, then Marang River Regent shuffles back → 4 - 3 + 1 = 2.
                withClue("Drew three and shuffled the Omen back into the library") {
                    game.librarySize(1) shouldBe libraryBefore - 3 + 1
                }
                withClue("Coil and Catch resolved to the library, not the battlefield") {
                    game.findCardsInLibrary(1, "Marang River Regent").size shouldBe 1
                    game.isOnBattlefield("Marang River Regent") shouldBe false
                }
            }
        }
    }
}
