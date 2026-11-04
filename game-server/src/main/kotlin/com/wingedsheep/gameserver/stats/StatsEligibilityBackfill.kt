package com.wingedsheep.gameserver.stats

import com.wingedsheep.gameserver.replay.ReplayService
import com.wingedsheep.gameserver.replay.ReplayStatus
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import kotlin.concurrent.thread

/**
 * Fills in [StatsEligibility] for games recorded before it was tracked, from their stored replays.
 *
 * A finished replay holds every input the game applied, which is exactly what the live recorder reads
 * at game over, so a past game gets the same verdict a new one would. Games with no finished replay
 * (too short to store one, or recorded before replays were) keep `took_action = null` and go on
 * counting, as they always have.
 *
 * Runs once per boot on a background thread, walking forward by match id so a replay that can't be
 * read is passed over rather than retried forever. Once every replayable game is filled in the scan
 * finds nothing and costs one query.
 */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class StatsEligibilityBackfill(
    private val jdbc: JdbcTemplate,
    private val replays: ReplayService,
) {
    private val logger = LoggerFactory.getLogger(StatsEligibilityBackfill::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        thread(name = "stats-eligibility-backfill", isDaemon = true) {
            runCatching { backfill() }.onFailure { logger.warn("Stats eligibility backfill stopped: {}", it.message, it) }
        }
    }

    /** Backfill every eligible game; returns how many matches were updated. */
    fun backfill(): Int {
        var after = 0L
        var updated = 0
        while (true) {
            val batch = jdbc.query(
                """
                SELECT r.id, r.game_id FROM match_results r
                JOIN game_replays gr ON gr.game_id = r.game_id
                WHERE r.id > ?
                  AND EXISTS (SELECT 1 FROM match_participants p WHERE p.match_id = r.id AND p.took_action IS NULL)
                ORDER BY r.id
                LIMIT 100
                """.trimIndent(),
                { rs, _ -> rs.getLong("id") to rs.getString("game_id") },
                after,
            )
            if (batch.isEmpty()) break
            for ((matchId, gameId) in batch) {
                after = matchId
                if (runCatching { backfillMatch(matchId, gameId) }.getOrDefault(false)) updated++
            }
        }
        if (updated > 0) logger.info("Backfilled stats eligibility for {} recorded game(s) from their replays", updated)
        return updated
    }

    private data class Seat(val id: Long, val name: String, val isAi: Boolean)

    private fun backfillMatch(matchId: Long, gameId: String): Boolean {
        val stored = replays.findStored(gameId) ?: return false
        if (stored.status != ReplayStatus.FINISHED) return false
        val replay = stored.replay
        // Seats are matched to replay players by name; a name held twice can't be told apart.
        val idByName = replay.players.groupBy { it.name }
            .filterValues { it.size == 1 }
            .mapValues { it.value.single().playerId }
        val seats = jdbc.query(
            "SELECT id, player_name, is_ai FROM match_participants WHERE match_id = ?",
            { rs, _ -> Seat(rs.getLong("id"), rs.getString("player_name"), rs.getBoolean("is_ai")) },
            matchId,
        )
        if (seats.any { !it.isAi && it.name !in idByName }) return false
        val acted = StatsEligibility.playersWhoActed(replay.actions)
        val tookAction = seats.associate { it.id to (it.isAi || idByName[it.name] in acted) }
        val counts = StatsEligibility.countsInStats(tookAction)
        for (seat in seats) {
            jdbc.update(
                "UPDATE match_participants SET took_action = ?, counts_in_stats = ? WHERE id = ?",
                tookAction.getValue(seat.id), counts.getValue(seat.id), seat.id,
            )
        }
        return true
    }
}
