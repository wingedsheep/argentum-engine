package com.wingedsheep.gameserver.replay

import com.wingedsheep.engine.handlers.PredicateEvaluator
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

/** One recorded action as re-simulation applied it: [action] took [before] to [after]. */
data class ReplayStep(val index: Int, val before: GameState, val action: GameAction, val after: GameState)

/**
 * The outcome of [ReplayReconstructor.walk]: the last position it could vouch for, and — when
 * [fidelity] is [ReplayFidelity.DIVERGED] — the index of the action that failed and why.
 */
data class ReplayWalk(
    val finalState: GameState,
    val fidelity: ReplayFidelity,
    val divergedAtAction: Int?,
    val divergenceReason: String?,
)

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

        val start = engine.initialState(replay)
        var previous = engine.spectatorStateBuilder.buildState(start, seats, setup.seatRoster, replay.gameId)
        val initial = previous
        val deltas = ArrayList<SpectatorReplayDelta>(replay.actions.size)

        val walk = fold(engine, replay, start) { step ->
            val snapshot = engine.spectatorStateBuilder.buildState(step.after, seats, setup.seatRoster, replay.gameId)
            deltas.add(SpectatorReplayDiffCalculator.computeDelta(previous, snapshot))
            previous = snapshot
        }
        return ReconstructedReplay(
            initialSnapshot = initial,
            deltas = deltas,
            fidelity = walk.fidelity,
            divergedAtFrame = walk.divergedAtAction,
            divergenceReason = walk.divergenceReason,
        )
    }

    /**
     * Fold [replay]'s recorded actions one at a time, handing [visit] each action together with the
     * state it was applied to and the state it produced — for tools that need every intermediate
     * position rather than the viewer stream (the AI game-review logs re-derive what each seat could
     * have done from `before`). Stops at a divergence, exactly as [reconstruct] does.
     */
    fun walk(replay: CompactReplay, visit: (ReplayStep) -> Unit): ReplayWalk {
        val engine = engineFor(replay)
        return fold(engine, replay, engine.initialState(replay), visit)
    }

    private fun fold(engine: ReplayEngine, replay: CompactReplay, start: GameState, visit: (ReplayStep) -> Unit): ReplayWalk {
        var state = start
        for ((index, action) in replay.actions.withIndex()) {
            val step = engine.applyAction(replay, state, action, index)
            if (step.failure != null) {
                logger.warn(
                    "Replay {} (recorded on {}) diverged at action {} ({}): {} — truncating to {} frames",
                    replay.gameId, replay.engineVersion, index, action::class.simpleName, step.failure, 1 + index,
                )
                return ReplayWalk(state, ReplayFidelity.DIVERGED, index, step.failure)
            }
            val next = step.state!!
            visit(ReplayStep(index, state, action, next))
            state = next
        }
        val fidelity = if (replay.checkpoints.isEmpty()) ReplayFidelity.UNVERIFIED else ReplayFidelity.EXACT
        return ReplayWalk(state, fidelity, null, null)
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

    /** The state a replay starts from, as reconstruction builds it. */
    internal fun initialState(replay: CompactReplay): GameState = engineFor(replay).initialState(replay)

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
    val spectatorStateBuilder = SpectatorStateBuilder(cardRegistry, ClientStateTransformer(cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null)))

    fun initialState(replay: CompactReplay): GameState {
        val setup = replay.setup
        val config = GameConfig(
            players = setup.players.map {
                PlayerConfig(
                    name = it.name,
                    deck = it.deck,
                    startingLife = it.startingLife,
                    playerId = EntityId(it.playerId),
                    commanderCardNames = it.commanderCardNames,
                )
            },
            startingHandSize = setup.startingHandSize,
            skipMulligans = setup.skipMulligans,
            useHandSmoother = setup.useHandSmoother,
            handSmootherCandidates = setup.handSmootherCandidates,
            startingPlayerIndex = setup.startingPlayerIndex,
            format = setup.format,
            attackMode = setup.attackMode,
            playingForAnte = setup.playingForAnte,
            chaosRules = setup.chaosRules,
            teams = setup.teams,
            seed = setup.seed,
            preserveGraveyardOrder = setup.preserveGraveyardOrder,
            shuffledDeckIds = replay.version >= CompactReplay.SHUFFLED_DECK_IDS_VERSION,
            shuffledTeamSeats = replay.version >= CompactReplay.SHUFFLED_TEAM_SEATS_VERSION,
        )
        val initialized = gameInitializer.initializeGame(config)
        val begun = actionProcessor.beginGame(initialized.state, initialized.events).state
        return applyYields(begun, replay.yields, afterActionCount = 0)
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
                ReplayYieldOp.MANUAL_TRIGGER_ORDERING -> current.withManualTriggerOrdering(playerId, true)
                ReplayYieldOp.AUTOMATIC_TRIGGER_ORDERING -> current.withManualTriggerOrdering(playerId, false)
            }
        }
        return current
    }

    /**
     * Re-bind a recorded action to the current reconstructed state. Historical recordings minted
     * decision IDs from a UUID (bare, or as a `<label>-<uuid>` suffix), so the recorded ID never
     * matched the freshly created one and had to be retargeted; the choice payload (targets,
     * cards, numbers — all by deterministic entity id) is untouched, so the outcome is identical.
     *
     * Current routing IDs reproduce from `GameState.nextRoutingId`, so a modern recording's ID
     * already addresses the reconstructed prompt. A mismatch there is not bookkeeping noise — it
     * means reconstruction reached a *different* question than the one the player answered, and
     * silently retargeting would replace divergence with a plausible-looking wrong line of play.
     * So the fallback is gated on the legacy ID shape and a modern mismatch is left to fail.
     */
    private fun rebind(action: GameAction, state: GameState): GameAction {
        if (action !is SubmitDecision) return action
        val pendingId = state.pendingDecision?.id ?: return action
        val recordedId = action.response.decisionId
        if (pendingId == recordedId) return action
        if (!LEGACY_DECISION_ID.containsMatchIn(recordedId)) return action
        return action.copy(response = action.response.withDecisionId(pendingId))
    }

    private companion object {
        /** The UUID a pre-`nextRoutingId` recording embedded in every decision ID. */
        private val LEGACY_DECISION_ID =
            Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}
