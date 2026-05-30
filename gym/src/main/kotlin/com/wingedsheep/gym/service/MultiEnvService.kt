package com.wingedsheep.gym.service

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.gym.GameEnvironment
import com.wingedsheep.gym.GameGymEnv
import com.wingedsheep.gym.GymEnv
import com.wingedsheep.gym.contract.ObservationResult
import com.wingedsheep.gym.deckbuild.DeckbuildEnvironment
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.model.EntityId
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

/**
 * In-JVM registry for many concurrent gym environments. Transport-agnostic:
 * exposes create / reset / observe / step / fork / snapshot / dispose methods
 * that HTTP, gRPC, or an in-process training loop can call directly.
 *
 * Environments are polymorphic ([GymEnv]): game envs ([GameGymEnv]) and deckbuild
 * envs ([DeckbuildEnvironment]) share the observe/step/fork surface. Operations that
 * only make sense for a game (decision submission, snapshot/restore, reset) require a
 * [GameGymEnv] and reject other env kinds.
 *
 * ## Threading
 *
 * Each env is single-threaded — two calls naming the same [EnvId] must not
 * overlap or they race on mutable env fields. The intended use is: a trainer
 * owns an env and calls it sequentially, possibly interleaved with N other
 * envs which run in parallel via [stepBatch].
 *
 * ## Registry regeneration
 *
 * Every `reset` / `step` rebuilds the env's action mapping. Action IDs from a
 * previous step are invalidated.
 */
class MultiEnvService(
    val cardRegistry: CardRegistry,
    boosterGenerator: BoosterGenerator? = null,
    val workerPool: EnvWorkerPool = EnvWorkerPool(),
    val snapshotCodec: SnapshotCodec = SnapshotCodec()
) {
    private val envs = ConcurrentHashMap<EnvId, GymEnv>()
    val deckResolver: DeckResolver = DeckResolver(cardRegistry, boosterGenerator)

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Create a new game env and run `reset` immediately. The returned observation
     * is the opening state (post-mulligan if [EnvConfig.skipMulligans]).
     */
    fun create(config: EnvConfig): CreatedEnv {
        val gameConfig = config.toGameConfig()
        val env = GameEnvironment.create(cardRegistry)
        env.reset(gameConfig)
        val gymEnv = GameGymEnv(env, config.perspectivePlayerIndex, config.revealAll)
        val envId = EnvId.generate()
        envs[envId] = gymEnv
        return CreatedEnv(envId, gymEnv.observe())
    }

    /**
     * Create a new **deckbuild** env: open a sealed pool from [DeckbuildConfig.setCode]
     * and hand the agent the build interface. The opening observation lists the pool.
     */
    fun createDeckbuild(config: DeckbuildConfig): CreatedEnv {
        val sealed = deckResolver.openSealedPool(config.setCode, config.boosterCount)
        val env = DeckbuildEnvironment(
            pool = sealed.pool,
            basics = sealed.basics,
            targetSize = config.targetSize
        )
        val envId = EnvId.generate()
        envs[envId] = env
        return CreatedEnv(envId, env.observe())
    }

    /** Reset an existing game env while keeping the same [EnvId]. */
    fun reset(envId: EnvId, config: EnvConfig): ObservationResult =
        requireGameEnv(envId).reset(config.toGameConfig())

    /** Drop envs from the registry. Idempotent. */
    fun dispose(envIds: Collection<EnvId>) {
        envIds.forEach { envs.remove(it) }
    }

    fun listEnvs(): Set<EnvId> = envs.keys.toSet()

    // =========================================================================
    // Observations / stepping
    // =========================================================================

    /** Get the current observation without advancing state. */
    fun observe(envId: EnvId, revealAll: Boolean? = null): ObservationResult =
        requireEnv(envId).observe(revealAll)

    /**
     * Advance a single env by the given [StepRequest.actionId]. The ID must
     * come from the most-recent observation for that env.
     */
    fun step(request: StepRequest): ObservationResult =
        requireEnv(request.envId).step(request.actionId)

    /** Advance N envs in parallel. Each env is single-threaded inside its own task. */
    fun stepBatch(requests: List<StepRequest>): List<Pair<EnvId, ObservationResult>> {
        if (requests.isEmpty()) return emptyList()
        val tasks = requests.map { req -> Callable { req.envId to step(req) } }
        return workerPool.invokeAll(tasks)
    }

    /**
     * Submit a raw `DecisionResponse` for a game env paused on a complex pending
     * decision. Simple decisions are driven via [step] with a folded action ID.
     */
    fun submitDecision(envId: EnvId, response: DecisionResponse): ObservationResult =
        requireGameEnv(envId).submitDecision(response)

    // =========================================================================
    // Fork / snapshot / restore
    // =========================================================================

    /** Fork an env N times. Children diverge independently from the next step on. */
    fun fork(srcEnvId: EnvId, count: Int = 1): List<EnvId> {
        require(count > 0) { "fork count must be positive" }
        val src = requireEnv(srcEnvId)
        return List(count) {
            val newId = EnvId.generate()
            envs[newId] = src.fork()
            newId
        }
    }

    fun snapshot(envId: EnvId): SnapshotHandle =
        requireGameEnv(envId).snapshot(snapshotCodec)

    /** Restore a game env to a previously-snapshotted state. */
    fun restore(envId: EnvId, handle: SnapshotHandle): ObservationResult =
        requireGameEnv(envId).restore(snapshotCodec, handle)

    // =========================================================================
    // Internals
    // =========================================================================

    private fun EnvConfig.toGameConfig(): GameConfig = GameConfig(
        players = players.map { spec ->
            PlayerConfig(
                name = spec.name,
                deck = deckResolver.resolve(spec.deck),
                startingLife = spec.startingLife,
                playerId = spec.playerId
            )
        },
        startingHandSize = startingHandSize,
        skipMulligans = skipMulligans,
        useHandSmoother = useHandSmoother,
        startingPlayerIndex = startingPlayerIndex
    )

    private fun requireEnv(envId: EnvId): GymEnv =
        envs[envId] ?: throw NoSuchElementException("Unknown envId: $envId")

    private fun requireGameEnv(envId: EnvId): GameGymEnv =
        requireEnv(envId) as? GameGymEnv
            ?: throw IllegalStateException("Env $envId is not a game env; operation not supported")
}

/** Result of [MultiEnvService.create] — the new env's ID plus its opening observation. */
data class CreatedEnv(
    val envId: EnvId,
    val observation: ObservationResult
)
