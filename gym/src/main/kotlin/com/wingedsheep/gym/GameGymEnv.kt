package com.wingedsheep.gym

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.ActionParameterizer
import com.wingedsheep.engine.core.ActionParams
import com.wingedsheep.gym.contract.ActionRegistry
import com.wingedsheep.gym.contract.ObservationBuilder
import com.wingedsheep.gym.contract.ObservationResult
import com.wingedsheep.gym.contract.ResolvedAction
import com.wingedsheep.gym.contract.StateDigest
import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.gym.service.SnapshotCodec
import com.wingedsheep.gym.service.SnapshotHandle
import com.wingedsheep.sdk.model.EntityId

/**
 * [GymEnv] adapter over a [GameEnvironment] — a game of Magic.
 *
 * Holds the per-env bookkeeping that used to live in `MultiEnvService.EnvEntry`
 * (perspective, default reveal flag, the live [ActionRegistry] from the last
 * observation) so the service layer can treat every env type the same. The
 * underlying [GameEnvironment] is left untouched, since the trainer SPI drives
 * it directly.
 */
class GameGymEnv(
    val environment: GameEnvironment,
    private val perspectivePlayerIndex: Int,
    private val defaultRevealAll: Boolean,
    private val observationBuilder: ObservationBuilder = ObservationBuilder(environment.cardRegistry)
) : GymEnv {

    @Volatile
    private var registry: ActionRegistry = ActionRegistry.EMPTY

    private var observedDecisionId: String? = null

    override val isTerminal: Boolean get() = environment.state.gameOver

    override fun observe(revealAll: Boolean?): ObservationResult =
        build(revealAll ?: defaultRevealAll)

    /** Observe the current state from one named seat without enabling debug visibility. */
    fun observeForPlayer(
        playerId: EntityId,
        revealAll: Boolean? = null
    ): ObservationResult {
        require(playerId in environment.playerIds) { "Player $playerId is not seated in this env" }
        return build(revealAll ?: defaultRevealAll, playerId)
    }

    override fun step(actionId: Int, params: ActionParams): ObservationResult {
        executeResolved(registry.resolve(actionId), actionId, params)
        return build(defaultRevealAll)
    }

    override fun fork(): GymEnv =
        GameGymEnv(environment.fork(), perspectivePlayerIndex, defaultRevealAll, observationBuilder)
            .also { it.build(defaultRevealAll) }

    // --- game-only operations (used by MultiEnvService via cast) -------------

    /** Re-initialise the underlying game in place. */
    fun reset(gameConfig: GameConfig): ObservationResult {
        environment.reset(gameConfig)
        return build(defaultRevealAll)
    }

    /** Submit a raw `DecisionResponse` while paused on a complex decision. */
    fun submitDecision(response: DecisionResponse): ObservationResult {
        val pending = environment.state.pendingDecision
            ?: throw IllegalStateException("Env is not paused on a decision")
        require(observedDecisionId == pending.id) {
            "Observe the deciding player before submitting this decision"
        }
        check(response.decisionId == pending.id) {
            "Decision ID mismatch: response=${response.decisionId}, pending=${pending.id}"
        }
        environment.step(SubmitDecision(pending.playerId, response))
        failOnDecisionRejection(response.decisionId)
        return build(defaultRevealAll)
    }

    fun snapshot(codec: SnapshotCodec): SnapshotHandle =
        codec.save(state = environment.state, playerIds = environment.playerIds, stepCount = 0)

    fun restore(codec: SnapshotCodec, handle: SnapshotHandle): ObservationResult {
        val snap = codec.load(handle)
        environment.restore(snap.state, snap.playerIds, snap.stepCount)
        return build(defaultRevealAll)
    }

    // --- internals -----------------------------------------------------------

    private fun build(
        revealAll: Boolean,
        requestedPerspective: EntityId? = null
    ): ObservationResult {
        val perspective = requestedPerspective
            ?: environment.playerIds.getOrNull(perspectivePlayerIndex)
            ?: throw IllegalStateException("Env has no player at index $perspectivePlayerIndex")
        val result = observationBuilder.build(
            environment.state, perspective, environment.legalActions(), revealAll
        )

        // A non-acting perspective must not receive another seat's pending decision or legal-action
        // surface: action descriptions can themselves reveal hidden cards or choices. Callers that
        // control several seats request the current actor explicitly through observeForPlayer.
        val seatOwnsAction = environment.agentToAct == null || environment.agentToAct == perspective
        if (!revealAll && !seatOwnsAction) {
            val observation = result.observation as? TrainingObservation
                ?: throw IllegalStateException("GameGymEnv expected a TrainingObservation")
            val sanitized = observation.copy(
                pendingDecision = null,
                legalActions = emptyList(),
                stateDigest = ""
            )
            val safeObservation = sanitized.copy(stateDigest = StateDigest.compute(sanitized))
            registry = ActionRegistry.EMPTY
            observedDecisionId = null
            return ObservationResult(safeObservation, ActionRegistry.EMPTY)
        }

        registry = result.registry
        observedDecisionId = environment.state.pendingDecision?.id
        return result
    }

    private fun executeResolved(resolved: ResolvedAction, actionId: Int, params: ActionParams) {
        when (resolved) {
            is ResolvedAction.Legal -> {
                // The enumerated action is a template for the action types that need a choice the
                // ID can't carry (attackers, blockers, targets, X); params complete it.
                val action = ActionParameterizer.apply(resolved.action, params, environment.state)
                environment.step(action)
                failOnRejection(actionId)
            }
            is ResolvedAction.Decision -> {
                val response = if (resolved.response is com.wingedsheep.engine.core.PlayCardResponse) {
                    resolved.response.copy(action = ActionParameterizer.apply(resolved.response.action, params, environment.state))
                } else {
                    require(params.isEmpty) { "Action ID $actionId is a folded decision response and takes no step params" }
                    resolved.response
                }
                val pending = environment.state.pendingDecision
                    ?: throw IllegalStateException("Registry has a decision response but env is not paused")
                environment.step(SubmitDecision(pending.playerId, response))
                failOnRejection(actionId)
            }
            ResolvedAction.Unknown ->
                throw IllegalArgumentException("Action ID $actionId is not valid for the current step")
        }
    }

    /**
     * An engine rejection leaves the state untouched, which would otherwise read as a successful
     * no-op — the exact way a mis-declared attack used to disappear. Applies to a submitted decision
     * for the same reason it applies to a played action: neither changes the state when refused.
     */
    private fun failOnRejection(actionId: Int) {
        environment.lastRejection?.let {
            throw IllegalArgumentException("Action $actionId rejected by the engine: $it")
        }
    }

    /** Raw structured decisions bypass the action registry, so surface their rejection explicitly. */
    private fun failOnDecisionRejection(decisionId: String) {
        environment.lastRejection?.let {
            throw IllegalArgumentException("Decision $decisionId rejected by the engine: $it")
        }
    }
}
