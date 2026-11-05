package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cast Out (AKH #8) — {3}{W} Enchantment.
 *
 *   Flash
 *   When this enchantment enters, exile target nonland permanent an opponent controls until this
 *   enchantment leaves the battlefield.
 *   Cycling {W}
 */
class CastOutScenarioTest : ScenarioTestBase() {

    init {
        context("Cast Out") {

            test("flashed in on the opponent's turn, it exiles their creature until it leaves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cast Out")
                    .withCardInHand(1, "Disenchant")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!

                withClue("flash allows casting on the opponent's turn") {
                    game.castSpell(1, "Cast Out").error shouldBe null
                }
                game.resolveStack()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant is exiled") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInExile(2, "Hill Giant") shouldBe true
                }

                // Priority returns to the active player (Player2) after resolution; hand it back.
                if (game.state.priorityPlayerId == game.player2Id) game.passPriority()
                game.castSpell(1, "Disenchant", game.findPermanent("Cast Out")!!).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant returns when Cast Out leaves the battlefield") {
                    game.isOnBattlefield("Cast Out") shouldBe false
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }

            test("cycling {W} draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cast Out")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Cast Out").error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Cast Out") shouldBe true
                game.isInHand(1, "Island") shouldBe true
            }
        }
    }
}
