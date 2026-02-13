package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Dream Chisel.
 *
 * Dream Chisel: {2}
 * Artifact
 * Face-down creature spells you cast cost {1} less to cast.
 *
 * Cards used:
 * - Dream Chisel ({2}, Artifact)
 * - Whipcorder ({W}{W}, Creature, Morph {W}) — morph creature
 * - Exalted Angel ({4}{W}{W}, Creature, Morph {2}{W}{W}) — morph creature
 */
class DreamChiselScenarioTest : ScenarioTestBase() {

    init {
        context("Dream Chisel reduces face-down spell costs") {

            test("morph costs {2} instead of {3} with Dream Chisel on battlefield") {
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Whipcorder")
                    .withCardOnBattlefield(1, "Dream Chisel")
                    .withLandsOnBattlefield(1, "Plains", 2) // Only 2 mana — enough with Dream Chisel
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Whipcorder face-down — should cost {2} with Dream Chisel
                val whipcorderCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                val result = game.execute(CastSpell(game.player1Id, whipcorderCardId, castFaceDown = true))
                withClue("Cast morph should succeed with 2 mana and Dream Chisel") {
                    result.error shouldBe null
                }
                game.resolveStack()

                // Verify face-down creature is on the battlefield
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }
            }

            test("morph fails with only 2 mana and no Dream Chisel") {
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Whipcorder")
                    .withLandsOnBattlefield(1, "Plains", 2) // Only 2 mana — not enough without Dream Chisel
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val whipcorderCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                val result = game.execute(CastSpell(game.player1Id, whipcorderCardId, castFaceDown = true))
                withClue("Cast morph should fail with only 2 mana and no Dream Chisel") {
                    result.error shouldNotBe null
                }
            }

            test("two Dream Chisels reduce morph cost to {1}") {
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Exalted Angel")
                    .withCardOnBattlefield(1, "Dream Chisel")
                    .withCardOnBattlefield(1, "Dream Chisel")
                    .withLandsOnBattlefield(1, "Plains", 1) // Only 1 mana — enough with two Dream Chisels
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angelCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Exalted Angel"
                }
                val result = game.execute(CastSpell(game.player1Id, angelCardId, castFaceDown = true))
                withClue("Cast morph should succeed with 1 mana and two Dream Chisels") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }
            }

            test("Dream Chisel does not reduce opponent's morph costs") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dream Chisel") // Player 1 controls Dream Chisel
                    .withCardInHand(2, "Whipcorder")          // Player 2 tries to morph
                    .withLandsOnBattlefield(2, "Plains", 2)   // Only 2 mana for opponent
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val whipcorderCardId = game.state.getHand(game.player2Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                val result = game.execute(CastSpell(game.player2Id, whipcorderCardId, castFaceDown = true))
                withClue("Opponent's morph should still cost {3} — Dream Chisel only helps its controller") {
                    result.error shouldNotBe null
                }
            }

            test("three Dream Chisels reduce morph cost to {0}") {
                val game = scenario()
                    .withPlayers("Morpher", "Opponent")
                    .withCardInHand(1, "Whipcorder")
                    .withCardOnBattlefield(1, "Dream Chisel")
                    .withCardOnBattlefield(1, "Dream Chisel")
                    .withCardOnBattlefield(1, "Dream Chisel")
                    // No lands at all — morph should be free
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val whipcorderCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Whipcorder"
                }
                val result = game.execute(CastSpell(game.player1Id, whipcorderCardId, castFaceDown = true))
                withClue("Cast morph should succeed for free with three Dream Chisels") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }
            }
        }
    }
}