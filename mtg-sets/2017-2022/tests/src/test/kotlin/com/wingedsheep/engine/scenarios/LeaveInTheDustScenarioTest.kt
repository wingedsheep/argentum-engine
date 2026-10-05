package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Leave in the Dust (AER #37) — {3}{U} Instant.
 *
 *   Return target nonland permanent to its owner's hand.
 *   Draw a card.
 */
class LeaveInTheDustScenarioTest : ScenarioTestBase() {

    init {
        context("Leave in the Dust") {

            test("bounces a nonland permanent to its owner's hand and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Leave in the Dust")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val handBefore = game.handSize(1)
                game.castSpell(1, "Leave in the Dust", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant returned to its owner's hand") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInHand(2, "Hill Giant") shouldBe true
                }
                withClue("caster drew a card (cast one, drew one)") {
                    game.handSize(1) shouldBe handBefore
                }
                game.isInGraveyard(1, "Leave in the Dust") shouldBe true
            }

            test("cannot target a land") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Leave in the Dust")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!
                withClue("a land is not a legal target") {
                    game.castSpell(1, "Leave in the Dust", targetId = forest).error shouldNotBe null
                    game.isOnBattlefield("Forest") shouldBe true
                }
            }

            test("an illegal target on resolution means no card is drawn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Leave in the Dust")
                    .withCardInHand(2, "Leave in the Dust")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Leave in the Dust", targetId = giant).error shouldBe null
                val handAfterCast = game.handSize(1)

                // Opponent responds by bouncing their own Giant, so Player 1's spell loses its only target.
                game.passPriority()
                game.castSpell(2, "Leave in the Dust", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("Player 1's Leave in the Dust fizzled — no card drawn") {
                    game.handSize(1) shouldBe handAfterCast
                }
                game.isInHand(2, "Hill Giant") shouldBe true
            }
        }
    }
}
