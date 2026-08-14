package com.wingedsheep.gameserver.replay

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.session.SpectatorSeat
import com.wingedsheep.gameserver.session.SpectatorStateBuilder
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** How faithfully a re-simulation reproduced the game that was actually played. */
enum class ReplayFidelity {
    /** Every recorded action applied, and every checkpoint matched. This is the real game. */
    EXACT,

    /**
     * Every recorded action applied, but the record carries no checkpoints to verify against — a v1
     * replay. Almost certainly fine; we just can't prove it.
     */
    UNVERIFIED,

    /**
     * The re-simulation stopped early: an action no longer applies, or a checkpoint proved the board
     * had drifted from the one that was played. Frames past the divergence are withheld rather than
     * shown, because they describe a game that never happened.
     */
    DIVERGED,
}

/** A replay reconstructed back into the snapshot + delta stream the client replay viewer consumes. */
data class ReconstructedReplay(
    val initialSnapshot: ServerMessage.SpectatorStateUpdate,
    val deltas: List<SpectatorReplayDelta>,
    val fidelity: ReplayFidelity = ReplayFidelity.UNVERIFIED,
    /** Frame index the re-simulation stopped at, when [fidelity] is [ReplayFidelity.DIVERGED]. */
    val divergedAtFrame: Int? = null,
    /** Human-readable cause of the divergence, for logs and the viewer's degraded badge. */
    val divergenceReason: String? = null,
) {
    val frameCount: Int get() = 1 + deltas.size
    val isComplete: Boolean get() = fidelity != ReplayFidelity.DIVERGED
}

/**
 * Re-simulates a [CompactReplay] to regenerate exactly what was (or would have been) shown live.
 *
 * Because the engine is deterministic — same seed + same seat ids + same decks + same ordered
 * actions ⇒ byte-identical [GameState] sequence (entity ids included; the engine never mints a
 * UUID) — we can rebuild the initial state with [GameInitializer], fold the recorded actions
 * through [ActionProcessor], and run the *same* [SpectatorStateBuilder] / [SpectatorReplayDiffCalculator]
 * the live broadcast used. The result is the `{initialSnapshot, deltas}` shape the client's
 * `reconstructSnapshots()` already understands, and any single frame's full unmasked state for the
 * "share frame as scenario" path.
 *
 * ## Surviving deploys
 * That determinism argument holds across *time* only if the engine is also unchanged, which over a
 * long-lived project it never is. Two defences apply:
 *
 * 1. **Pinned cards** ([ReplayCardPin]) — the replay carries the compiled definitions it ran on and
 *    they shadow the live corpus for this reconstruction, so card edits (by far the most common
 *    change) stop mattering.
 * 2. **Checkpoints** ([ReplayFingerprint]) — for what pinning can't cover (core rules changes,
 *    tokens, wished-for cards) the recorder left position fingerprints behind, re-checked as we
 *    fold, so drift is caught instead of rendered.
 *
 * When either defence reports a problem we stop at the last frame we can vouch for and mark the
 * result [ReplayFidelity.DIVERGED]; [ReplayService] then falls back to the presentation stream
 * materialized at record time ([ReplayPresentation]) so the viewer still sees the whole game.
 */
@Component
class ReplayReconstructor(
    private val cardRegistry: CardRegistry,
    // Same registry the live game was created with, so re-stamped printing images match
    // byte-for-byte. Nullable to mirror GameInitializer / GameSession (tests pass null).
    private val printingRegistry: PrintingRegistry?,
    private val tokenArtRegistry: com.wingedsheep.engine.registry.TokenArtRegistry? = null,
) {
    private val logger = LoggerFactory.getLogger(ReplayReconstructor::class.java)

    /** Rebuild the full snapshot + delta stream for [replay]. */
    fun reconstruct(replay: CompactReplay): ReconstructedReplay {
        val engine = engineFor(replay)
        val setup = replay.setup
        val seats = setup.players.map { SpectatorSeat(EntityId(it.playerId), it.name) }

        var state = engine.initialState(replay)
        var previous = engine.spectatorStateBuilder.buildState(state, seats, setup.seatRoster, replay.gameId)
        val initial = previous
        val deltas = ArrayList<SpectatorReplayDelta>(replay.actions.size)
        var divergence: String? = null

        for ((index, action) in replay.actions.withIndex()) {
            val step = engine.applyAction(replay, state, action, index)
            if (step.failure != null) {
                divergence = step.failure
                logger.warn(
                    "Replay {} (recorded on {}) diverged at action {} ({}): {} — truncating to {} frames",
                    replay.gameId, replay.engineVersion, index, action::class.simpleName,
                    step.failure, 1 + deltas.size,
                )
                break
            }
            state = step.state!!
            val snapshot = engine.spectatorStateBuilder.buildState(state, seats, setup.seatRoster, replay.gameId)
            deltas.add(SpectatorReplayDiffCalculator.computeDelta(previous, snapshot))
            previous = snapshot
        }

        val fidelity = when {
            divergence != null -> ReplayFidelity.DIVERGED
            replay.checkpoints.isEmpty() -> ReplayFidelity.UNVERIFIED
            else -> ReplayFidelity.EXACT
        }
        return ReconstructedReplay(
            initialSnapshot = initial,
            deltas = deltas,
            fidelity = fidelity,
            divergedAtFrame = if (divergence != null) deltas.size else null,
            divergenceReason = divergence,
        )
    }

    /**
     * The full, unmasked [GameState] at [frame] (0 = initial state, N = after the Nth action).
     * Powers the "share frame as scenario" path. Returns null if the frame is out of range or the
     * replay diverges before reaching it — a shared scenario must be the real position or nothing.
     */
    fun reconstructStateAt(replay: CompactReplay, frame: Int): GameState? {
        if (frame < 0 || frame > replay.actions.size) return null
        val engine = engineFor(replay)
        var state = engine.initialState(replay)
        for (index in 0 until frame) {
            val step = engine.applyAction(replay, state, replay.actions[index], index)
            if (step.failure != null) {
                logger.warn(
                    "Replay {} diverged at action {} while seeking frame {}: {}",
                    replay.gameId, index, frame, step.failure,
                )
                return null
            }
            state = step.state!!
        }
        return state
    }

    /**
     * Engine services bound to this replay's pinned card definitions. Built per reconstruction
     * because the pinned corpus differs per replay; the overlay is a thin child registry, so this
     * costs a handful of map inserts rather than a copy of the corpus.
     */
    private fun engineFor(replay: CompactReplay): ReplayEngine =
        ReplayEngine(ReplayCardPin.overlay(cardRegistry, replay.pinnedCards), printingRegistry, tokenArtRegistry)
}

/** Outcome of folding one recorded action: a new state, or the reason we can't trust it. */
private class StepResult(val state: GameState?, val failure: String?)

/**
 * A [ReplayReconstructor] run bound to one replay's card corpus — the engine plumbing plus the
 * yield / decision-rebind / checkpoint bookkeeping that folding a recorded stream needs.
 */
private class ReplayEngine(
    cardRegistry: CardRegistry,
    printingRegistry: PrintingRegistry?,
    tokenArtRegistry: com.wingedsheep.engine.registry.TokenArtRegistry? = null,
) {
    private val actionProcessor = ActionProcessor(EngineServices(cardRegistry, printingRegistry, tokenArtRegistry))
    private val gameInitializer = GameInitializer(cardRegistry, printingRegistry)
    val spectatorStateBuilder = SpectatorStateBuilder(cardRegistry, ClientStateTransformer(cardRegistry))

    fun initialState(replay: CompactReplay): GameState {
        val setup = replay.setup
        val config = GameConfig(
            players = setup.players.map {
                PlayerConfig(
                    name = it.name,
                    deck = it.deck,
                    startingLife = it.startingLife,
                    playerId = EntityId(it.playerId),
                    commanderCardName = it.commanderCardName,
                )
            },
            startingHandSize = setup.startingHandSize,
            skipMulligans = setup.skipMulligans,
            useHandSmoother = setup.useHandSmoother,
            handSmootherCandidates = setup.handSmootherCandidates,
            startingPlayerIndex = setup.startingPlayerIndex,
            format = setup.format,
            attackMode = setup.attackMode,
            teams = setup.teams,
            seed = setup.seed,
        )
        return applyYields(gameInitializer.initializeGame(config).state, replay.yields, afterActionCount = 0)
    }

    /**
     * Apply the action at [index], re-apply any yields set at that point, and verify the checkpoint
     * stamped there. Returns a failure reason instead of a state when the action doesn't apply or
     * the position no longer matches what was recorded.
     */
    fun applyAction(replay: CompactReplay, state: GameState, action: GameAction, index: Int): StepResult {
        val result = actionProcessor.process(state, rebind(action, state)).result
        if (result.error != null) return StepResult(null, "action rejected: ${result.error}")

        val afterActionCount = index + 1
        // Re-apply any yields set right after this action was originally applied, so the engine's
        // auto-answers reproduce on the next iteration exactly as they did live.
        val next = applyYields(result.state, replay.yields, afterActionCount)

        val checkpoint = replay.checkpoints.firstOrNull { it.afterActionCount == afterActionCount }
        if (checkpoint != null) {
            val actual = ReplayFingerprint.of(next)
            if (actual != checkpoint.fingerprint) {
                return StepResult(
                    null,
                    "position drifted from the recording after action $index " +
                        "(recorded ${checkpoint.fingerprint}, re-simulated $actual)",
                )
            }
        }
        return StepResult(next, null)
    }

    /**
     * Re-apply every recorded yield whose [ReplayYieldEntry.afterActionCount] equals [afterActionCount]
     * (i.e. it was originally set right after that many actions had been applied). Mirrors
     * [com.wingedsheep.gameserver.session.GameSession.setAbilityYield] and friends so the engine's
     * auto-answers reproduce identically. Almost always a no-op (most games carry no yields).
     */
    private fun applyYields(state: GameState, yields: List<ReplayYieldEntry>, afterActionCount: Int): GameState {
        if (yields.isEmpty()) return state
        var current = state
        for (entry in yields) {
            if (entry.afterActionCount != afterActionCount) continue
            val playerId = EntityId(entry.playerId)
            current = when (entry.op) {
                ReplayYieldOp.SET -> current.withYield(playerId, entry.identity!!, entry.kind!!)
                ReplayYieldOp.CLEAR_ABILITY -> current.withoutYield(playerId, entry.identity!!)
                ReplayYieldOp.CLEAR_ALL -> current.withoutYields(playerId)
            }
        }
        return current
    }

    /**
     * Re-bind a recorded action to the current reconstructed state. Decision ids are minted from a
     * UUID each run, so a recorded [SubmitDecision] carries the *original* run's id; we retarget it
     * at the id the freshly created pending decision actually has. The choice payload (targets,
     * cards, numbers — all by deterministic entity id) is untouched, so the outcome is identical.
     */
    private fun rebind(action: GameAction, state: GameState): GameAction {
        if (action !is SubmitDecision) return action
        val pendingId = state.pendingDecision?.id ?: return action
        if (pendingId == action.response.decisionId) return action
        return action.copy(response = action.response.withDecisionId(pendingId))
    }
}
