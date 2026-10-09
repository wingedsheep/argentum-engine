package com.wingedsheep.gameserver.activity

import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.session.PlayerIdentity
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * What the admin Live overview knows about players beyond the server's own session state: the page
 * each client says it's showing ([ClientMessage.ReportActivity]), when each player last sent a real
 * input, and a short in-memory feed of the moves worth noticing — opening a lobby, joining a queue,
 * submitting a deck. Fed from the single WebSocket dispatch point, so no handler needs to know it
 * exists. Nothing here is persisted; a restart starts the feed empty.
 */
@Component
class PlayerActivityTracker {

    /** Overridable for tests. */
    internal var clock: () -> Instant = Instant::now

    data class PageReport(val page: String, val since: Instant)

    data class FeedEntry(
        val at: Instant,
        val playerName: String,
        /** The account behind the move, when signed in — lets an admin hide their own moves. */
        val userId: UUID?,
        val kind: String,
        val text: String,
    ) {
        val signedIn: Boolean get() = userId != null
    }

    private val pages = ConcurrentHashMap<String, PageReport>()
    private val lastInput = ConcurrentHashMap<String, Instant>()
    private val feed = ArrayDeque<FeedEntry>()

    /** Record one message from an authenticated player. Liveness pings are not input and never reach here. */
    fun onMessage(identity: PlayerIdentity, message: ClientMessage) {
        if (identity.isAi) return
        val now = clock()
        if (message is ClientMessage.ReportActivity) {
            val page = sanitizePage(message.page) ?: return
            val previous = pages[identity.token]
            if (previous?.page == page) return
            pages[identity.token] = PageReport(page, now)
            // Landing back on home is the absence of an activity, not one worth a feed line.
            if (page != HOME) append(identity, now, "page", "opened ${pageLabel(page)}")
            return
        }
        val firstSeen = lastInput.put(identity.token, now) == null
        // A Connect also follows every socket drop (sleep, network blip); only the first is news.
        if (message is ClientMessage.Connect && !firstSeen) return
        describe(message)?.let { (kind, text) -> append(identity, now, kind, text) }
    }

    fun pageOf(token: String): PageReport? = pages[token]

    fun lastInputAt(token: String): Instant? = lastInput[token]

    /** Newest first, leaving out the moves of [excludeUserId]'s account (the admin viewing the feed). */
    fun recentFeed(excludeUserId: UUID? = null): List<FeedEntry> =
        synchronized(feed) { feed.reversed() }.filter { excludeUserId == null || it.userId != excludeUserId }

    /** Drop per-player entries for identities the registry no longer holds. */
    fun retainOnly(liveTokens: Set<String>) {
        pages.keys.retainAll(liveTokens)
        lastInput.keys.retainAll(liveTokens)
    }

    private fun append(identity: PlayerIdentity, at: Instant, kind: String, text: String) {
        synchronized(feed) {
            feed.addLast(FeedEntry(at, identity.playerName, identity.userId, kind, text))
            while (feed.size > FEED_SIZE) feed.removeFirst()
        }
    }

    private fun describe(message: ClientMessage): Pair<String, String>? = when (message) {
        is ClientMessage.Connect -> "connect" to "came online"
        is ClientMessage.CreateTournamentLobby ->
            "lobby" to "opened a ${pretty(message.format)} ${if (message.gameMode == "FREE_FOR_ALL") "free-for-all" else "tournament"} lobby" +
                message.setCodes.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = " (", postfix = ")").orEmpty()
        is ClientMessage.JoinLobby -> "lobby" to "joined a lobby"
        is ClientMessage.StartTournamentLobby -> "lobby" to "started their lobby"
        is ClientMessage.LeaveLobby -> "lobby" to "left a lobby"
        is ClientMessage.CreateQuickGameLobby -> "lobby" to "opened a quick game " + when {
            message.vsAi -> "vs the AI"
            message.twoHeadedGiant -> "lobby (Two-Headed Giant)"
            message.momirBasic -> "lobby (Momir Basic)"
            else -> "lobby"
        }
        is ClientMessage.JoinQuickGameLobby -> "lobby" to "joined a quick game lobby"
        is ClientMessage.LeaveQuickGameLobby -> "lobby" to "left a quick game lobby"
        is ClientMessage.SubmitSealedDeck, is ClientMessage.SubmitQuickGameLobbyDeck -> "deck" to "submitted a deck"
        is ClientMessage.JoinMatchmaking -> "queue" to "started searching for a match"
        is ClientMessage.LeaveMatchmaking -> "queue" to "stopped searching"
        is ClientMessage.SpectateGame -> "watch" to "started spectating a game"
        is ClientMessage.Concede -> "game" to "conceded a game"
        else -> null
    }

    companion object {
        const val HOME = "home"
        const val FEED_SIZE = 80
        private val PAGE_PATTERN = Regex("[a-z0-9-]{1,32}")

        /** Pages are client-supplied: keep them to a short slug so nothing odd lands on the dashboard. */
        fun sanitizePage(raw: String): String? = raw.trim().lowercase().takeIf { PAGE_PATTERN.matches(it) }

        fun pageLabel(page: String): String = when (page) {
            "deckbuilder" -> "the deckbuilder"
            "set-completion" -> "Set Completion"
            "scenario" -> "the scenario builder"
            "learn" -> "Learn to Play"
            "llm-tournament" -> "the LLM tournament"
            "ai-sandbox" -> "the AI sandbox"
            "replay" -> "a replay"
            else -> "the ${page.replace('-', ' ')} page"
        }

        private fun pretty(token: String): String = token.lowercase().replace('_', ' ')
    }
}
