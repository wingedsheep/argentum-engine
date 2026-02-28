package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Exiled Doomsayer.
 *
 * Exiled Doomsayer: {1}{W}
 * Creature — Human Cleric 1/2
 * All morph costs cost {2} more.
 * (This doesn't affect the cost to cast creature spells face down.)
 *
 * Tests verify:
 * - Morph (turn face-up) costs are increased by {2}
 * - Face-down casting cost is NOT affected
 * - Affects both players' morph costs (global effect)
 */
class ExiledDoomsayerScenarioTest : ScenarioTestBase() {

    init {
        context("Exiled Doomsayer increases morph costs") {

            test("morph cost to turn face up is increased by {2}") {
                // Battering Craghorn has morph {1}{R}{R} — with Exiled Doomsayer it should cost {3}{R}{R}
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Exiled Doomsayer")
                    .withCardInHand(1, "Battering Craghorn")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Battering Craghorn face-down for {3} (NOT affected by Exiled Doomsayer)
                val morphCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Battering Craghorn"
                }
                val castResult = game.execute(CastSpell(game.player1Id, morphCardId, castFaceDown = true))
                withClue("Face-down casting should succeed (not affected by Exiled Doomsayer)") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                faceDownId shouldNotBe null

                // Turn face up — with Exiled Doomsayer, morph cost {1}{R}{R} becomes {3}{R}{R}
                // Player has plenty of mana, so this should succeed
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                withClue("Turn face-up should succeed with enough mana") {
                    turnUpResult.error shouldBe null
                }

                // Verify creature is face up
                val container = game.state.getEntity(faceDownId)
                withClue("Creature should be face up") {
                    container?.has<FaceDownComponent>() shouldBe false
                }
            }

            test("cannot turn face up without enough mana due to cost increase") {
                // Whipcorder has morph {W} — with Exiled Doomsayer it costs {2}{W}
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Exiled Doomsayer")
                    .withCardInHand(1, "Whipcorder")
                    .withLandsOnBattlefield(1, "Plains", 4) // 4 Plains: enough for {3} cast + not enough left for {2}{W} morph
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Whipcorder face-down for {3}
                val morphCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                val castResult = game.execute(CastSpell(game.player1Id, morphCardId, castFaceDown = true))
                withClue("Face-down casting should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                faceDownId shouldNotBe null

                // Try to turn face up — morph cost {W} + {2} increase = {2}{W}
                // Player only has 1 Plains untapped (4 - 3 used for casting)
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                withClue("Turn face-up should fail — not enough mana ({2}{W} needed, only 1 Plains available)") {
                    turnUpResult.error shouldNotBe null
                }
            }

            test("face-down casting cost is NOT affected by Exiled Doomsayer") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Exiled Doomsayer")
                    .withCardInHand(1, "Whipcorder")
                    .withLandsOnBattlefield(1, "Plains", 3) // Exactly 3 — enough for normal face-down cost
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val morphCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                val castResult = game.execute(CastSpell(game.player1Id, morphCardId, castFaceDown = true))
                withClue("Face-down casting should still cost {3} (Exiled Doomsayer doesn't affect face-down cost)") {
                    castResult.error shouldBe null
                }
            }

            test("affects opponent's morph costs too (global effect)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Exiled Doomsayer") // P1 controls Exiled Doomsayer
                    .withCardInHand(2, "Whipcorder")
                    .withLandsOnBattlefield(2, "Plains", 4) // 4 Plains for P2
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // P2 casts face-down for {3}
                val morphCardId = game.state.getHand(game.player2Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                val castResult = game.execute(CastSpell(game.player2Id, morphCardId, castFaceDown = true))
                castResult.error shouldBe null
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                faceDownId shouldNotBe null

                // P2 tries to turn face up — morph cost {W} + {2} increase = {2}{W}
                // P2 only has 1 Plains left (4 - 3 used for face-down) — not enough
                val turnUpResult = game.execute(TurnFaceUp(game.player2Id, faceDownId!!))
                withClue("Opponent's morph cost should also be increased — not enough mana") {
                    turnUpResult.error shouldNotBe null
                }
            }
        }
    }
}
