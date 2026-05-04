package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class WhiskerquillScribeScenarioTest : ScenarioTestBase() {

    init {
        context("Whiskerquill Scribe — Valiant discard-draw") {
            test("declining to discard does not draw a card") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Whiskerquill Scribe")
                    .withCardInHand(1, "Shore Up")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scribeId = game.findPermanent("Whiskerquill Scribe")!!

                // Shore Up targets Whiskerquill Scribe → Valiant trigger fires
                game.castSpell(1, "Shore Up", scribeId)

                // Valiant trigger resolves → "You may discard a card" decision
                game.resolveStack()

                withClue("Valiant should present a yes/no decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Decline the discard
                game.answerYesNo(false)

                // Shore Up resolves
                game.resolveStack()

                // Bug: player was drawing a card even after declining
                withClue("Hand should be empty — no draw when discard is declined") {
                    game.handSize(1) shouldBe 0
                }
            }

            test("accepting discard discards one card and draws one") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Whiskerquill Scribe")
                    .withCardInHand(1, "Shore Up")
                    .withCardInHand(1, "Mountain") // discard fodder
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scribeId = game.findPermanent("Whiskerquill Scribe")!!

                // Hand before cast: [Shore Up, Mountain]
                // Shore Up targets Whiskerquill Scribe → Valiant trigger fires
                // Hand after cast: [Mountain]
                game.castSpell(1, "Shore Up", scribeId)

                // Valiant trigger resolves → yes/no decision
                game.resolveStack()

                // Accept the discard — engine auto-selects Mountain (only card in hand)
                // and immediately draws 1; no separate card-selection step needed
                game.answerYesNo(true)

                // Shore Up resolves
                game.resolveStack()

                withClue("Hand should have 1 card after discard-1-draw-1") {
                    game.handSize(1) shouldBe 1
                }
                withClue("Mountain should be in graveyard after discard") {
                    game.isInGraveyard(1, "Mountain") shouldBe true
                }
            }

            test("accepting discard discards one card and draws one (keep one card in hand)") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Whiskerquill Scribe")
                    .withCardInHand(1, "Shore Up")
                    .withCardInHand(1, "Mountain") // discard fodder
                    .withCardInHand(1, "Plains") // keep
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scribeId = game.findPermanent("Whiskerquill Scribe")!!

                // Hand before cast: [Shore Up, Mountain]
                // Shore Up targets Whiskerquill Scribe → Valiant trigger fires
                // Hand after cast: [Mountain]
                game.castSpell(1, "Shore Up", scribeId)

                // Valiant trigger resolves → yes/no decision
                game.resolveStack()

                // Accept the discard — engine auto-selects Mountain (only card in hand)
                // and immediately draws 1; no separate card-selection step needed
                game.answerYesNo(true)

                // Select Mountain to discard
                val mountainIds = game.findCardsInHand(1, "Mountain")
                withClue("Mountain should be in hand for discard selection") {
                    mountainIds.isNotEmpty() shouldBe true
                }
                game.selectCards(mountainIds)
                // Shore Up resolves
                game.resolveStack()

                withClue("Hand should have 2 cards after discard-1-draw-1") {
                    game.handSize(1) shouldBe 2
                }
                withClue("Mountain should be in graveyard after discard") {
                    game.isInGraveyard(1, "Mountain") shouldBe true
                }
            }

            test("accepting discard with empty hand") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Whiskerquill Scribe")
                    .withCardInHand(1, "Shore Up")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scribeId = game.findPermanent("Whiskerquill Scribe")!!

                // Hand before cast: [Shore Up] — after cast, hand is empty
                game.castSpell(1, "Shore Up", scribeId)

                // Valiant trigger resolves → yes/no decision
                game.resolveStack()

                // Accept the discard — but hand is empty, so nothing is discarded
                // and the "if you do" draw must not fire
                game.answerYesNo(true)

                // Shore Up resolves
                game.resolveStack()

                withClue("No draw when nothing was discarded") {
                    game.handSize(1) shouldBe 0
                }
                withClue("Shore Up in graveyard after resolving, nothing else") {
                    game.graveyardSize(1) shouldBe 1
                }
            }


        }
    }
}
