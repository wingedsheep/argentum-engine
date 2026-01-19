package com.wingedsheep.rulesengine.decision

import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId

/**
 * Abstraction for getting player input/decisions during the game.
 *
 * This interface allows the game engine to request decisions from players
 * without knowing the implementation details (could be human input via UI,
 * AI decision making, network protocol, etc.).
 *
 * The interface uses a synchronous request-response model where the engine
 * asks for a decision and waits for the response.
 */
interface PlayerInterface {
    /**
     * Request a decision from a player.
     *
     * @param state The current game state (for context)
     * @param decision The decision that needs to be made
     * @return The player's response to the decision
     */
    fun requestDecision(state: GameState, decision: PlayerDecision): DecisionResponse
}

/**
 * Result of requesting a decision, which may succeed or fail.
 */
sealed interface DecisionResult {
    /**
     * The decision was made successfully.
     */
    data class Success(val response: DecisionResponse) : DecisionResult

    /**
     * The decision request failed (timeout, disconnect, invalid response, etc.).
     */
    data class Failure(val reason: String) : DecisionResult

    /**
     * The player conceded the game instead of making a decision.
     */
    data class Conceded(val playerId: EntityId) : DecisionResult
}

/**
 * Extended interface that supports async decisions and timeouts.
 */
interface AsyncPlayerInterface : PlayerInterface {
    /**
     * Request a decision with a timeout.
     *
     * @param state The current game state
     * @param decision The decision that needs to be made
     * @param timeoutMs Maximum time to wait for a response in milliseconds
     * @return The result of the decision request
     */
    fun requestDecisionWithTimeout(
        state: GameState,
        decision: PlayerDecision,
        timeoutMs: Long
    ): DecisionResult
}

/**
 * A player interface that routes decisions to the appropriate player's interface.
 * Useful for multi-player games where each player may have a different interface.
 */
class MultiPlayerInterface(
    private val playerInterfaces: Map<EntityId, PlayerInterface>
) : PlayerInterface {
    override fun requestDecision(state: GameState, decision: PlayerDecision): DecisionResponse {
        val playerInterface = playerInterfaces[decision.playerId]
            ?: throw IllegalStateException("No interface registered for player ${decision.playerId}")
        return playerInterface.requestDecision(state, decision)
    }

    fun registerPlayer(playerId: EntityId, playerInterface: PlayerInterface): MultiPlayerInterface {
        return MultiPlayerInterface(playerInterfaces + (playerId to playerInterface))
    }

    fun unregisterPlayer(playerId: EntityId): MultiPlayerInterface {
        return MultiPlayerInterface(playerInterfaces - playerId)
    }

    companion object {
        fun empty(): MultiPlayerInterface = MultiPlayerInterface(emptyMap())
    }
}

/**
 * A player interface that uses pre-programmed responses for testing.
 * Responses are consumed in order as decisions are requested.
 */
class ScriptedPlayerInterface(
    private val responses: MutableList<DecisionResponse> = mutableListOf()
) : PlayerInterface {
    private var nextResponseIndex = 0

    override fun requestDecision(state: GameState, decision: PlayerDecision): DecisionResponse {
        if (nextResponseIndex >= responses.size) {
            throw IllegalStateException("No more scripted responses available for decision: ${decision.description}")
        }
        return responses[nextResponseIndex++]
    }

    fun addResponse(response: DecisionResponse) {
        responses.add(response)
    }

    fun addResponses(vararg newResponses: DecisionResponse) {
        responses.addAll(newResponses)
    }

    fun reset() {
        nextResponseIndex = 0
    }

    fun clear() {
        responses.clear()
        nextResponseIndex = 0
    }

    val remainingResponses: Int
        get() = responses.size - nextResponseIndex

    companion object {
        fun withResponses(vararg responses: DecisionResponse): ScriptedPlayerInterface {
            return ScriptedPlayerInterface(responses.toMutableList())
        }
    }
}

/**
 * A player interface that always returns a default/automatic response.
 * Useful for testing basic game flow without specific decisions.
 */
class AutoPlayerInterface(
    private val defaultResponder: (PlayerDecision) -> DecisionResponse = ::defaultResponse
) : PlayerInterface {
    override fun requestDecision(state: GameState, decision: PlayerDecision): DecisionResponse {
        return defaultResponder(decision)
    }

    companion object {
        /**
         * Provides sensible default responses for each decision type.
         */
        fun defaultResponse(decision: PlayerDecision): DecisionResponse = when (decision) {
            is ChooseTargets -> TargetsChoice(decision.decisionId, emptyMap())
            is ChooseAttackers -> AttackersChoice(decision.decisionId, emptyList())
            is ChooseBlockers -> BlockersChoice(decision.decisionId, emptyMap())
            is ChooseDamageAssignmentOrder -> DamageAssignmentOrderChoice(decision.decisionId, decision.blockerIds)
            is ChooseManaPayment -> ManaPaymentChoice(decision.decisionId)
            is YesNoDecision -> YesNoChoice(decision.decisionId, false) // Default to "no"
            is ChooseCards -> CardsChoice(decision.decisionId, decision.cards.take(decision.minCount).map { it.entityId })
            is ChooseOrder<*> -> OrderChoice(decision.decisionId, decision.items.indices.toList())
            is ChooseMode -> ModeChoice(decision.decisionId, listOf(0)) // Choose first mode
            is ChooseNumber -> NumberChoice(decision.decisionId, decision.minimum)
            is PriorityDecision -> PriorityChoice.Pass(decision.decisionId)
            is MulliganDecision -> MulliganChoice.Keep(decision.decisionId)
            is ChooseMulliganBottomCards -> CardsChoice(decision.decisionId, decision.hand.take(decision.cardsToPutOnBottom))
            is SacrificeUnlessDecision -> SacrificeUnlessChoice(decision.decisionId, false) // Default to not paying
        }
    }
}

/**
 * A player interface that delegates to another interface but can intercept/modify decisions.
 * Useful for AI assistance, tutorials, or logging.
 */
class InterceptingPlayerInterface(
    private val delegate: PlayerInterface,
    private val interceptor: (GameState, PlayerDecision, DecisionResponse) -> DecisionResponse = { _, _, response -> response }
) : PlayerInterface {
    override fun requestDecision(state: GameState, decision: PlayerDecision): DecisionResponse {
        val response = delegate.requestDecision(state, decision)
        return interceptor(state, decision, response)
    }
}

/**
 * A player interface that logs all decisions for debugging/replay.
 */
class LoggingPlayerInterface(
    private val delegate: PlayerInterface,
    private val logger: (PlayerDecision, DecisionResponse) -> Unit = { decision, response ->
        println("Decision: ${decision.description} -> $response")
    }
) : PlayerInterface {
    private val _decisionLog = mutableListOf<Pair<PlayerDecision, DecisionResponse>>()

    val decisionLog: List<Pair<PlayerDecision, DecisionResponse>>
        get() = _decisionLog.toList()

    override fun requestDecision(state: GameState, decision: PlayerDecision): DecisionResponse {
        val response = delegate.requestDecision(state, decision)
        _decisionLog.add(decision to response)
        logger(decision, response)
        return response
    }

    fun clearLog() {
        _decisionLog.clear()
    }
}
