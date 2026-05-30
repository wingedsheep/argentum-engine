package com.wingedsheep.gym

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.gym.contract.ActionRegistry
import com.wingedsheep.gym.contract.ObservationBuilder
import com.wingedsheep.gym.contract.ObservationResult
import com.wingedsheep.gym.contract.ResolvedAction
import com.wingedsheep.gym.service.SnapshotCodec
import com.wingedsheep.gym.service.SnapshotHandle

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
    private val observationBuilder: ObservationBuilder = ObservationBuilder()
) : GymEnv {

    @Volatile
    private var registry: ActionRegistry = ActionRegistry.EMPTY

    override val isTerminal: Boolean get() = environment.state.gameOver

    override fun observe(revealAll: Boolean?): ObservationResult =
        build(revealAll ?: defaultRevealAll)

    override fun step(actionId: Int): ObservationResult {
        executeResolved(registry.resolve(actionId), actionId)
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
        check(response.decisionId == pending.id) {
            "Decision ID mismatch: response=${response.decisionId}, pending=${pending.id}"
        }
        environment.step(SubmitDecision(pending.playerId, response))
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

    private fun build(revealAll: Boolean): ObservationResult {
        val perspective = environment.playerIds.getOrNull(perspectivePlayerIndex)
            ?: throw IllegalStateException("Env has no player at index $perspectivePlayerIndex")
        val result = observationBuilder.build(
            environment.state, perspective, environment.legalActions(), revealAll
        )
        registry = result.registry
        return result
    }

    private fun executeResolved(resolved: ResolvedAction, actionId: Int) {
        when (resolved) {
            is ResolvedAction.Legal -> environment.step(resolved.action)
            is ResolvedAction.Decision -> {
                val pending = environment.state.pendingDecision
                    ?: throw IllegalStateException("Registry has a decision response but env is not paused")
                environment.step(SubmitDecision(pending.playerId, resolved.response))
            }
            ResolvedAction.Unknown ->
                throw IllegalArgumentException("Action ID $actionId is not valid for the current step")
        }
    }
}
