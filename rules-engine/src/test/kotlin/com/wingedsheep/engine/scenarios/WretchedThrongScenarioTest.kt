package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Wretched Throng (VOW #91) — {1}{U} Creature — Zombie Horror, 2/1.
 *
 *   When this creature dies, you may search your library for a card named Wretched Throng,
 *   reveal it, put it into your hand, then shuffle.
 *
 * Exercises the dies trigger: killing the creature offers an optional library search for another
 * copy of itself; accepting puts it into hand, declining leaves the library untouched.
 */
class WretchedThrongScenarioTest : ScenarioTestBase() {

    init {
        context("Wretched Throng dies trigger") {

            test("dying offers an optional search for another Wretched Throng, which can be accepted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wretched Throng", summoningSickness = false)
                    .withCardInLibrary(1, "Wretched Throng")
                    .withCardInLibrary(1, "Forest")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val throng = game.findPermanent("Wretched Throng")!!

                // Kill it with 3 damage (it's a 2/1) so its dies trigger fires.
                game.castSpell(1, "Lightning Bolt", targetId = throng).error shouldBe null
                game.resolveStack()

                withClue("Wretched Throng died") {
                    game.isOnBattlefield("Wretched Throng") shouldBe false
                    game.isInGraveyard(1, "Wretched Throng") shouldBe true
                }

                withClue("the dies trigger offers a search decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // The optional trigger surfaces two sequential decisions: first the "you may
                // search" yes/no gate, then the library-search card selection. Accept the gate,
                // then pick the copy of Wretched Throng out of the library.
                if (game.getPendingDecision() is com.wingedsheep.engine.core.YesNoDecision) {
                    game.answerYesNo(true).error shouldBe null
                }
                val selection = game.getPendingDecision() as? com.wingedsheep.engine.core.SelectCardsDecision
                withClue("the search offers the library copy of Wretched Throng") {
                    selection shouldNotBe null
                }
                val libraryCopy = selection!!.options.first()
                game.selectCards(listOf(libraryCopy)).error shouldBe null
                game.resolveStack()

                withClue("the found copy of Wretched Throng is now in hand") {
                    game.isInHand(1, "Wretched Throng") shouldBe true
                }
            }

            test("declining the search leaves the library copy in place") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wretched Throng", summoningSickness = false)
                    .withCardInLibrary(1, "Wretched Throng")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val throng = game.findPermanent("Wretched Throng")!!
                game.castSpell(1, "Lightning Bolt", targetId = throng).error shouldBe null
                game.resolveStack()

                if (game.hasPendingDecision()) {
                    val decision = game.getPendingDecision()
                    if (decision is com.wingedsheep.engine.core.SelectCardsDecision) {
                        game.selectCards(emptyList())
                    } else {
                        game.answerYesNo(false)
                    }
                }
                game.resolveStack()

                withClue("declining leaves the copy out of hand") {
                    game.isInHand(1, "Wretched Throng") shouldBe false
                }
            }
        }
    }
}
