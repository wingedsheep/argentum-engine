package com.wingedsheep.gameserver.replay

import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.session.GameSession
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps in-flight recordings in the [ReplayStore] so a restart doesn't lose a game in progress.
 *
 * That guarantee used to come free: the Redis session blob carried the recording along with the live
 * state, and Redis was written on every state change. Moving replays to a single store meant taking
 * the guarantee back explicitly — but *not* by writing on every action. The record is a whole
 * re-encoded blob, so per-action writes are quadratic in the length of the game for a property
 * (surviving a crash) that a few seconds of granularity satisfies just as well.
 *
 * So we sweep instead: every few seconds, flush the sessions whose action count moved. The cost of
 * coarse flushing is that a crash loses the last few actions, which
 * [GameSession.restoreReplayRecording] detects on the way back up (via the fingerprint written with
 * each flush) and handles by keeping the honest shorter replay rather than splicing the rest of the
 * game onto a stale prefix.
 */
@Component
class ReplayCheckpointFlusher(
    private val gameRepository: GameRepository,
    private val replayService: ReplayService,
    private val engineVersion: EngineVersion,
) {
    private val logger = LoggerFactory.getLogger(ReplayCheckpointFlusher::class.java)

    /** sessionId -> what the last flush wrote, so an idle game isn't rewritten every sweep. */
    private val flushed = ConcurrentHashMap<String, Flushed>()

    /**
     * The action count is what normally moves; [truncated] is here because it can flip *without*
     * the count moving — a recording frozen by the size cap stops appending, so a game that was
     * flushed at exactly the cap would otherwise keep a stored record that claims to be the whole
     * game right up until game over.
     */
    private data class Flushed(val actions: Int, val truncated: Boolean)

    /** Guards the one-time startup reconciliation in [adoptRecordsLeftByAPreviousRun]. */
    private val reconciled = AtomicBoolean(false)

    @Scheduled(fixedDelay = FLUSH_INTERVAL_MS, initialDelay = FLUSH_INTERVAL_MS)
    fun flush() {
        val live = gameRepository.findAll()
        val liveIds = live.mapTo(HashSet()) { it.sessionId }
        adoptRecordsLeftByAPreviousRun(liveIds)

        for (session in live) {
            runCatching { flushSession(session) }
                .onFailure { logger.warn("Failed to flush replay for ${session.sessionId}: ${it.message}") }
        }

        // A game we were flushing that no longer has a session was abandoned (conceded away,
        // disconnected, swept as a zombie) — a game over would have called [forget] first. Close its
        // record out so the partial recording is watchable instead of stuck mid-write forever.
        for (sessionId in flushed.keys - liveIds) {
            flushed.remove(sessionId)
            runCatching { replayService.finalizePartial(sessionId) }
                .onFailure { logger.warn("Failed to finalize abandoned replay $sessionId: ${it.message}") }
        }
    }

    /**
     * Once, on the first sweep: close out `IN_PROGRESS` records whose game didn't come back.
     *
     * The abandoned-record sweep above can only see games *this* process flushed, so a crash leaves
     * every record it was mid-write on stranded: the session is gone from Redis (or was never
     * recovered), no one flushes it again, and an `IN_PROGRESS` record is excluded from history — a
     * silently unwatchable replay. Recovery has already run by the time the scheduler starts
     * ([com.wingedsheep.gameserver.persistence.SessionRecoveryService] loads every persisted session
     * on `@PostConstruct`), so a stored recording with no live session here has no live session
     * anywhere and is safe to finalize; one that *was* recovered is adopted so its next flush
     * continues the record instead of starting from a stale count.
     */
    private fun adoptRecordsLeftByAPreviousRun(liveIds: Set<String>) {
        if (!reconciled.compareAndSet(false, true)) return
        val stranded = runCatching { replayService.inProgress() }
            .onFailure { logger.warn("Could not reconcile in-progress replays on startup: ${it.message}") }
            .getOrNull() ?: return

        for (record in stranded) {
            val gameId = record.replay.gameId
            if (gameId in liveIds) {
                flushed[gameId] = Flushed(record.replay.actions.size, record.replay.truncated)
            } else {
                runCatching { replayService.finalizePartial(gameId) }
                    .onFailure { logger.warn("Failed to finalize stranded replay $gameId: ${it.message}") }
            }
        }
    }

    /** Flush everything one last time on shutdown, which is the graceful half of "a restart". */
    @PreDestroy
    fun flushOnShutdown() {
        flush()
    }

    /** Drop bookkeeping for a game that has finished (its final record is already stored). */
    fun forget(sessionId: String) {
        flushed.remove(sessionId)
    }

    private fun flushSession(session: GameSession) {
        // One consistent read: the stored log and the stored fingerprint must describe the same
        // position, or a crash at the position the fingerprint names lets the resume gate pass over a
        // hole in the log. See [ReplayRecordingSnapshot].
        val snapshot = session.replayRecordingSnapshot() ?: return
        // A finished game's final record is written on the game-over path while its session is still
        // live and still recorded; there is nothing left to checkpoint, and [ReplayService] would
        // refuse the write anyway. Skip before paying for the encode.
        if (snapshot.gameOver) return
        if (flushed[session.sessionId] == Flushed(snapshot.actions.size, snapshot.truncated)) return

        replayService.saveInProgress(
            replay = CompactReplay(
                gameId = session.sessionId,
                players = session.getPlayers().map { ReplayPlayerInfo(it.playerId.value, it.playerName) },
                startedAt = (snapshot.startedAt ?: Instant.now()).toString(),
                endedAt = "",
                winnerName = null,
                setup = snapshot.setup,
                actions = snapshot.actions,
                yields = snapshot.yields,
                engineVersion = engineVersion.value,
                // Independent of the action count — derived from the decklists, which never change.
                pinnedCards = session.getPinnedCards(),
                checkpoints = snapshot.checkpoints,
                truncated = snapshot.truncated,
            ),
            resumeFingerprint = snapshot.fingerprint,
        )
        flushed[session.sessionId] = Flushed(snapshot.actions.size, snapshot.truncated)
    }

    private companion object {
        const val FLUSH_INTERVAL_MS = 5_000L
    }
}
