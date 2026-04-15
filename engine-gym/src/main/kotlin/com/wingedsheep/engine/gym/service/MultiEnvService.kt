package com.wingedsheep.engine.gym.service

import com.wingedsheep.engine.ai.BoosterGenerator
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.gym.GameEnvironment
import com.wingedsheep.engine.gym.contract.ActionRegistry
import com.wingedsheep.engine.gym.contract.ObservationBuilder
import com.wingedsheep.engine.gym.contract.ObservationResult
import com.wingedsheep.engine.gym.contract.ResolvedAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.model.EntityId
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

/**
 * In-JVM registry for many concurrent gym environments. Transport-agnostic:
 * exposes create / reset / observe / step / fork / snapshot / dispose methods
 * that HTTP, gRPC, or an in-process training loop can call directly.
 *
 * ## Threading
 *
 * Each env is single-threaded — two calls naming the same [EnvId] must not
 * overlap or they race on mutable `GameEnvironment` fields. The intended use
 * is: a trainer owns an env and calls it sequentially, possibly interleaved
 * with N other envs which run in parallel via [stepBatch]. If a caller
 * manually issues concurrent calls to the same env, behaviour is undefined.
 *
 * ## Registry regeneration
 *
 * Every `reset` / `step` rebuilds the [ActionRegistry] for that env. Stored
 * handles from a previous step are invalidated and resolving them returns
 * `Unknown`.
 */
class MultiEnvService(
    val cardRegistry: CardRegistry,
    boosterGenerator: BoosterGenerator? = null,
    val workerPool: EnvWorkerPool = EnvWorkerPool(),
    val snapshotCodec: SnapshotCodec = SnapshotCodec()
) {
    private val envs = ConcurrentHashMap<EnvId, EnvEntry>()
    private val observationBuilder = ObservationBuilder()
    val deckResolver: DeckResolver = DeckResolver(cardRegistry, boosterGenerator)

    /** Book-keeping for a single active environment. */
    private class EnvEntry(
        val environment: GameEnvironment,
        val perspectivePlayerIndex: Int,
        val revealAll: Boolean,
        @Volatile var registry: ActionRegistry = ActionRegistry.EMPTY
    )

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Create a new env and run `reset` immediately. The returned observation
     * is the opening state (post-mulligan if [EnvConfig.skipMulligans]).
     */
    fun create(config: EnvConfig): CreatedEnv {
        val playerConfigs = config.players.map { spec ->
            PlayerConfig(
                name = spec.name,
                deck = deckResolver.resolve(spec.deck),
                startingLife = spec.startingLife,
                playerId = spec.playerId
            )
        }
        val gameConfig = GameConfig(
            players = playerConfigs,
            startingHandSize = config.startingHandSize,
            skipMulligans = config.skipMulligans,
            useHandSmoother = config.useHandSmoother,
            startingPlayerIndex = config.startingPlayerIndex
        )

        val env = GameEnvironment.create(cardRegistry)
        env.reset(gameConfig)

        val envId = EnvId.generate()
        val entry = EnvEntry(env, config.perspectivePlayerIndex, config.revealAll)
        envs[envId] = entry

        val observation = buildObservation(envId, entry)
        return CreatedEnv(envId, observation)
    }

    /** Reset an existing env while keeping the same [EnvId]. Config reuses the constructor's cards. */
    fun reset(envId: EnvId, config: EnvConfig): ObservationResult {
        val oldEntry = requireEntry(envId)
        val playerConfigs = config.players.map { spec ->
            PlayerConfig(
                name = spec.name,
                deck = deckResolver.resolve(spec.deck),
                startingLife = spec.startingLife,
                playerId = spec.playerId
            )
        }
        val gameConfig = GameConfig(
            players = playerConfigs,
            startingHandSize = config.startingHandSize,
            skipMulligans = config.skipMulligans,
            useHandSmoother = config.useHandSmoother,
            startingPlayerIndex = config.startingPlayerIndex
        )
        oldEntry.environment.reset(gameConfig)
        val newEntry = EnvEntry(oldEntry.environment, config.perspectivePlayerIndex, config.revealAll)
        envs[envId] = newEntry
        return buildObservation(envId, newEntry)
    }

    /** Drop envs from the registry. Idempotent. */
    fun dispose(envIds: Collection<EnvId>) {
        envIds.forEach { envs.remove(it) }
    }

    fun listEnvs(): Set<EnvId> = envs.keys.toSet()

    // =========================================================================
    // Observations
    // =========================================================================

    /** Get the current observation without advancing state. */
    fun observe(envId: EnvId, revealAll: Boolean? = null): ObservationResult {
        val entry = requireEntry(envId)
        val view = revealAll ?: entry.revealAll
        return buildObservation(envId, entry, revealAll = view)
    }

    // =========================================================================
    // Stepping
    // =========================================================================

    /**
     * Advance a single env by the given [StepRequest.actionId]. The ID must
     * come from the most-recent observation for that env — any older ID is
     * treated as invalid.
     */
    fun step(request: StepRequest): ObservationResult {
        val entry = requireEntry(request.envId)
        val resolved = entry.registry.resolve(request.actionId)
        executeResolved(entry, resolved, request.actionId)
        return buildObservation(request.envId, entry)
    }

    /**
     * Advance N envs in parallel. Each env is processed single-threaded
     * inside its own task; envs do not share state so this is safe.
     */
    fun stepBatch(requests: List<StepRequest>): List<Pair<EnvId, ObservationResult>> {
        if (requests.isEmpty()) return emptyList()
        val tasks = requests.map { req ->
            Callable { req.envId to step(req) }
        }
        return workerPool.invokeAll(tasks)
    }

    /**
     * Submit a raw `DecisionResponse` for an env that is paused on a complex
     * pending decision (multi-select, distribute, order, search, etc.).
     * Simple decisions (yes/no, choose-number, choose-mode, choose-color,
     * choose-option, single-select cards) should be driven via [step] with a
     * folded action ID instead.
     */
    fun submitDecision(envId: EnvId, response: DecisionResponse): ObservationResult {
        val entry = requireEntry(envId)
        val pending = entry.environment.state.pendingDecision
            ?: throw IllegalStateException("Env $envId is not paused on a decision")
        check(response.decisionId == pending.id) {
            "Decision ID mismatch: response=${response.decisionId}, pending=${pending.id}"
        }
        entry.environment.step(SubmitDecision(pending.playerId, response))
        return buildObservation(envId, entry)
    }

    // =========================================================================
    // Fork / snapshot / restore
    // =========================================================================

    /**
     * Fork an env N times. Because `GameState` is immutable, each fork shares
     * the base state with the source for free; subsequent steps on each
     * child diverge independently.
     */
    fun fork(srcEnvId: EnvId, count: Int = 1): List<EnvId> {
        require(count > 0) { "fork count must be positive" }
        val src = requireEntry(srcEnvId)
        return List(count) {
            val forkedEnv = src.environment.fork()
            val newId = EnvId.generate()
            val newEntry = EnvEntry(forkedEnv, src.perspectivePlayerIndex, src.revealAll)
            envs[newId] = newEntry
            // Rebuild the registry so freshly-forked envs have valid action IDs.
            buildObservation(newId, newEntry)
            newId
        }
    }

    fun snapshot(envId: EnvId): SnapshotHandle {
        val entry = requireEntry(envId)
        return snapshotCodec.save(
            state = entry.environment.state,
            playerIds = entry.environment.playerIds,
            stepCount = 0 // stepCount reset on restore — training code doesn't rely on it
        )
    }

    /**
     * Restore an env to a previously-snapshotted state. The perspective and
     * reveal settings are preserved; only the underlying [com.wingedsheep.engine.state.GameState]
     * changes.
     */
    fun restore(envId: EnvId, handle: SnapshotHandle): ObservationResult {
        val entry = requireEntry(envId)
        val snap = snapshotCodec.load(handle)
        entry.environment.restore(snap.state, snap.playerIds, snap.stepCount)
        return buildObservation(envId, entry)
    }

    // =========================================================================
    // Internals
    // =========================================================================

    private fun executeResolved(entry: EnvEntry, resolved: ResolvedAction, actionId: Int) {
        when (resolved) {
            is ResolvedAction.Legal -> entry.environment.step(resolved.action)
            is ResolvedAction.Decision -> {
                val pending = entry.environment.state.pendingDecision
                    ?: throw IllegalStateException("Registry has a decision response but env is not paused")
                entry.environment.step(SubmitDecision(pending.playerId, resolved.response))
            }
            ResolvedAction.Unknown ->
                throw IllegalArgumentException("Action ID $actionId is not valid for the current step")
        }
    }

    private fun buildObservation(
        envId: EnvId,
        entry: EnvEntry,
        revealAll: Boolean = entry.revealAll
    ): ObservationResult {
        val env = entry.environment
        val perspective = env.playerIds.getOrNull(entry.perspectivePlayerIndex)
            ?: throw IllegalStateException("Env $envId has no player at index ${entry.perspectivePlayerIndex}")
        val legalActions = env.legalActions()
        val result = observationBuilder.build(env.state, perspective, legalActions, revealAll)
        entry.registry = result.registry
        return result
    }

    private fun requireEntry(envId: EnvId): EnvEntry =
        envs[envId] ?: throw NoSuchElementException("Unknown envId: $envId")
}

/** Result of [MultiEnvService.create] — the new env's ID plus its opening observation. */
data class CreatedEnv(
    val envId: EnvId,
    val observation: ObservationResult
)
