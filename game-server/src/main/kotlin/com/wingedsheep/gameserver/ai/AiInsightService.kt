package com.wingedsheep.gameserver.ai

import com.wingedsheep.ai.insight.AiDecisionInsight
import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.sdk.model.EntityId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val logger = LoggerFactory.getLogger(AiInsightService::class.java)

/**
 * The local testing mode's store and its step control: every choice the engine AI made in a live
 * game with the scores it assigned each option, and — when step mode is on — the ability to hold the
 * AI at a decision and hand it a different move than the one it picked.
 *
 * Off unless `game.ai.insight-enabled` is set (`application-local.yml` turns it on). While off,
 * [sinkFor] and [gateFor] return null, no AI is given either, and the `Strategist` skips the
 * recording branch entirely — a production server pays nothing for this.
 *
 * The retained [GameState]s are what make an entry exportable as AI training input: the scores alone
 * say what the AI preferred, not what it was looking at. They cost little to *hold* (states are
 * immutable and structurally shared with the live game), so the ring buffer is sized by how much
 * history is useful to browse rather than by memory.
 */
@Service
class AiInsightService(gameProperties: GameProperties) {

    val enabled: Boolean = gameProperties.ai.insightEnabled

    /** Decisions per game session, oldest first. */
    private val byGame = ConcurrentHashMap<String, ArrayDeque<AiInsightEntry>>()

    /**
     * Seat → game session, so the client can ask for "my game" using the only id it has, its own
     * player id. Rebuilt from each recorded state's turn order, which keeps it correct across
     * reconnects and never needs its own teardown path beyond [clearGame].
     */
    private val gameByPlayer = ConcurrentHashMap<EntityId, String>()

    /**
     * Seats whose human asked to approve each AI move before it is submitted.
     *
     * Keyed by **player**, not game, because the toggle has to work before the AI has ever moved —
     * and [gameByPlayer] only learns the mapping from a recorded decision. Keying this by game made
     * arming step mode at the start of a game a silent no-op.
     */
    private val stepModeByPlayer = ConcurrentHashMap<EntityId, Boolean>()

    /** Last time each client polled — the watchdog behind [stepModeActive]. */
    private val lastPolledAtByPlayer = ConcurrentHashMap<EntityId, Long>()

    /** The decision an AI is currently held at, per game. At most one: only one seat acts at a time. */
    private val pendingByGame = ConcurrentHashMap<String, PendingAiDecision>()

    private val nextId = AtomicLong(1)

    /**
     * The sink for one AI seat, or null when the mode is off — which is exactly what the AI wiring
     * passes through to `EngineAiPlayerController`, so "off" needs no second check anywhere.
     */
    fun sinkFor(gameSessionId: String, aiPlayerId: EntityId): AiInsightSink? {
        if (!enabled) return null
        logger.info("AI insight recording enabled for game {} seat {}", gameSessionId, aiPlayerId.value)
        return AiInsightSink { state, insight -> record(gameSessionId, state, insight) }
    }

    /**
     * The step gate for a game's AI seats, or null when the mode is off.
     *
     * Returned as a lambda rather than a service reference so `AiWebSocketSession` — which knows
     * nothing about insight — depends only on "given the move you were about to make, what should I
     * actually submit?".
     */
    fun gateFor(gameSessionId: String): AiActionGate? {
        if (!enabled) return null
        return AiActionGate { aiPlayerId, proposed -> awaitApproval(gameSessionId, aiPlayerId, proposed) }
    }

    private fun record(gameSessionId: String, state: GameState, insight: AiDecisionInsight) {
        val entry = AiInsightEntry(nextId.getAndIncrement(), Instant.now(), insight, state)
        val entries = byGame.computeIfAbsent(gameSessionId) { ArrayDeque() }
        synchronized(entries) {
            entries.addLast(entry)
            while (entries.size > MAX_DECISIONS_PER_GAME) entries.removeFirst()
        }
        state.turnOrder.forEach { gameByPlayer[it] = gameSessionId }
    }

    // ── Browsing ──────────────────────────────────────────────────────────────

    /** The game session [playerId] is seated in, or null if no AI in it has recorded anything yet. */
    fun gameForPlayer(playerId: EntityId): String? = gameByPlayer[playerId]

    /** Recorded decisions for [gameSessionId], newest first. */
    fun decisions(gameSessionId: String, limit: Int = MAX_DECISIONS_PER_GAME): List<AiInsightEntry> {
        val entries = byGame[gameSessionId] ?: return emptyList()
        return synchronized(entries) { entries.toList() }.asReversed().take(limit)
    }

    fun decision(gameSessionId: String, id: Long): AiInsightEntry? =
        decisions(gameSessionId).firstOrNull { it.id == id }

    fun clearGame(gameSessionId: String) {
        byGame.remove(gameSessionId)
        gameByPlayer.entries.removeIf { it.value == gameSessionId }
        // Releasing first: a held AI whose history is being wiped must not be stranded.
        pendingByGame[gameSessionId]?.let { it.deferred.complete(it.proposed) }
        pendingByGame.remove(gameSessionId)
    }

    // ── Step mode ─────────────────────────────────────────────────────────────

    /** Note that [playerId]'s client is watching. See [stepModeActive]. */
    fun markPolled(playerId: EntityId) {
        lastPolledAtByPlayer[playerId] = System.currentTimeMillis()
    }

    fun setStepMode(playerId: EntityId, on: Boolean) {
        stepModeByPlayer[playerId] = on
        logger.info("AI step mode {} for seat {}", if (on) "ON" else "OFF", playerId.value)
        // Turning it off must release whatever is already held, or the AI sits there until the
        // watchdog fires — the click that disables the mode is also "stop holding my game".
        if (!on) gameForPlayer(playerId)?.let { resume(it, optionIndex = null) }
    }

    fun isStepMode(playerId: EntityId): Boolean = stepModeByPlayer[playerId] == true

    /**
     * Step mode only holds the AI while a client is actually watching.
     *
     * The only thing that can release a held decision is a human clicking in the panel. If the tab
     * is closed with step mode left on, every AI move would stall for the full
     * [APPROVAL_TIMEOUT_MS] — so a stale poll is treated as "nobody is there", and the game plays on
     * at full speed. The seats are read off the recorded position rather than tracked separately,
     * which keeps this correct for a pod where only one player is watching.
     */
    private fun stepModeActive(seats: List<EntityId>): Boolean {
        val now = System.currentTimeMillis()
        return seats.any { seat ->
            stepModeByPlayer[seat] == true &&
                (lastPolledAtByPlayer[seat]?.let { now - it < WATCHER_TIMEOUT_MS } ?: false)
        }
    }

    /** The decision currently held for [gameSessionId], if any. */
    fun pending(gameSessionId: String): PendingAiDecision? = pendingByGame[gameSessionId]

    /**
     * Hold [proposed] until a human approves it or swaps it, and return whatever should actually be
     * submitted.
     *
     * Only decisions the AI genuinely weighed are held: a window with a single legal action has no
     * insight to show, and stopping on it would be a click with nothing to read. Falls back to
     * [proposed] on every path that isn't an explicit human choice — a timeout, a game with no
     * recorded decision, a seat mismatch — because a debugging aid must never be able to wedge a
     * game.
     */
    private suspend fun awaitApproval(
        gameSessionId: String,
        aiPlayerId: EntityId,
        proposed: GameAction,
    ): GameAction {
        val latest = decisions(gameSessionId, 1).firstOrNull() ?: return proposed
        if (latest.insight.playerId != aiPlayerId) return proposed
        if (!stepModeActive(latest.state.turnOrder)) return proposed

        val held = PendingAiDecision(latest.id, aiPlayerId, proposed, CompletableDeferred())
        pendingByGame[gameSessionId] = held
        logger.info("AI held at decision {} in game {}: {}", latest.id, gameSessionId, latest.insight.chosenLabel)
        return try {
            withTimeoutOrNull(APPROVAL_TIMEOUT_MS) { held.deferred.await() } ?: proposed
        } finally {
            pendingByGame.remove(gameSessionId, held)
        }
    }

    /**
     * Release a held AI. [optionIndex] swaps in that option's action instead of the AI's own pick;
     * null lets the AI play what it chose.
     *
     * An override is stamped on the recorded entry, so the timeline shows which decisions a human
     * disagreed with and the export carries "the AI wanted A, the human played B" — which is the
     * pair worth training on.
     */
    fun resume(gameSessionId: String, optionIndex: Int?): Boolean {
        val held = pendingByGame[gameSessionId] ?: return false
        val entry = decision(gameSessionId, held.decisionId)
        val option = optionIndex?.let { entry?.insight?.options?.getOrNull(it) }
        val override = option?.action
        if (option != null && override == null) return false
        if (option != null && override != null) {
            entry?.humanOverride = AiHumanOverride(optionIndex, option.label)
            logger.info("Human overrode AI decision {}: {} → {}", held.decisionId, entry?.insight?.chosenLabel, option.label)
        }
        return held.deferred.complete(override ?: held.proposed)
    }

    private companion object {
        /**
         * Ring-buffer depth per game. The AI decides on most priority windows, so a long game
         * produces hundreds; this is roughly "the last few turns", which is the window anyone
         * actually scrolls back through when a play looks wrong.
         */
        const val MAX_DECISIONS_PER_GAME = 200

        /** Longest a held AI waits for a human before playing its own pick. */
        const val APPROVAL_TIMEOUT_MS = 10 * 60 * 1000L

        /** How stale the last poll may be before step mode is treated as unattended. */
        const val WATCHER_TIMEOUT_MS = 15_000L
    }
}

/**
 * Given the move an AI is about to submit, the move it should actually submit.
 *
 * Suspends while step mode holds the decision. `AiWebSocketSession` calls this from its existing IO
 * coroutine, so a hold is the game sitting quietly at the AI's turn, not a blocked thread.
 */
fun interface AiActionGate {
    suspend fun approve(aiPlayerId: EntityId, proposed: GameAction): GameAction
}

/** An AI decision held for human approval. */
class PendingAiDecision(
    val decisionId: Long,
    val aiPlayerId: EntityId,
    /** What the AI chose on its own. */
    val proposed: GameAction,
    internal val deferred: CompletableDeferred<GameAction>,
)

/** A move a human substituted for the AI's pick. */
data class AiHumanOverride(val optionIndex: Int, val label: String)

/**
 * One recorded decision: what the AI weighed, and the position it weighed it in.
 *
 * [state] is the AI's own view — unmasked, and already determinized if the profile samples hidden
 * information — which is what makes an export reproduce the decision rather than approximate it.
 */
class AiInsightEntry(
    val id: Long,
    val recordedAt: Instant,
    val insight: AiDecisionInsight,
    val state: GameState,
) {
    /** Set when a human played something other than the AI's pick at this decision. */
    @Volatile
    var humanOverride: AiHumanOverride? = null
}
