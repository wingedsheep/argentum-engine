package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.engine.core.*
import com.wingedsheep.sdk.core.Phase
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.collections.shouldContain
import kotlin.time.Duration.Companion.seconds

class GameFlowTest : GameServerTestBase() {

    private fun findPlayableLand(legalActions: List<LegalActionInfo>): PlayLand? {
        return legalActions
            .filter { it.actionType == "PlayLand" }
            .map { it.action as PlayLand }
            .firstOrNull()
    }

    private suspend fun GameContext.safePassPriority() {
        val countBefore1 = player1.client.stateUpdateCount()

        // Check if either player has a pending decision (e.g., discard to hand size at cleanup)
        // Decisions are only visible to the player who must resolve them
        val p1Decision = player1.client.messages.filterIsInstance<ServerMessage.StateUpdate>().lastOrNull()?.pendingDecision
        val p2Decision = player2.client.messages.filterIsInstance<ServerMessage.StateUpdate>().lastOrNull()?.pendingDecision
        val pendingDecision = p1Decision ?: p2Decision

        if (pendingDecision is SelectCardsDecision) {
            val selected = pendingDecision.options.take(pendingDecision.minSelections)
            val decisionPlayer = if (pendingDecision.playerId == player1.id) player1 else player2
            decisionPlayer.client.send(
                ClientMessage.SubmitAction(
                    SubmitDecision(pendingDecision.playerId, CardsSelectedResponse(pendingDecision.id, selected))
                )
            )
        } else {
            val state = player1.client.requireLatestState()
            val priorityPlayer = if (state.priorityPlayerId == player1.id) player1 else player2
            priorityPlayer.client.send(ClientMessage.SubmitAction(PassPriority(priorityPlayer.id)))
        }

        eventually(5.seconds) {
            player1.client.stateUpdateCount() shouldBeGreaterThan countBefore1
        }
    }

    init {
        context("Playing Lands") {
            test("active player can play a land during main phase") {
                val ctx = setupGame(monoGreenLands)
                val active = ctx.activePlayer()

                var currentState = active.client.requireLatestState()
                var limit = 0
                while (currentState.currentPhase != Phase.PRECOMBAT_MAIN && limit++ < 20) {
                    ctx.safePassPriority()
                    currentState = active.client.requireLatestState()
                }

                currentState.currentPhase shouldBe Phase.PRECOMBAT_MAIN

                val hand = currentState.hand(active.id)
                val initialSize = hand.size
                val landId = hand.cardIds.first()

                val actions = active.client.latestLegalActions()
                val playAction = findPlayableLand(actions)
                playAction shouldNotBe null

                active.client.submitAndWait(playAction!!)

                val newState = active.client.requireLatestState()
                newState.battlefield().cardIds shouldContain landId
                newState.hand(active.id).size shouldBe initialSize - 1
            }
        }

        context("Multi-Turn Gameplay") {
            test("turn counter increments correctly") {
                val ctx = setupGame(monoGreenLands)
                var state = ctx.player1.client.requireLatestState()
                state.turnNumber shouldBe 1

                var limit = 0
                while (state.turnNumber == 1 && limit++ < 50) {
                    ctx.safePassPriority()
                    state = ctx.player1.client.requireLatestState()
                }
                state.turnNumber shouldBe 2
            }
        }
    }
}
