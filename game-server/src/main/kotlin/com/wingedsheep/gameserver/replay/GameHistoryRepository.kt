package com.wingedsheep.gameserver.replay

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * In-memory repository for completed game replays.
 * Capped at 100 games (oldest evicted first).
 */
@Component
class GameHistoryRepository {

    private val history = ConcurrentLinkedDeque<GameReplayRecord>()
    private val maxSize = 100

    fun save(record: GameReplayRecord) {
        history.addFirst(record)
        while (history.size > maxSize) {
            history.removeLast()
        }
    }

    fun findAll(): List<GameReplayRecord> = history.toList()

    fun findById(gameId: String): GameReplayRecord? =
        history.find { it.gameId == gameId }

    fun findByPlayerId(playerId: String): List<GameReplayRecord> =
        history.filter { it.player1Id == playerId || it.player2Id == playerId }
}
