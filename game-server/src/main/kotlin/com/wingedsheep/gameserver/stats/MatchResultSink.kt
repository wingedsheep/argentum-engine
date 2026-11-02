package com.wingedsheep.gameserver.stats

import com.wingedsheep.gameserver.persistence.MatchCardRow
import com.wingedsheep.gameserver.persistence.MatchParticipantRow
import com.wingedsheep.gameserver.persistence.MatchResultRepository
import com.wingedsheep.gameserver.persistence.MatchResultRow
import com.wingedsheep.sdk.model.PrintingRef
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/** A finished game ready to be recorded for stats. */
data class RecordedMatch(
    val gameId: String,
    val format: String?,
    val tournamentName: String?,
    /** The lobby that produced this game, or null for non-lobby games. Links games to a tournament. */
    val lobbyId: String? = null,
    /** Matchmaking context: a LobbyGameMode name, or QUICK_GAME / CASUAL for non-lobby games. */
    val gameMode: String?,
    /** True for a ranked (ELO-adjusting) game. */
    val ranked: Boolean = false,
    /** Replay frame count at game-over — the activity measure behind the recording gate. */
    val frameCount: Int,
    /** GameState.turnNumber at game-over. */
    val turnCount: Int,
    val startedAt: Instant?,
    val endedAt: Instant,
    val participants: List<RecordedParticipant>,
)

/** One seat in a finished game. [userId] is null for guests and AI. */
data class RecordedParticipant(
    val userId: UUID?,
    val playerName: String,
    val won: Boolean,
    /** Deck color identity, canonical WUBRG order (e.g. "WU"); empty = colorless. Null if unknown. */
    val colors: String? = null,
    /** Comma-separated distinct set codes in the deck. Null if unknown. */
    val setCodes: String? = null,
    /** True for AI seats (which also have null userId). */
    val isAi: Boolean = false,
    /** Raw connecting IP, admin-only; never sent to clients. Null for AI / unknown. */
    val clientIp: String? = null,
    /** This seat's deck as card-name -> copies, for card-level stats. */
    val deckCards: Map<String, Int> = emptyMap(),
    /**
     * The printing each card was played as, card-name -> `"SET-CN"` ([PrintingRef.identifier]), for
     * the cards the deck pinned to one (a Limited pool card, a chosen art). Absent = default printing.
     */
    val deckPrintings: Map<String, String> = emptyMap(),
)

/** A starting deck split into the per-name copy counts and printings [RecordedParticipant] stores. */
data class RecordedDeck(val cards: Map<String, Int>, val printings: Map<String, String>) {
    companion object {
        /**
         * Split a session deck list — card names, each optionally pinned as `"Name#SET-CN"` — into
         * copies per canonical name and the printing each name was played as. A name whose copies
         * are pinned to different printings records the one most of them use (ties: first seen);
         * a bare `"Name#CN"` pin without a set code can't address a printing and is ignored.
         */
        fun of(deckList: List<String>): RecordedDeck {
            val cards = LinkedHashMap<String, Int>()
            val pins = LinkedHashMap<String, LinkedHashMap<String, Int>>()
            for (entry in deckList) {
                val hash = entry.lastIndexOf('#')
                val name = if (hash < 0) entry else entry.substring(0, hash)
                cards.merge(name, 1, Int::plus)
                if (hash < 0) continue
                val coordinates = entry.substring(hash + 1)
                val dash = coordinates.indexOf('-')
                if (dash <= 0 || dash == coordinates.lastIndex) continue
                pins.getOrPut(name) { LinkedHashMap() }.merge(coordinates, 1, Int::plus)
            }
            val printings = pins.mapValues { (_, counts) -> counts.maxByOrNull { it.value }!!.key }
            return RecordedDeck(cards, printings)
        }
    }
}

/**
 * Records finished games for durable stats. The game-over path calls this unconditionally; which
 * implementation is wired depends on whether accounts are enabled, so [GamePlayHandler] stays
 * decoupled from the persistence layer.
 */
interface MatchResultSink {
    fun record(match: RecordedMatch)
}

/** Default: accounts disabled — stats are not persisted. */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpMatchResultSink : MatchResultSink {
    override fun record(match: RecordedMatch) = Unit
}

/**
 * Accounts enabled: persist the match, but only when at least one seat is a human (signed-in or
 * guest). AI-only games (e.g. the LLM tournament) would otherwise flood the table. Guest seats do not
 * contribute to any user's per-user stats (those filter by user_id) but do count toward the global
 * admin dashboard and geolocation.
 */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class JdbcMatchResultSink(private val matchResults: MatchResultRepository) : MatchResultSink {
    private val logger = LoggerFactory.getLogger(JdbcMatchResultSink::class.java)

    override fun record(match: RecordedMatch) {
        if (match.participants.none { !it.isAi }) return
        matchResults.save(
            MatchResultRow(
                gameId = match.gameId,
                format = match.format,
                tournamentName = match.tournamentName,
                lobbyId = match.lobbyId,
                gameMode = match.gameMode,
                ranked = match.ranked,
                frameCount = match.frameCount,
                turnCount = match.turnCount,
                startedAt = match.startedAt,
                endedAt = match.endedAt,
                participants = match.participants.map {
                    MatchParticipantRow(
                        userId = it.userId,
                        playerName = it.playerName,
                        won = it.won,
                        colors = it.colors,
                        setCodes = it.setCodes,
                        isAi = it.isAi,
                        clientIp = it.clientIp,
                        cards = it.deckCards.map { (name, copies) ->
                            MatchCardRow(cardName = name, copies = copies, printing = it.deckPrintings[name])
                        }.toSet(),
                    )
                }.toSet(),
            )
        )
        logger.debug("Recorded match {} for stats ({} seats)", match.gameId, match.participants.size)
    }
}
