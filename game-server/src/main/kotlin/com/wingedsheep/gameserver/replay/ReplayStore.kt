package com.wingedsheep.gameserver.replay

import com.wingedsheep.gameserver.persistence.GameReplayPlayerRow
import com.wingedsheep.gameserver.persistence.GameReplayRepository
import com.wingedsheep.gameserver.persistence.GameReplayRow
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Collections

/** Whether a stored record is still being written to, or is the final record of a finished game. */
enum class ReplayStatus { IN_PROGRESS, FINISHED }

/**
 * Everything persisted for one recorded game: the compact input log, the archived viewer stream, and
 * the bookkeeping that lets an interrupted recording be safely resumed.
 */
data class StoredReplay(
    val replay: CompactReplay,
    val status: ReplayStatus,
    /** gzip+base64 [ReplayPresentation] body. Null while in progress, and for over-sized games. */
    val presentation: String? = null,
    /**
     * [ReplayFingerprint] of the live position at the moment this record was written.
     *
     * A game in progress is flushed periodically, so a crash loses the actions played since the last
     * flush. Appending later actions onto that short prefix would silently produce a record of a
     * game nobody played, so on restore we compare this against the recovered live state: equal
     * means nothing was lost and recording continues; different means we stop and keep the honest
     * shorter replay. See [com.wingedsheep.gameserver.session.GameSession.restoreReplayRecording].
     */
    val resumeFingerprint: String? = null,
)

/** Listing projection, read from the metadata columns without decoding the payload. */
data class ReplaySummary(
    val gameId: String,
    val playerNames: List<String>,
    val startedAt: String,
    val endedAt: String,
    val winnerName: String?,
    val frameCount: Int,
    val tournamentName: String? = null,
    val tournamentRound: Int? = null,
)

/**
 * The one home for replays.
 *
 * Replays used to live in three places at once — this table, an in-memory ring buffer of the last
 * 100 games, and (for games still in progress) the Redis session blob — which meant three
 * lifetimes, three eviction rules, and three different answers to "is this game replayable". There
 * is now a single store: finished games and in-flight recordings alike are rows here, written by
 * [ReplayService] and nobody else.
 *
 * Which implementation is wired depends on whether accounts (and therefore a database) are enabled,
 * so the game-over path stays decoupled from the persistence layer — exactly like
 * [com.wingedsheep.gameserver.stats.MatchResultSink].
 */
interface ReplayStore {
    fun save(record: StoredReplay)
    fun find(gameId: String): StoredReplay?
    fun findRecentForPlayer(playerId: String, limit: Int): List<ReplaySummary>

    /** In-progress records, for resuming recordings after a restart. */
    fun findInProgress(): List<StoredReplay>
}

/**
 * Default: no database configured (accounts disabled — local dev, e2e, self-hosted casual play).
 *
 * Replays still have exactly one home, it just doesn't survive a restart. Bounded so a long-running
 * server without a database can't accumulate them without limit.
 */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "false", matchIfMissing = true)
class InMemoryReplayStore : ReplayStore {

    private val records = Collections.synchronizedMap(
        object : LinkedHashMap<String, StoredReplay>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, StoredReplay>) = size > MAX_RECORDS
        }
    )

    override fun save(record: StoredReplay) {
        records[record.replay.gameId] = record
    }

    override fun find(gameId: String): StoredReplay? = records[gameId]

    override fun findRecentForPlayer(playerId: String, limit: Int): List<ReplaySummary> =
        synchronized(records) {
            records.values
                .filter { it.status == ReplayStatus.FINISHED }
                .filter { record -> record.replay.players.any { it.playerId == playerId } }
                .sortedByDescending { it.replay.endedAt }
                .take(limit)
                .map { it.replay.toSummary() }
        }

    override fun findInProgress(): List<StoredReplay> =
        synchronized(records) { records.values.filter { it.status == ReplayStatus.IN_PROGRESS } }

    private companion object {
        const val MAX_RECORDS = 200
    }
}

/** Accounts enabled: replays are rows in Postgres, upserted by game id. */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class JdbcReplayStore(private val replays: GameReplayRepository) : ReplayStore {
    private val logger = LoggerFactory.getLogger(JdbcReplayStore::class.java)

    /**
     * Upsert by game id, keeping the pins out of the hot path.
     *
     * A game in progress is flushed every few seconds and only its action log moves, so an existing
     * in-progress row takes the narrow [GameReplayRepository.updateRecording] write. The full
     * aggregate save — which also (re)writes the pins, the seat children and the finished-game
     * metadata — runs on the first flush and again at game over, twice per game rather than once per
     * five seconds.
     */
    override fun save(record: StoredReplay) {
        val replay = record.replay
        val existing = replays.findByGameId(replay.gameId)
        // Pins live in their own write-once column, so they must not also ride along in the blob.
        val data = ReplayCodec.encode(replay.copy(pinnedCards = emptyList()))
        val endedAt = parseInstant(replay.endedAt) ?: Instant.now()

        if (existing != null && existing.status == ReplayStatus.IN_PROGRESS.name && record.status == ReplayStatus.IN_PROGRESS) {
            replays.updateRecording(
                gameId = replay.gameId,
                data = data,
                status = record.status.name,
                resumeFingerprint = record.resumeFingerprint,
                frameCount = replay.frameCount,
                endedAt = endedAt,
                engineVersion = replay.engineVersion,
            )
            logger.debug(
                "Flushed in-progress replay {} ({} actions, pins untouched)",
                replay.gameId, replay.actions.size,
            )
            return
        }

        replays.save(
            GameReplayRow(
                id = existing?.id,
                gameId = replay.gameId,
                format = replay.setup.format::class.simpleName,
                winnerName = replay.winnerName,
                tournamentName = replay.tournamentName,
                tournamentRound = replay.tournamentRound,
                startedAt = parseInstant(replay.startedAt),
                endedAt = endedAt,
                frameCount = replay.frameCount,
                playerNames = replay.players.joinToString(", ") { it.name },
                status = record.status.name,
                engineVersion = replay.engineVersion,
                resumeFingerprint = record.resumeFingerprint,
                data = data,
                // Never drop pins already stored: a record can be re-saved by a path that didn't
                // recompute them (finalizePartial), and losing them costs the replay its durability.
                pinnedCards = ReplayCodec.encodePins(replay.pinnedCards) ?: existing?.pinnedCards,
                presentation = record.presentation?.let { ReplayCodec.encodeText(it) }
                    ?: existing?.presentation,
                players = replay.players.mapIndexed { seat, player ->
                    GameReplayPlayerRow(seat = seat, playerId = player.playerId, playerName = player.name)
                }.toSet(),
            )
        )
        logger.debug(
            "Persisted {} replay {} ({} actions)", record.status, replay.gameId, replay.actions.size,
        )
    }

    override fun find(gameId: String): StoredReplay? = replays.findByGameId(gameId)?.toStored()

    override fun findRecentForPlayer(playerId: String, limit: Int): List<ReplaySummary> =
        replays.findRecentForPlayer(playerId, limit).map { row ->
            ReplaySummary(
                gameId = row.gameId,
                playerNames = row.playerNames.split(", ").filter { it.isNotBlank() },
                startedAt = row.startedAt?.toString() ?: "",
                endedAt = row.endedAt.toString(),
                winnerName = row.winnerName,
                frameCount = row.frameCount,
                tournamentName = row.tournamentName,
                tournamentRound = row.tournamentRound,
            )
        }

    override fun findInProgress(): List<StoredReplay> =
        replays.findByStatus(ReplayStatus.IN_PROGRESS.name).mapNotNull { it.toStored() }

    private fun GameReplayRow.toStored(): StoredReplay? {
        val decoded = runCatching { ReplayCodec.decode(data) }
            .onFailure { logger.error("Replay {} failed to decode: {}", gameId, it.message) }
            .getOrNull() ?: return null
        // Pins come from their own column post-V11. A pre-V11 row has none there and still carries
        // them inside the blob, so only overwrite when the column actually holds something.
        val pins = runCatching { ReplayCodec.decodePins(pinnedCards) }
            .onFailure { logger.error("Replay {} has unreadable pins: {}", gameId, it.message) }
            .getOrDefault(emptyList())
        return StoredReplay(
            replay = if (pins.isEmpty()) decoded else decoded.copy(pinnedCards = pins),
            status = runCatching { ReplayStatus.valueOf(status) }.getOrDefault(ReplayStatus.FINISHED),
            presentation = presentation?.let { runCatching { ReplayCodec.decodeText(it) }.getOrNull() },
            resumeFingerprint = resumeFingerprint,
        )
    }

    private fun parseInstant(value: String?): Instant? =
        value?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }
}

/** Listing projection for an in-memory record, which has no metadata columns to read instead. */
private fun CompactReplay.toSummary() = ReplaySummary(
    gameId = gameId,
    playerNames = players.map { it.name },
    startedAt = startedAt,
    endedAt = endedAt,
    winnerName = winnerName,
    frameCount = frameCount,
    tournamentName = tournamentName,
    tournamentRound = tournamentRound,
)
