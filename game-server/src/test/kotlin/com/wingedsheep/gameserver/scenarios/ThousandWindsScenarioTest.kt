package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Thousand Winds.
 *
 * Thousand Winds ({4}{U}{U}, 5/6 Flying Elemental)
 * Morph {5}{U}{U}
 * When this creature is turned face up, return all other tapped creatures to their owners' hands.
 */
class ThousandWindsScenarioTest : ScenarioTestBase() {

    init {
        context("Thousand Winds turn-face-up trigger") {

            test("turning face up returns all other tapped creatures to their owners' hands") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Thousand Winds")
                    .withLandsOnBattlefield(1, "Island", 10) // Enough mana for morph + turn face up
                    .withCardOnBattlefield(2, "Alabaster Kirin", tapped = true)
                    .withCardOnBattlefield(2, "Alpine Grizzly", tapped = true)
                    .withCardOnBattlefield(2, "Summit Prowler") // untapped — should NOT be bounced
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Thousand Winds face-down for {3}
                val thousandWindsId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Thousand Winds"
                }
                val castResult = game.execute(CastSpell(game.player1Id, thousandWindsId, castFaceDown = true))
                castResult.error shouldBe null
                game.resolveStack()

                // Verify face-down creature is on the battlefield
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                faceDownId shouldNotBe null

                // Turn face up by paying morph cost {5}{U}{U}
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                turnUpResult.error shouldBe null

                // The triggered ability goes on the stack — resolve it
                game.resolveStack()

                // Tapped creatures should have been returned to hand
                val p2Hand = game.state.getHand(game.player2Id)
                val p2HandNames = p2Hand.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }
                p2HandNames.contains("Alabaster Kirin") shouldBe true
                p2HandNames.contains("Alpine Grizzly") shouldBe true

                // Untapped creature should still be on battlefield
                val battlefieldNames = game.state.getBattlefield().mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                }
                battlefieldNames.contains("Summit Prowler") shouldBe true

                // Thousand Winds itself should still be on battlefield (excludeSelf)
                battlefieldNames.contains("Thousand Winds") shouldBe true
            }

            test("does not return untapped creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Thousand Winds")
                    .withLandsOnBattlefield(1, "Island", 10)
                    .withCardOnBattlefield(1, "Scion of Glaciers") // untapped own creature
                    .withCardOnBattlefield(2, "Summit Prowler") // untapped opponent creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast face-down
                val thousandWindsId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Thousand Winds"
                }
                game.execute(CastSpell(game.player1Id, thousandWindsId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                game.execute(TurnFaceUp(game.player1Id, faceDownId))
                game.resolveStack()

                // No creatures should have been bounced (all untapped)
                val battlefieldNames = game.state.getBattlefield().mapNotNull {
                    game.state.getEntity(it)?.get<CardComponent>()?.name
                }
                battlefieldNames.contains("Scion of Glaciers") shouldBe true
                battlefieldNames.contains("Summit Prowler") shouldBe true
                battlefieldNames.contains("Thousand Winds") shouldBe true
            }

            test("returns own tapped creatures too") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Thousand Winds")
                    .withLandsOnBattlefield(1, "Island", 10)
                    .withCardOnBattlefield(1, "Scion of Glaciers", tapped = true) // own tapped creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val thousandWindsId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Thousand Winds"
                }
                game.execute(CastSpell(game.player1Id, thousandWindsId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                game.execute(TurnFaceUp(game.player1Id, faceDownId))
                game.resolveStack()

                // Own tapped creature should be returned to hand
                val p1Hand = game.state.getHand(game.player1Id)
                val p1HandNames = p1Hand.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }
                p1HandNames.contains("Scion of Glaciers") shouldBe true
            }
        }
    }
}
