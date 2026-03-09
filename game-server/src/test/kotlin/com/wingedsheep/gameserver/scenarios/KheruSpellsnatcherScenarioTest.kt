package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Scenario tests for Kheru Spellsnatcher.
 *
 * Tests the CounterSpellToExileEffect:
 * - Counters a spell and exiles it instead of putting in graveyard
 * - Grants permanent MayPlayFromExile and PlayWithoutPayingCost
 */
class KheruSpellsnatcherScenarioTest : ScenarioTestBase() {

    private fun TestGame.isInExile(playerNumber: Int, cardName: String): Boolean {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).any { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    private fun TestGame.getExileCards(playerNumber: Int): List<EntityId> {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId)
    }

    init {
        context("Kheru Spellsnatcher counter and exile") {
            test("counters a spell and exiles it with free cast permission") {
                // Player 1 is active, casts Spellsnatcher face-down
                // Then pass turns to Player 2's main phase
                // Player 2 casts a creature, Player 1 responds by morphing
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Kheru Spellsnatcher")
                    .withCardInHand(2, "Alpine Grizzly") // {2}{G} 4/2 creature
                    .withLandsOnBattlefield(1, "Island", 9) // 3 for face-down + 6 for morph
                    .withLandsOnBattlefield(2, "Forest", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Step 1: Player 1 casts Kheru Spellsnatcher face-down for {3}
                val cardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Kheru Spellsnatcher"
                }
                val castResult = game.execute(CastSpell(game.player1Id, cardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                faceDownId.shouldNotBeNull()

                // Step 2: Pass through rest of Player 1's turn to Player 2's main phase
                // First advance past P1's current phase, then to P2's precombat main
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // Verify we're now on Player 2's turn
                withClue("Active player should now be Player 2") {
                    game.state.activePlayerId shouldBe game.player2Id
                }

                // Step 3: Player 2 casts Alpine Grizzly ({2}{G})
                val grizzlyResult = game.castSpell(2, "Alpine Grizzly")
                withClue("Alpine Grizzly should be cast successfully: ${grizzlyResult.error}") {
                    grizzlyResult.error shouldBe null
                }

                // Player 2 passes priority
                game.execute(PassPriority(game.player2Id))

                // Find the spell on the stack
                val spellOnStack = game.state.stack.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Alpine Grizzly"
                }

                // Step 4: Player 1 turns Kheru Spellsnatcher face up (morph cost {4}{U}{U})
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId))
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Triggered ability fires — select the spell on the stack as target
                withClue("Should have a pending decision for target selection") {
                    game.state.pendingDecision shouldNotBe null
                }
                game.selectTargets(listOf(spellOnStack))

                // Resolve the triggered ability
                game.resolveStack()

                // Alpine Grizzly should be in exile, not on battlefield or graveyard
                withClue("Alpine Grizzly should be in Player 2's exile") {
                    game.isInExile(2, "Alpine Grizzly") shouldBe true
                }
                withClue("Alpine Grizzly should NOT be on the battlefield") {
                    game.isOnBattlefield("Alpine Grizzly") shouldBe false
                }
                withClue("Alpine Grizzly should NOT be in Player 2's graveyard") {
                    game.isInGraveyard(2, "Alpine Grizzly") shouldBe false
                }

                // The exiled card should have permanent MayPlayFromExile and PlayWithoutPayingCost
                val exiledId = game.getExileCards(2).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Alpine Grizzly"
                }
                val exiledContainer = game.state.getEntity(exiledId)!!
                withClue("Exiled card should have MayPlayFromExileComponent") {
                    val comp = exiledContainer.get<MayPlayFromExileComponent>()
                    comp.shouldNotBeNull()
                    comp.permanent shouldBe true
                    comp.controllerId shouldBe game.player1Id
                }
                withClue("Exiled card should have PlayWithoutPayingCostComponent") {
                    val comp = exiledContainer.get<PlayWithoutPayingCostComponent>()
                    comp.shouldNotBeNull()
                    comp.permanent shouldBe true
                    comp.controllerId shouldBe game.player1Id
                }
            }
        }
    }
}
