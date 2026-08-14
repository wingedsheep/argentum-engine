package com.wingedsheep.gameserver

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ServerMessage
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Free-for-All lobby mode (multiplayer.md Phase 4), end-to-end over real WebSockets:
 * a 3-player premade-decks FFA pod plays one multiplayer game; a mid-game concede
 * continues the game 2-way (CR 800.4a); the second concede ends it; standings come back
 * as the elimination order; readying up starts a play-again game with the same pod.
 *
 * Also the AI's seat at a multiplayer table: that a pod and a 2HG lobby accept AI players at all,
 * and that a seated AI is wired to the pod's game rather than sitting there holding no-op callbacks.
 *
 * AI deckbuilding is pinned to the deterministic heuristic builder: left to the server's own config
 * these tests would call an LLM whenever the machine running them happens to export an API key.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["game.ai.heuristic-deckbuilding=true"],
)
class FreeForAllLobbyTest : FunSpec() {

    @LocalServerPort
    private var port: Int = 0

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        serializersModule = engineSerializersModule
    }

    private val activeClients = mutableListOf<FfaTestClient>()
    private val activeContainers = mutableListOf<org.apache.tomcat.websocket.WsWebSocketContainer>()

    override suspend fun afterTest(testCase: io.kotest.core.test.TestCase, result: io.kotest.engine.test.TestResult) {
        activeClients.forEach { it.close() }
        activeClients.clear()
        activeContainers.forEach { runCatching { it.destroy() } }
        activeContainers.clear()
        super.afterTest(testCase, result)
    }

    private fun wsUrl(): String = "ws://localhost:$port/game"

    private fun createWsContainer() = org.apache.tomcat.websocket.WsWebSocketContainer().apply {
        defaultMaxSessionIdleTimeout = 300_000L
        defaultMaxTextMessageBufferSize = 1024 * 1024
        defaultMaxBinaryMessageBufferSize = 1024 * 1024
    }.also { activeContainers.add(it) }

    private fun createClient(): FfaTestClient {
        val client = FfaTestClient(json, createWsContainer(), wsUrl())
        activeClients.add(client)
        return client
    }

    init {
        test("3-player premade FFA pod: one game, mid-game concede continues, standings = elimination order, play again") {
            val forestDeck = mapOf("Forest" to 40)

            // ── Lobby: host creates an FFA premade lobby, two players join ──
            val alice = createClient()
            val aliceConnected = alice.connectAs("Alice")
            alice.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "PREMADE_DECKS",
                maxPlayers = 4,
                gameMode = "FREE_FOR_ALL",
            ))
            eventually(5.seconds) {
                alice.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            val lobbyId = alice.messages.filterIsInstance<ServerMessage.LobbyCreated>().first().lobbyId

            val bob = createClient()
            val bobConnected = bob.connectAs("Bob")
            bob.send(ClientMessage.JoinLobby(lobbyId))
            val charlie = createClient()
            val charlieConnected = charlie.connectAs("Charlie")
            charlie.send(ClientMessage.JoinLobby(lobbyId))

            eventually(5.seconds) {
                alice.latestLobbyUpdate()?.players?.size shouldBe 3
                alice.latestLobbyUpdate()?.settings?.gameMode shouldBe "FREE_FOR_ALL"
            }

            // ── Decks: everyone submits a premade deck, host starts ──
            for (client in listOf(alice, bob, charlie)) {
                client.send(ClientMessage.SubmitSealedDeck(deckList = forestDeck))
            }
            eventually(5.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.any { it is ServerMessage.DeckSubmitted }
                } shouldBe true
            }
            alice.send(ClientMessage.StartTournamentLobby)

            // ── One multiplayer game seats all three players ──
            eventually(10.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.any { it is ServerMessage.FreeForAllGameStarting }
                } shouldBe true
            }
            val starting = alice.messages.filterIsInstance<ServerMessage.FreeForAllGameStarting>().first()
            starting.gameNumber shouldBe 1
            starting.players shouldHaveSize 3
            starting.players.count { it.isYou } shouldBe 1
            val gameSessionId = starting.gameSessionId

            // Every seat gets the 3-player roster and a mulligan decision.
            eventually(10.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.filterIsInstance<ServerMessage.GameStarted>().any { it.players.size == 3 } &&
                        c.messages.any { it is ServerMessage.MulliganDecision }
                } shouldBe true
            }
            for (client in listOf(alice, bob, charlie)) {
                client.send(ClientMessage.KeepHand)
            }
            eventually(10.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.any { it is ServerMessage.StateUpdate }
                } shouldBe true
            }

            // ── Mid-game concede: Charlie leaves, the game continues 2-way (CR 800.4a) ──
            charlie.send(ClientMessage.Concede)
            eventually(10.seconds) {
                charlie.messages.any { it is ServerMessage.PlayerEliminated } shouldBe true
            }
            // The remaining players see Charlie's elimination in the rebroadcast state.
            // (Routine updates arrive as deltas; request a resync to get a full state to assert on.)
            alice.send(ClientMessage.RequestResync)
            eventually(10.seconds) {
                val state = alice.latestState()
                state.shouldNotBeNull()
                state.players.first { it.playerId.value == charlieConnected.playerId }.hasLost shouldBe true
            }
            // No game over — Alice and Bob play on (CR 800.4a).
            alice.messages.none { it is ServerMessage.GameOver } shouldBe true
            bob.messages.none { it is ServerMessage.GameOver } shouldBe true

            // ── Second concede ends the game; standings are the elimination order ──
            bob.send(ClientMessage.Concede)
            eventually(10.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.any { it is ServerMessage.FreeForAllGameComplete }
                } shouldBe true
            }
            val gameOver = alice.messages.filterIsInstance<ServerMessage.GameOver>().first()
            gameOver.winnerId?.value shouldBe aliceConnected.playerId
            gameOver.gameId shouldBe gameSessionId

            val complete = alice.messages.filterIsInstance<ServerMessage.FreeForAllGameComplete>().first()
            complete.gamesPlayed shouldBe 1
            complete.standings shouldHaveSize 3
            complete.standings.map { it.placement } shouldBe listOf(1, 2, 3)
            complete.standings[0].playerId shouldBe aliceConnected.playerId   // winner
            complete.standings[1].playerId shouldBe bobConnected.playerId     // eliminated last
            complete.standings[2].playerId shouldBe charlieConnected.playerId // eliminated first

            // ── Play again: all three ready up, a second game starts with the same pod ──
            for (client in listOf(alice, bob, charlie)) {
                client.send(ClientMessage.ReadyForNextRound)
            }
            eventually(10.seconds) {
                listOf(alice, bob, charlie).all { c ->
                    c.messages.filterIsInstance<ServerMessage.FreeForAllGameStarting>()
                        .any { it.gameNumber == 2 }
                } shouldBe true
            }
            val secondGame = alice.messages.filterIsInstance<ServerMessage.FreeForAllGameStarting>()
                .first { it.gameNumber == 2 }
            secondGame.players shouldHaveSize 3
            (secondGame.gameSessionId == gameSessionId) shouldBe false
        }

        test("FFA lobby seats AI players and caps the pod at 6") {
            val host = createClient()
            host.connectAs("Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "SEALED",
                maxPlayers = 8,
                gameMode = "FREE_FOR_ALL",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            eventually(5.seconds) {
                // maxPlayers is mode-capped at 6 even though the client asked for 8
                host.latestLobbyUpdate()?.settings?.maxPlayers shouldBe 6
            }

            // An AI takes a pod seat like any other player: nothing about the Table axis decides
            // whether it may sit down, and the engine AI reads a pod as N opposing sides.
            repeat(3) { host.send(ClientMessage.AddAiToLobby) }
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 4
            }
            host.latestLobbyUpdate()?.players?.count { it.isAi } shouldBe 3
            host.messages.none { it is ServerMessage.Error } shouldBe true

            // The cap is the pod's, not the roster's — six seats, three of them still open.
            repeat(3) { host.send(ClientMessage.AddAiToLobby) }
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 6
            }
            host.send(ClientMessage.AddAiToLobby)
            eventually(5.seconds) {
                host.messages.filterIsInstance<ServerMessage.Error>()
                    .any { it.message.contains("full") } shouldBe true
            }
            host.latestLobbyUpdate()?.players?.size shouldBe 6
        }

        test("a Two-Headed Giant lobby seats AI teammates and opponents") {
            val host = createClient()
            host.connectAs("2HG Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "SEALED",
                maxPlayers = 4,
                gameMode = "TWO_HEADED_GIANT",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }

            // Three AI fill out the two teams of two (CR 810) — one of them is the host's teammate.
            repeat(3) { host.send(ClientMessage.AddAiToLobby) }
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 4
            }
            host.latestLobbyUpdate()?.players?.count { it.isAi } shouldBe 3
            host.messages.none { it is ServerMessage.Error } shouldBe true
        }

        test("switching a lobby holding AI players to a multiplayer table is allowed") {
            val host = createClient()
            host.connectAs("Mode Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "SEALED",
                maxPlayers = 4,
                gameMode = "TOURNAMENT",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            host.send(ClientMessage.AddAiToLobby)
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 2
            }

            // The seat counts are the only thing a mode switch has to reconcile now; a pod of two
            // (you and one AI) is a legal Free-for-All.
            host.send(ClientMessage.UpdateLobbySettings(gameMode = "FREE_FOR_ALL"))
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.settings?.gameMode shouldBe "FREE_FOR_ALL"
            }
            host.latestLobbyUpdate()?.players?.count { it.isAi } shouldBe 1
            host.messages.none { it is ServerMessage.Error } shouldBe true
        }

        test("an AI seated in a premade-decks pod is dealt a deck, the way a quick game rolls one") {
            // The AI has no deck to bring, which used to make premade decks the one format it was
            // refused from. A quick game had always answered that by generating one for it; this is
            // the same answer in a lobby, so "just me, my own deck, at a pod" reaches a table.
            val host = createClient()
            host.connectAs("Premade Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "PREMADE_DECKS",
                maxPlayers = 4,
                gameMode = "FREE_FOR_ALL",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }

            repeat(3) { host.send(ClientMessage.AddAiToLobby) }
            eventually(15.seconds) {
                val players = host.latestLobbyUpdate()?.players
                players?.size shouldBe 4
                // Dealt at the moment they sit down: the premade start gate wants every seat to have
                // submitted, so an AI that arrived empty-handed would block the host forever.
                players?.filter { it.isAi }?.all { it.deckSubmitted } shouldBe true
            }
            host.messages.none { it is ServerMessage.Error } shouldBe true

            host.send(ClientMessage.SubmitSealedDeck(deckList = mapOf("Forest" to 40)))
            host.send(ClientMessage.StartTournamentLobby)
            eventually(30.seconds) {
                host.messages.any { it is ServerMessage.FreeForAllGameStarting } shouldBe true
            }
            host.messages.filterIsInstance<ServerMessage.FreeForAllGameStarting>()
                .first().players shouldHaveSize 4
        }

        test("the host picks each AI seat's deck separately, and only that seat's changes") {
            val host = createClient()
            host.connectAs("Deck Picker Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "PREMADE_DECKS",
                maxPlayers = 4,
                gameMode = "FREE_FOR_ALL",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            repeat(2) { host.send(ClientMessage.AddAiToLobby) }
            eventually(15.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 3
            }
            // Both seats start on Auto — the rolled deck, which is what they were dealt on arrival.
            host.latestLobbyUpdate()?.players?.filter { it.isAi }?.all { it.aiDeck?.kind == "auto" } shouldBe true

            val aiSeats = host.latestLobbyUpdate()!!.players.filter { it.isAi }
            val chosen = aiSeats[0].playerId
            val untouched = aiSeats[1].playerId

            // An exact list, the way the "Pick a deck" tab sends one. Every name is checked against
            // the registry on arrival, so these are real Portal cards.
            val burn = mapOf("Mountain" to 32, "Lava Axe" to 4, "Raging Goblin" to 4)
            host.send(ClientMessage.SetLobbyAiDeck(
                playerId = chosen,
                spec = com.wingedsheep.gameserver.lobby.AiDeckSpec.Fixed(burn, label = "Burn"),
            ))
            eventually(10.seconds) {
                host.messages.filterIsInstance<ServerMessage.Error>().map { it.message } shouldBe emptyList()
                val seat = host.latestLobbyUpdate()?.players?.first { it.playerId == chosen }
                seat?.aiDeck?.kind shouldBe "deck"
                seat?.aiDeck?.label shouldBe "Burn"
                seat?.aiDeck?.cardCount shouldBe burn.values.sum()
                // Re-rolled onto the chosen list, so the seat is still ready to start.
                seat?.deckSubmitted shouldBe true
            }
            // Per seat means per seat: the other AI is still on what it was dealt.
            host.latestLobbyUpdate()?.players?.first { it.playerId == untouched }?.aiDeck?.kind shouldBe "auto"
            host.messages.none { it is ServerMessage.Error } shouldBe true

            // Pinning the pool to sets is the middle answer, and it round-trips as one.
            host.send(ClientMessage.SetLobbyAiDeck(
                playerId = untouched,
                spec = com.wingedsheep.gameserver.lobby.AiDeckSpec.Sets(listOf("POR")),
            ))
            eventually(15.seconds) {
                val seat = host.latestLobbyUpdate()?.players?.first { it.playerId == untouched }
                seat?.aiDeck?.kind shouldBe "sets"
                seat?.aiDeck?.setCodes shouldBe listOf("POR")
                seat?.deckSubmitted shouldBe true
            }
        }

        test("choosing an AI's deck is refused in a limited lobby, where it builds from its pool") {
            val host = createClient()
            host.connectAs("Sealed Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "SEALED",
                maxPlayers = 4,
                gameMode = "FREE_FOR_ALL",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            host.send(ClientMessage.AddAiToLobby)
            eventually(10.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 2
            }
            // No pool yet, so no deck either — and no choice to make about it.
            host.latestLobbyUpdate()?.players?.first { it.isAi }?.deckSubmitted shouldBe false

            val aiId = host.latestLobbyUpdate()!!.players.first { it.isAi }.playerId
            host.send(ClientMessage.SetLobbyAiDeck(
                playerId = aiId,
                spec = com.wingedsheep.gameserver.lobby.AiDeckSpec.Fixed(mapOf("Forest" to 40)),
            ))
            eventually(5.seconds) {
                host.messages.filterIsInstance<ServerMessage.Error>()
                    .any { it.message.contains("pool it is dealt") } shouldBe true
            }
        }

        test("a Commander premade lobby deals an AI its own commander deck, and the host can override it") {
            // Adding the AI is enough: the server builds it a legal Commander deck and designates a
            // commander, the same way a quick game rolls one. Before there was a builder for that
            // shape the seat sat there deckless until the host picked a list for it.
            val host = createClient()
            host.connectAs("Commander Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "PREMADE_DECKS",
                maxPlayers = 4,
                gameMode = "FREE_FOR_ALL",
                rules = "COMMANDER",
            ))
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.settings?.rules shouldBe "COMMANDER"
            }

            host.send(ClientMessage.AddAiToLobby)
            eventually(10.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 2
            }
            val ai = host.latestLobbyUpdate()!!.players.first { it.isAi }
            eventually(20.seconds) {
                host.latestLobbyUpdate()?.players?.first { it.isAi }?.deckSubmitted shouldBe true
            }

            host.send(ClientMessage.SetLobbyAiDeck(
                playerId = ai.playerId,
                spec = com.wingedsheep.gameserver.lobby.AiDeckSpec.Fixed(
                    deckList = mapOf("Plains" to 99),
                    label = "Zetalpa",
                    commander = "Zetalpa, Primal Dawn",
                ),
            ))
            // The seat already had a deck, so `deckSubmitted` can't tell the override apart from the
            // generated one — the commander on the spec is what changed.
            eventually(10.seconds) {
                val seat = host.latestLobbyUpdate()?.players?.first { it.isAi }
                seat?.deckSubmitted shouldBe true
                seat?.aiDeck?.commander shouldBe "Zetalpa, Primal Dawn"
            }
        }

        test("a premade lobby holding an AI re-rolls its deck when the host switches to Commander") {
            // `resyncAiDecks` drops the AI's non-commander deck on the switch; what it builds instead
            // has to be a legal commander deck, or the seat would sit there deckless and the lobby
            // would never be startable.
            val host = createClient()
            host.connectAs("Rules Switch Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "PREMADE_DECKS",
                maxPlayers = 4,
                gameMode = "FREE_FOR_ALL",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            host.send(ClientMessage.AddAiToLobby)
            eventually(10.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 2
            }

            host.send(ClientMessage.UpdateLobbySettings(rules = "COMMANDER"))
            eventually(20.seconds) {
                host.latestLobbyUpdate()?.settings?.rules shouldBe "COMMANDER"
                host.latestLobbyUpdate()?.players?.first { it.isAi }?.deckSubmitted shouldBe true
            }
        }

        test("a sealed FFA pod of one human and two AI builds its decks, starts, and the AI plays") {
            // The end-to-end proof that an AI seat is wired to the pod's game rather than merely
            // allowed into the lobby. Every assertion after the game starts depends on
            // `FreeForAllHandler.wireAiSeats`: without it the AI holds the no-op callbacks
            // `createAiIdentity` gave it, never answers its mulligan, and the pod hangs there.
            val host = createClient()
            host.connectAs("Pod Human")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "SEALED",
                maxPlayers = 3,
                gameMode = "FREE_FOR_ALL",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            repeat(2) { host.send(ClientMessage.AddAiToLobby) }
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 3
            }

            host.send(ClientMessage.StartTournamentLobby)
            // Sealed pools are dealt to every seat, the AI ones auto-built in the background.
            eventually(10.seconds) {
                host.messages.any { it is ServerMessage.SealedPoolGenerated } shouldBe true
            }

            // Basics are always legal in a sealed deck, so the human's deck needs nothing from the
            // pool — this test is about the pod, not about deckbuilding.
            host.send(ClientMessage.SubmitSealedDeck(deckList = mapOf("Forest" to 40)))

            // The last deck in starts the pod game — and on this path the last deck is often an
            // AI's, submitted from `launchAiDeckBuilding`'s coroutine rather than a WebSocket.
            eventually(60.seconds) {
                host.messages.any { it is ServerMessage.FreeForAllGameStarting } shouldBe true
            }
            val starting = host.messages.filterIsInstance<ServerMessage.FreeForAllGameStarting>().first()
            starting.players shouldHaveSize 3

            eventually(30.seconds) {
                host.messages.filterIsInstance<ServerMessage.GameStarted>().any { it.players.size == 3 } shouldBe true
                host.messages.any { it is ServerMessage.MulliganDecision } shouldBe true
            }
            host.send(ClientMessage.KeepHand)

            // Both AI seats answered their own mulligans, so the game left the mulligan phase and
            // is running. A pod with an unwired AI never gets here.
            eventually(30.seconds) {
                host.messages.any { it is ServerMessage.StateUpdate } shouldBe true
            }
            host.send(ClientMessage.RequestResync)
            eventually(30.seconds) {
                val state = host.latestState()
                state.shouldNotBeNull()
                state.players shouldHaveSize 3
            }

            // Once the only human concedes there is nobody left for the AI-only table to serve.
            // The server finalizes it through the normal completion path instead of letting both
            // AI controllers keep playing an invisible game in the background.
            host.send(ClientMessage.Concede)
            eventually(10.seconds) {
                host.messages.any { it is ServerMessage.FreeForAllGameComplete } shouldBe true
            }
            host.messages.filterIsInstance<ServerMessage.GameOver>()
                .any { it.gameId == starting.gameSessionId } shouldBe true
        }

        test("4-player Commander FFA pod: every seat starts at 40 life with its own commander in its own command zone") {
            // Reached the old way, through commander deck legality — which is now the *back-compat
            // inference* path: this lobby never sends `rules`, exactly as an older client wouldn't, so
            // setting a commander-shaped `deckFormat` has to default the Rules axis to Commander. A
            // saved premade Commander lobby must keep playing Commander, and this is what proves it.
            // The explicit `rules = "COMMANDER"` path is the test below.
            //
            // A mono-green commander behind 99 Forests: 100 cards including the commander, singleton
            // apart from basics, one colour identity — the smallest deck that passes the Commander
            // validator, so the test is about the pod rather than about deck construction.
            val commanderName = "Dwynen, Gilt-Leaf Daen"
            val commanderDeck = mapOf("Forest" to 99, commanderName to 1)

            val host = createClient()
            val hostConnected = host.connectAs("Pod Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "PREMADE_DECKS",
                maxPlayers = 4,
                gameMode = "FREE_FOR_ALL",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            val lobbyId = host.messages.filterIsInstance<ServerMessage.LobbyCreated>().first().lobbyId

            val guests = listOf("Pod Two", "Pod Three", "Pod Four").map { name ->
                createClient().also { it.connectAs(name); it.send(ClientMessage.JoinLobby(lobbyId)) }
            }
            val pod = listOf(host) + guests
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 4
            }

            // Commander shape comes from the deck-construction format on a premade lobby.
            host.send(ClientMessage.UpdateLobbySettings(deckFormat = "COMMANDER"))
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.settings?.deckFormat shouldBe "COMMANDER"
            }

            for (client in pod) {
                client.send(ClientMessage.SubmitSealedDeck(
                    deckList = commanderDeck,
                    commander = commanderName,
                ))
            }
            eventually(5.seconds) {
                pod.all { c -> c.messages.any { it is ServerMessage.DeckSubmitted } } shouldBe true
            }
            // Nothing was rejected — a missing or off-identity commander surfaces as INVALID_DECK.
            host.messages.none { it is ServerMessage.Error } shouldBe true

            host.send(ClientMessage.StartTournamentLobby)

            // One four-seat game, not a bracket of 1v1 matches.
            eventually(15.seconds) {
                pod.all { c -> c.messages.any { it is ServerMessage.FreeForAllGameStarting } } shouldBe true
            }
            val starting = host.messages.filterIsInstance<ServerMessage.FreeForAllGameStarting>().first()
            starting.players shouldHaveSize 4

            eventually(15.seconds) {
                pod.all { c ->
                    c.messages.filterIsInstance<ServerMessage.GameStarted>().any { it.players.size == 4 } &&
                        c.messages.any { it is ServerMessage.MulliganDecision }
                } shouldBe true
            }
            for (client in pod) client.send(ClientMessage.KeepHand)

            // Routine updates are deltas; resync for a full state to assert the pod's shape on.
            host.send(ClientMessage.RequestResync)
            eventually(15.seconds) {
                val state = host.latestState()
                state.shouldNotBeNull()
                state.players shouldHaveSize 4
                // Commander life, not the 20 a Standard game would give — and per player, since
                // Commander shares nothing.
                state.players.all { it.life == 40 } shouldBe true
                // Each of the four seats has its own one-card command zone.
                for (player in state.players) {
                    val commandZone = state.zones.firstOrNull {
                        it.zoneId.ownerId == player.playerId &&
                            it.zoneId.zoneType == com.wingedsheep.sdk.core.Zone.COMMAND
                    }
                    commandZone.shouldNotBeNull()
                    commandZone.size shouldBe 1
                }
                // The host's own commander is visible and flagged as such.
                val ownCommander = state.zones
                    .first {
                        it.zoneId.ownerId.value == hostConnected.playerId &&
                            it.zoneId.zoneType == com.wingedsheep.sdk.core.Zone.COMMAND
                    }
                    .cardIds.single()
                val card = state.cards.getValue(ownCommander)
                card.name shouldBe commanderName
                card.isCommander shouldBe true
            }
        }

        test("4-player Commander FFA pod reached explicitly through the Rules axis, with no deck-format restriction") {
            // The path the current client takes: Commander is asked for as *rules*, not smuggled in as
            // deck legality. Nothing here restricts what may go in a deck — the lobby's deckFormat
            // stays null — which is exactly the separation the axis exists for, and it still has to
            // produce a Commander game: 40 life and a command zone per seat.
            val commanderName = "Dwynen, Gilt-Leaf Daen"
            val commanderDeck = mapOf("Forest" to 99, commanderName to 1)

            val host = createClient()
            host.connectAs("Rules Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "PREMADE_DECKS",
                maxPlayers = 4,
                gameMode = "FREE_FOR_ALL",
                rules = "COMMANDER",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            val lobbyId = host.messages.filterIsInstance<ServerMessage.LobbyCreated>().first().lobbyId

            val guests = listOf("Rules Two", "Rules Three", "Rules Four").map { name ->
                createClient().also { it.connectAs(name); it.send(ClientMessage.JoinLobby(lobbyId)) }
            }
            val pod = listOf(host) + guests
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 4
            }
            // The axis is reported back on its own field, and it did not drag a deck restriction in.
            host.latestLobbyUpdate()?.settings?.rules shouldBe "COMMANDER"
            host.latestLobbyUpdate()?.settings?.deckFormat shouldBe null

            for (client in pod) {
                client.send(ClientMessage.SubmitSealedDeck(
                    deckList = commanderDeck,
                    commander = commanderName,
                ))
            }
            eventually(5.seconds) {
                pod.all { c -> c.messages.any { it is ServerMessage.DeckSubmitted } } shouldBe true
            }
            host.messages.none { it is ServerMessage.Error } shouldBe true

            host.send(ClientMessage.StartTournamentLobby)
            eventually(15.seconds) {
                pod.all { c -> c.messages.any { it is ServerMessage.FreeForAllGameStarting } } shouldBe true
            }
            eventually(15.seconds) {
                pod.all { c ->
                    c.messages.filterIsInstance<ServerMessage.GameStarted>().any { it.players.size == 4 } &&
                        c.messages.any { it is ServerMessage.MulliganDecision }
                } shouldBe true
            }
            for (client in pod) client.send(ClientMessage.KeepHand)

            host.send(ClientMessage.RequestResync)
            eventually(15.seconds) {
                val state = host.latestState()
                state.shouldNotBeNull()
                state.players shouldHaveSize 4
                state.players.all { it.life == 40 } shouldBe true
                for (player in state.players) {
                    val commandZone = state.zones.firstOrNull {
                        it.zoneId.ownerId == player.playerId &&
                            it.zoneId.zoneType == com.wingedsheep.sdk.core.Zone.COMMAND
                    }
                    commandZone.shouldNotBeNull()
                    commandZone.size shouldBe 1
                }
            }
        }

        test("Two-Headed Giant refuses Commander rules before touching submitted decks") {
            val host = createClient()
            host.connectAs("Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "PREMADE_DECKS",
                maxPlayers = 4,
                gameMode = "TWO_HEADED_GIANT",
            ))
            eventually(5.seconds) {
                host.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
            }
            val lobbyId = host.messages.filterIsInstance<ServerMessage.LobbyCreated>().first().lobbyId

            for (name in listOf("Teammate", "Opponent A", "Opponent B")) {
                createClient().also { client ->
                    client.connectAs(name)
                    client.send(ClientMessage.JoinLobby(lobbyId))
                }
            }
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.players?.size shouldBe 4
            }

            // Commander deck legality defaults the Rules axis, and it means it: CR 903.4 anchors
            // colour identity to the commander, so there is no such thing as Commander-legal deck
            // construction without Commander rules. Asking for it at a 2HG table is therefore asking
            // for Commander at a 2HG table, and it is refused on the spot — not accepted and refused
            // at Start, which would leave a lobby the host can't start and can't see why.
            host.send(ClientMessage.UpdateLobbySettings(deckFormat = "COMMANDER"))
            eventually(5.seconds) {
                host.messages.filterIsInstance<ServerMessage.Error>().any {
                    it.message.contains("Two-Headed Giant") && it.message.contains("Commander")
                } shouldBe true
            }
            // Nothing was written: the refused message left a coherent 2HG lobby behind, rather than
            // a Commander-legality one that could never run.
            host.latestLobbyUpdate()?.settings?.deckFormat shouldBe null
            host.latestLobbyUpdate()?.settings?.rules shouldBe "STANDARD"

            // A legality that doesn't imply Commander rules is unaffected — the restriction is a
            // consequence of the Rules × Table conflict, not a blanket ban on the dropdown.
            host.send(ClientMessage.UpdateLobbySettings(deckFormat = "MODERN"))
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.settings?.deckFormat shouldBe "MODERN"
            }
            host.latestLobbyUpdate()?.settings?.rules shouldBe "STANDARD"
        }

        test("switching a Commander lobby to Two-Headed Giant is refused at the switch, not at Start") {
            // The other direction of the same conflict, and the reason it is one shared statement: a
            // host who is told "no" only after pressing Start has already assembled a lobby around a
            // combination that was never going to run.
            val host = createClient()
            host.connectAs("Switch Host")
            host.send(ClientMessage.CreateTournamentLobby(
                setCodes = listOf("POR"),
                format = "PREMADE_DECKS",
                maxPlayers = 4,
                gameMode = "FREE_FOR_ALL",
                rules = "COMMANDER",
            ))
            eventually(5.seconds) {
                host.latestLobbyUpdate()?.settings?.rules shouldBe "COMMANDER"
            }

            host.send(ClientMessage.UpdateLobbySettings(gameMode = "TWO_HEADED_GIANT"))
            eventually(5.seconds) {
                host.messages.filterIsInstance<ServerMessage.Error>().any {
                    it.message.contains("Two-Headed Giant") && it.message.contains("Commander")
                } shouldBe true
            }
            // The lobby is unchanged: the rejected switch left a coherent Commander pod behind.
            host.latestLobbyUpdate()?.settings?.gameMode shouldBe "FREE_FOR_ALL"
            host.latestLobbyUpdate()?.settings?.rules shouldBe "COMMANDER"
        }
    }

    // =========================================================================
    // Test client (mirrors SealedTournamentReconnectionTest's harness)
    // =========================================================================

    data class ConnectResult(val playerId: String, val token: String)

    inner class FfaTestClient(
        private val json: Json,
        private val container: jakarta.websocket.WebSocketContainer,
        private val url: String
    ) {
        private var session: WebSocketSession? = null
        val messages = CopyOnWriteArrayList<ServerMessage>()
        private val connectLatch = CountDownLatch(1)
        private val closed = AtomicBoolean(false)

        suspend fun connect() {
            withContext(Dispatchers.IO) {
                val client = StandardWebSocketClient(container)
                session = client.execute(
                    object : TextWebSocketHandler() {
                        override fun afterConnectionEstablished(session: WebSocketSession) {
                            connectLatch.countDown()
                        }

                        override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                            if (closed.get()) return
                            try {
                                messages.add(json.decodeFromString<ServerMessage>(message.payload))
                            } catch (_: Exception) {}
                        }

                        override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
                            closed.set(true)
                        }

                        override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
                            closed.set(true)
                        }
                    },
                    WebSocketHttpHeaders(),
                    URI.create(url)
                ).get(10, TimeUnit.SECONDS)

                if (!connectLatch.await(10, TimeUnit.SECONDS)) {
                    error("WebSocket connection timeout")
                }
            }
        }

        fun send(message: ClientMessage) {
            check(!closed.get()) { "Cannot send on closed connection" }
            check(session?.isOpen == true) { "WebSocket session is not open" }
            session?.sendMessage(TextMessage(json.encodeToString(message)))
        }

        fun close() {
            if (closed.compareAndSet(false, true)) {
                runCatching { session?.close() }
            }
        }

        suspend fun connectAs(playerName: String): ConnectResult {
            connect()
            send(ClientMessage.Connect(playerName))
            eventually(5.seconds) {
                messages.any { it is ServerMessage.Connected } shouldBe true
            }
            val connected = messages.filterIsInstance<ServerMessage.Connected>().first()
            return ConnectResult(connected.playerId, connected.token)
        }

        fun latestLobbyUpdate(): ServerMessage.LobbyUpdate? =
            messages.filterIsInstance<ServerMessage.LobbyUpdate>().lastOrNull()

        fun latestState(): com.wingedsheep.engine.view.ClientGameState? =
            messages.filterIsInstance<ServerMessage.StateUpdate>().lastOrNull()?.state
    }
}
