package com.wingedsheep.gameserver.activity

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.gameserver.lobby.QuickGameLobby
import com.wingedsheep.gameserver.lobby.QuickGameLobbyPlayer
import com.wingedsheep.gameserver.lobby.QuickGameLobbyRepository
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.matchmaking.MatchmakingMode
import com.wingedsheep.gameserver.matchmaking.MatchmakingService
import com.wingedsheep.gameserver.matchmaking.QueueKey
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.InMemoryLobbyRepository
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

/**
 * The admin Live games view's per-player activity: the page a client reports and the feed of moves
 * ([PlayerActivityTracker]), and how [PlayerActivityResolver] ranks server state over that page.
 */
class PlayerActivityTest : FunSpec({

    fun identity(name: String) = PlayerIdentity(playerId = EntityId.of("p-$name"), playerName = name)

    context("tracker") {
        test("a page report changes the page and logs it, but home and repeats are quiet") {
            val tracker = PlayerActivityTracker()
            val alice = identity("Alice")
            tracker.onMessage(alice, ClientMessage.ReportActivity("deckbuilder"))
            tracker.onMessage(alice, ClientMessage.ReportActivity("deckbuilder"))
            tracker.onMessage(alice, ClientMessage.ReportActivity("home"))

            tracker.pageOf(alice.token)?.page shouldBe "home"
            tracker.recentFeed().map { it.text } shouldBe listOf("opened the deckbuilder")
            // A page report is not input.
            tracker.lastInputAt(alice.token) shouldBe null
        }

        test("a malformed page is ignored") {
            val tracker = PlayerActivityTracker()
            val alice = identity("Alice")
            tracker.onMessage(alice, ClientMessage.ReportActivity("<script>"))
            tracker.pageOf(alice.token) shouldBe null
        }

        test("lobby and queue moves land in the feed newest first; only the first connect does") {
            val tracker = PlayerActivityTracker()
            var now = Instant.parse("2026-10-09T10:00:00Z")
            tracker.clock = { now }
            val bob = identity("Bob")
            tracker.onMessage(bob, ClientMessage.Connect(playerName = "Bob"))
            now = now.plusSeconds(5)
            tracker.onMessage(bob, ClientMessage.CreateTournamentLobby(setCodes = listOf("BLB"), format = "DRAFT"))
            now = now.plusSeconds(5)
            tracker.onMessage(bob, ClientMessage.Connect(playerName = "Bob"))
            tracker.onMessage(bob, ClientMessage.JoinMatchmaking(mode = MatchmakingMode.RANDOM_DECK))

            tracker.recentFeed().map { it.text } shouldBe listOf(
                "started searching for a match",
                "opened a draft tournament lobby (BLB)",
                "came online",
            )
            tracker.lastInputAt(bob.token) shouldBe now
        }

        test("the feed keeps only the newest entries and forgets departed players") {
            val tracker = PlayerActivityTracker()
            val carol = identity("Carol")
            repeat(PlayerActivityTracker.FEED_SIZE + 5) { tracker.onMessage(carol, ClientMessage.LeaveMatchmaking) }
            tracker.recentFeed().size shouldBe PlayerActivityTracker.FEED_SIZE

            tracker.retainOnly(emptySet())
            tracker.lastInputAt(carol.token) shouldBe null
        }

        test("AI identities are never tracked") {
            val tracker = PlayerActivityTracker()
            val ai = PlayerIdentity(playerId = EntityId.of("ai"), playerName = "Bot", isAi = true)
            tracker.onMessage(ai, ClientMessage.JoinLobby("x"))
            tracker.recentFeed().shouldBeEmpty()
        }
    }

    context("resolver") {
        val games = mockk<GameRepository> { every { findById(any()) } returns null }
        val matchmaking = mockk<MatchmakingService> { every { searchOf(any()) } returns null }

        fun resolver(
            tracker: PlayerActivityTracker = PlayerActivityTracker(),
            lobbies: InMemoryLobbyRepository = InMemoryLobbyRepository(),
            quick: QuickGameLobbyRepository = QuickGameLobbyRepository(),
            mm: MatchmakingService = matchmaking,
        ) = PlayerActivityResolver(games, lobbies, quick, mm, tracker)

        test("with nothing else going on, the reported page decides") {
            val tracker = PlayerActivityTracker()
            val alice = identity("Alice")
            resolver(tracker).resolve(alice).activity.kind shouldBe PlayerActivityResolver.Kind.HOME

            tracker.onMessage(alice, ClientMessage.ReportActivity("deckbuilder"))
            resolver(tracker).resolve(alice).activity.kind shouldBe PlayerActivityResolver.Kind.DECKBUILDER

            tracker.onMessage(alice, ClientMessage.ReportActivity("help"))
            resolver(tracker).resolve(alice).activity.detail shouldBe "Viewing the help page"
        }

        test("a matchmaking search outranks the page, and runs beside a lobby") {
            val alice = identity("Alice")
            val tracker = PlayerActivityTracker().also { it.onMessage(alice, ClientMessage.ReportActivity("deckbuilder")) }
            val searching = mockk<MatchmakingService> {
                every { searchOf(alice.playerId) } returns
                    MatchmakingService.Search(QueueKey(MatchmakingMode.JUMP_IN, null, false), 0L, matchFound = false)
            }
            val solo = resolver(tracker, mm = searching).resolve(alice)
            solo.activity.kind shouldBe PlayerActivityResolver.Kind.SEARCHING
            solo.activity.detail shouldBe "Searching for a match · Jump In"
            solo.searching shouldBe null

            val quick = QuickGameLobbyRepository()
            val lobby = QuickGameLobby(vsAi = true, setCode = null)
            lobby.players += QuickGameLobbyPlayer(alice.playerId, "Alice")
            quick.save(lobby)
            alice.currentQuickGameLobbyId = lobby.lobbyId
            val inLobby = resolver(tracker, quick = quick, mm = searching).resolve(alice)
            inLobby.activity.kind shouldBe PlayerActivityResolver.Kind.LOBBY
            inLobby.activity.detail shouldBe "Quick game lobby (random deck) vs the AI · choosing a deck"
            inLobby.searching shouldBe "Jump In"
        }

        test("a tournament lobby's waiting room names the host") {
            val alice = identity("Alice")
            val lobbies = InMemoryLobbyRepository()
            val lobby = TournamentLobby(setCodes = listOf("BLB"), setNames = listOf("Bloomburrow"), boosterGenerator = mockk<BoosterGenerator>(relaxed = true))
            lobby.addPlayer(alice)
            lobbies.saveLobby(lobby)
            alice.currentLobbyId = lobby.lobbyId

            val activity = resolver(lobbies = lobbies).resolve(alice).activity
            activity.kind shouldBe PlayerActivityResolver.Kind.LOBBY
            activity.detail shouldContain "Hosting a Sealed lobby · Bloomburrow · 1 seated"
        }
    }
})
