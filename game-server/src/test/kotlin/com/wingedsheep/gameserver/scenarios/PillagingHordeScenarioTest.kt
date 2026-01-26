package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Pillaging Horde's ETB sacrifice trigger with random discard.
 *
 * Card reference:
 * - Pillaging Horde (2RR): 5/5
 *   "When Pillaging Horde enters the battlefield, sacrifice it unless you
 *   discard a card at random."
 */
class PillagingHordeScenarioTest : ScenarioTestBase() {

    init {
        context("Pillaging Horde ETB trigger") {
            test("survives when player chooses to discard at random") {
                // Setup: Player 1 has Pillaging Horde and a card in hand
                val game = scenario()
                    .withPlayers("HordePlayer", "Opponent")
                    .withCardInHand(1, "Pillaging Horde")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Pillaging Horde
                val castResult = game.castSpell(1, "Pillaging Horde")
                withClue("Pillaging Horde should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell and the ETB trigger
                game.resolveStack()

                // There should be a pending yes/no decision
                withClue("There should be a pending decision") {
                    game.hasPendingDecision() shouldBe true
                }

                val decision = game.getPendingDecision()
                withClue("Decision should be a yes/no prompt") {
                    decision shouldNotBe null
                    decision!!.prompt shouldBe "Discard a card at random to keep Pillaging Horde?"
                }

                // Choose yes to discard at random
                val yesResult = game.answerYesNo(true)
                withClue("Yes choice should succeed") {
                    yesResult.error shouldBe null
                }

                // Verify results
                withClue("Pillaging Horde should be on the battlefield (survived)") {
                    game.isOnBattlefield("Pillaging Horde") shouldBe true
                }

                withClue("Grizzly Bears should be in graveyard (discarded)") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                withClue("Pillaging Horde should NOT be in graveyard") {
                    game.isInGraveyard(1, "Pillaging Horde") shouldBe false
                }
            }

            test("is sacrificed when player declines to discard") {
                // Setup: Player has Pillaging Horde and cards in hand
                val game = scenario()
                    .withPlayers("HordePlayer", "Opponent")
                    .withCardInHand(1, "Pillaging Horde")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve
                game.castSpell(1, "Pillaging Horde")
                game.resolveStack()

                withClue("There should be a pending decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose no (decline to discard)
                game.answerYesNo(false)

                // Verify Pillaging Horde was sacrificed
                withClue("Pillaging Horde should NOT be on battlefield (sacrificed)") {
                    game.isOnBattlefield("Pillaging Horde") shouldBe false
                }

                withClue("Pillaging Horde should be in graveyard") {
                    game.isInGraveyard(1, "Pillaging Horde") shouldBe true
                }

                withClue("Grizzly Bears should still be in hand (not discarded)") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }

            test("is automatically sacrificed when player has no cards in hand") {
                // Setup: Player has Pillaging Horde but no other cards in hand
                val game = scenario()
                    .withPlayers("HordePlayer", "Opponent")
                    .withCardInHand(1, "Pillaging Horde")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve
                game.castSpell(1, "Pillaging Horde")
                game.resolveStack()

                // With no cards in hand, the Horde should be automatically sacrificed
                // (no decision needed)
                withClue("There should be no pending decision when hand is empty") {
                    game.hasPendingDecision() shouldBe false
                }

                // Verify Pillaging Horde was sacrificed
                withClue("Pillaging Horde should NOT be on battlefield") {
                    game.isOnBattlefield("Pillaging Horde") shouldBe false
                }

                withClue("Pillaging Horde should be in graveyard") {
                    game.isInGraveyard(1, "Pillaging Horde") shouldBe true
                }
            }

            test("discards a random card when player has multiple cards") {
                // Setup: Player has Pillaging Horde and multiple cards in hand
                val game = scenario()
                    .withPlayers("HordePlayer", "Opponent")
                    .withCardInHand(1, "Pillaging Horde")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Mountain")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)
                withClue("Initial hand should have 4 cards") {
                    initialHandSize shouldBe 4
                }

                // Cast and resolve
                game.castSpell(1, "Pillaging Horde")
                game.resolveStack()

                // Choose to discard
                game.answerYesNo(true)

                // Verify one card was discarded (random, so we just check counts)
                withClue("Pillaging Horde should survive") {
                    game.isOnBattlefield("Pillaging Horde") shouldBe true
                }

                withClue("Hand size should be reduced by 1 (one random card discarded)") {
                    game.handSize(1) shouldBe 2  // 4 - 1 (cast) - 1 (discarded)
                }

                withClue("Graveyard should have exactly 1 card") {
                    game.graveyardSize(1) shouldBe 1
                }
            }
        }
    }
}
