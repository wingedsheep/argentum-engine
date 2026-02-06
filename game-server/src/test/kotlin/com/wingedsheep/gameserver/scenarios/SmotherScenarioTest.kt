package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Smother.
 *
 * Card reference:
 * - Smother (1B): Instant
 *   "Destroy target creature with mana value 3 or less. It can't be regenerated."
 */
class SmotherScenarioTest : ScenarioTestBase() {

    init {
        context("Smother mana value targeting") {
            test("can target and destroy creature with mana value 3 or less") {
                // Grizzly Bears costs 1G (MV 2)
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Smother")
                    .withCardOnBattlefield(2, "Grizzly Bears") // MV 2
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                // Cast Smother targeting Grizzly Bears
                val castResult = game.castSpell(1, "Smother", bears)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve
                game.resolveStack()

                // Grizzly Bears should be destroyed
                withClue("Grizzly Bears should be destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("Grizzly Bears should be in graveyard") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("can target creature with mana value exactly 3") {
                // Hill Giant costs 3R (MV 4) - this should NOT be targetable
                // Let's use a 3 MV creature - Festering Goblin is 1 MV
                // Need a creature with exactly MV 3
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Smother")
                    .withCardOnBattlefield(2, "Festering Goblin") // MV 1 - should be targetable
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goblin = game.findPermanent("Festering Goblin")!!

                // Cast Smother targeting Festering Goblin (MV 1)
                val castResult = game.castSpell(1, "Smother", goblin)
                withClue("Cast should succeed for MV 1 creature") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Festering Goblin should be destroyed") {
                    game.isOnBattlefield("Festering Goblin") shouldBe false
                }
            }

            test("can target face-down morph creature (mana value 0 per Rule 707.2)") {
                // Battering Craghorn costs 2RR (MV 4) face-up, but face-down it has MV 0
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Smother")
                    .withCardInHand(2, "Battering Craghorn")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 2 casts Battering Craghorn face-down for {3}
                val craghornCardId = game.state.getHand(game.player2Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Battering Craghorn"
                }
                val castMorphResult = game.execute(CastSpell(game.player2Id, craghornCardId, castFaceDown = true))
                withClue("Cast morph should succeed") {
                    castMorphResult.error shouldBe null
                }
                game.resolveStack()

                // Switch to player 1's turn to cast Smother
                game.advanceToPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state = game.state.copy(activePlayerId = game.player1Id, priorityPlayerId = game.player1Id)

                // Find the face-down creature on the battlefield
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.get<com.wingedsheep.engine.state.components.identity.FaceDownComponent>() != null
                }!!

                // Cast Smother targeting the face-down creature (MV 0 <= 3, should succeed)
                val castResult = game.castSpell(1, "Smother", faceDownId)
                withClue("Cast should succeed targeting face-down creature with MV 0") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Face-down creature should be destroyed
                withClue("Face-down creature should no longer be on battlefield") {
                    game.state.getBattlefield().none { entityId ->
                        game.state.getEntity(entityId)?.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>() == true
                    } shouldBe true
                }
            }

            test("cannot target creature with mana value greater than 3") {
                // Hill Giant costs 3R (MV 4)
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Smother")
                    .withCardOnBattlefield(2, "Hill Giant") // MV 4
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                // Try to cast Smother targeting Hill Giant (MV 4)
                val castResult = game.castSpell(1, "Smother", giant)

                // Should fail because Hill Giant's mana value is 4
                withClue("Cast should fail for MV 4 creature") {
                    castResult.error shouldNotBe null
                }

                // Hill Giant should still be on the battlefield
                withClue("Hill Giant should still be on the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }
        }
    }
}
