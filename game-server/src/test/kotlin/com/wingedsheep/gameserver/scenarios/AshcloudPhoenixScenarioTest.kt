package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Ashcloud Phoenix.
 *
 * Ashcloud Phoenix ({2}{R}{R}, 4/1 Flying Phoenix)
 * When Ashcloud Phoenix dies, return it to the battlefield face down under your control.
 * Morph {4}{R}{R}
 * When Ashcloud Phoenix is turned face up, it deals 2 damage to each player.
 */
class AshcloudPhoenixScenarioTest : ScenarioTestBase() {

    init {
        context("Ashcloud Phoenix dies trigger") {

            test("returns to battlefield face down when destroyed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ashcloud Phoenix")
                    .withCardInHand(2, "Smite the Monstrous")
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val phoenixId = game.findPermanent("Ashcloud Phoenix")
                phoenixId shouldNotBe null

                // Destroy the phoenix with Smite the Monstrous
                game.castSpell(2, "Smite the Monstrous", phoenixId)
                game.resolveStack()

                // The dies trigger should be on the stack — resolve it
                game.resolveStack()

                // Phoenix should NOT be in the graveyard
                val graveyardNames = game.findCardsInGraveyard(1, "Ashcloud Phoenix")
                graveyardNames.size shouldBe 0

                // A face-down creature should be on the battlefield
                val faceDownCreature = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                faceDownCreature shouldNotBe null

                // It should have MorphDataComponent so it can be turned face up
                val morphData = game.state.getEntity(faceDownCreature!!)?.get<MorphDataComponent>()
                morphData shouldNotBe null
                morphData!!.originalCardDefinitionId shouldBe "Ashcloud Phoenix"
            }

            test("deals 2 damage to each player when turned face up") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ashcloud Phoenix")
                    .withCardInHand(2, "Smite the Monstrous")
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withLandsOnBattlefield(1, "Mountain", 10) // Enough to pay morph cost {4}{R}{R}
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife1 = game.getLifeTotal(1)
                val startingLife2 = game.getLifeTotal(2)

                // Destroy the phoenix
                game.castSpell(2, "Smite the Monstrous", game.findPermanent("Ashcloud Phoenix"))
                game.resolveStack()

                // Resolve the dies trigger — returns face down
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                faceDownId shouldNotBe null

                // Pass to player 1's turn to turn face up
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Turn face up by paying morph cost {4}{R}{R}
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                turnUpResult.error shouldBe null

                // Resolve the "deals 2 damage to each player" trigger
                game.resolveStack()

                // Both players should have taken 2 damage
                game.getLifeTotal(1) shouldBe startingLife1 - 2
                game.getLifeTotal(2) shouldBe startingLife2 - 2

                // Phoenix should now be face up as a 4/1
                val entity = game.state.getEntity(faceDownId)
                entity?.has<FaceDownComponent>() shouldBe false
                entity?.get<CardComponent>()?.name shouldBe "Ashcloud Phoenix"
            }

        }
    }
}
