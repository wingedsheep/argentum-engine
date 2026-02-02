package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.wingedsheep.engine.core.engineSerializersModule
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test for sealed tournament with 5 players.
 * Tests state preservation across page refreshes (reconnections) at every stage:
 * - WAITING_FOR_PLAYERS
 * - DECK_BUILDING
 * - TOURNAMENT_ACTIVE
 * - TOURNAMENT_COMPLETE
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SealedTournamentReconnectionTest : FunSpec() {

    @LocalServerPort
    private var port: Int = 0

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        serializersModule = engineSerializersModule
    }

    private val activeClients = mutableListOf<TournamentTestClient>()

    override suspend fun afterTest(testCase: io.kotest.core.test.TestCase, result: io.kotest.core.test.TestResult) {
        activeClients.forEach { it.close() }
        activeClients.clear()
        super.afterTest(testCase, result)
    }

    private fun wsUrl(): String = "ws://localhost:$port/game"

    private fun createWsContainer() = org.apache.tomcat.websocket.WsWebSocketContainer().apply {
        defaultMaxSessionIdleTimeout = 300_000L
        defaultMaxTextMessageBufferSize = 1024 * 1024
        defaultMaxBinaryMessageBufferSize = 1024 * 1024
    }

    private fun createClient(): TournamentTestClient {
        val client = TournamentTestClient(json, createWsContainer(), wsUrl())
        activeClients.add(client)
        return client
    }

    init {
        context("Sealed Tournament with 5 Players - State Preservation") {

            test("full tournament flow with reconnection at each stage") {
                val playerNames = listOf("Alice", "Bob", "Charlie", "Diana", "Eve")
                val players = mutableListOf<PlayerTestContext>()

                // =========================================================================
                // Stage 1: WAITING_FOR_PLAYERS - Create lobby and add players
                // =========================================================================

                // Host creates lobby
                val hostClient = createClient()
                val hostConnected = hostClient.connectAs(playerNames[0])
                hostClient.send(ClientMessage.CreateTournamentLobby(
                    setCodes = listOf("POR"),
                    format = "SEALED",
                    boosterCount = 6,
                    maxPlayers = 8
                ))

                eventually(5.seconds) {
                    hostClient.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
                }

                val lobbyId = hostClient.messages
                    .filterIsInstance<ServerMessage.LobbyCreated>()
                    .first().lobbyId

                players.add(PlayerTestContext(
                    name = playerNames[0],
                    playerId = EntityId.of(hostConnected.playerId),
                    token = hostConnected.token,
                    client = hostClient,
                    isHost = true
                ))

                // Other players join
                for (i in 1 until 5) {
                    val client = createClient()
                    val connected = client.connectAs(playerNames[i])
                    client.send(ClientMessage.JoinLobby(lobbyId))

                    eventually(5.seconds) {
                        client.messages.any { it is ServerMessage.LobbyUpdate } shouldBe true
                    }

                    players.add(PlayerTestContext(
                        name = playerNames[i],
                        playerId = EntityId.of(connected.playerId),
                        token = connected.token,
                        client = client,
                        isHost = false
                    ))
                }

                // Verify all players see 5 players in lobby
                eventually(5.seconds) {
                    val hostLobbyUpdate = hostClient.latestLobbyUpdate()
                    hostLobbyUpdate?.players?.size shouldBe 5
                }

                // Test reconnection during WAITING_FOR_PLAYERS for player 3
                val player3 = players[2]
                player3.client.close()
                activeClients.remove(player3.client)

                val newClient3 = createClient()
                val reconnected3 = newClient3.reconnectAs(player3.name, player3.token)
                reconnected3.context shouldBe "lobby"
                reconnected3.contextId shouldBe lobbyId

                eventually(5.seconds) {
                    newClient3.latestLobbyUpdate()?.players?.size shouldBe 5
                }

                players[2] = player3.copy(client = newClient3)

                // =========================================================================
                // Stage 2: DECK_BUILDING - Start tournament
                // =========================================================================

                // Host starts the tournament
                players[0].client.send(ClientMessage.StartTournamentLobby)

                // All players should receive their sealed pool
                eventually(10.seconds) {
                    players.all { p ->
                        p.client.messages.any { it is ServerMessage.SealedPoolGenerated }
                    } shouldBe true
                }

                // Verify each player got cards
                for (player in players) {
                    val poolMsg = player.client.messages
                        .filterIsInstance<ServerMessage.SealedPoolGenerated>()
                        .first()
                    poolMsg.cardPool.size shouldBeGreaterThan 0
                }

                // Test reconnection during DECK_BUILDING for player 2
                val player2 = players[1]
                val player2PoolBefore = player2.client.messages
                    .filterIsInstance<ServerMessage.SealedPoolGenerated>()
                    .first().cardPool

                player2.client.close()
                activeClients.remove(player2.client)

                val newClient2 = createClient()
                val reconnected2 = newClient2.reconnectAs(player2.name, player2.token)
                reconnected2.context shouldBe "deckBuilding"
                reconnected2.contextId shouldBe lobbyId

                // Should receive pool again after reconnection
                eventually(5.seconds) {
                    newClient2.messages.any { it is ServerMessage.SealedPoolGenerated } shouldBe true
                }

                val player2PoolAfter = newClient2.messages
                    .filterIsInstance<ServerMessage.SealedPoolGenerated>()
                    .first().cardPool

                // Pool should be the same size
                player2PoolAfter.size shouldBe player2PoolBefore.size

                players[1] = player2.copy(client = newClient2)

                // =========================================================================
                // Stage 3: Submit decks
                // =========================================================================

                // Build a simple deck from pool for each player
                for (player in players) {
                    val pool = player.client.messages
                        .filterIsInstance<ServerMessage.SealedPoolGenerated>()
                        .first()

                    // Pick spells from pool (or as many as available)
                    val spells = pool.cardPool.take(36)
                    val deck = mutableMapOf<String, Int>()
                    spells.forEach { card ->
                        deck[card.name] = (deck[card.name] ?: 0) + 1
                    }
                    // Add 24 basic lands
                    deck["Forest"] = 24

                    player.client.send(ClientMessage.SubmitSealedDeck(deck))
                }

                // Wait for tournament to start (all decks submitted)
                eventually(10.seconds) {
                    players.any { p ->
                        p.client.messages.any { it is ServerMessage.TournamentStarted }
                    } shouldBe true
                }

                // =========================================================================
                // Stage 4: TOURNAMENT_ACTIVE - Test reconnection during matches
                // =========================================================================

                // Wait for matches to be set up
                delay(2000)

                // Find a player who got a TournamentMatchStarting message (has a match, not a bye)
                var matchPlayer: PlayerTestContext? = null
                var gameSessionId: String? = null

                for (player in players) {
                    val matchStarting = player.client.messages
                        .filterIsInstance<ServerMessage.TournamentMatchStarting>()
                        .firstOrNull()
                    if (matchStarting != null) {
                        matchPlayer = player
                        gameSessionId = matchStarting.gameSessionId
                        break
                    }
                }

                // Only test game reconnection if a match is active
                if (matchPlayer != null && gameSessionId != null) {
                    // Wait for game to start (mulligan phase)
                    eventually(5.seconds) {
                        matchPlayer!!.client.messages.any {
                            it is ServerMessage.GameStarted || it is ServerMessage.MulliganDecision
                        } shouldBe true
                    }

                    // Test reconnection during game
                    val originalClient = matchPlayer.client
                    originalClient.close()
                    activeClients.remove(originalClient)

                    val newMatchClient = createClient()
                    val reconnectedMatch = newMatchClient.reconnectAs(matchPlayer.name, matchPlayer.token)

                    // Should reconnect to game or tournament context
                    (reconnectedMatch.context == "game" || reconnectedMatch.context == "tournament") shouldBe true

                    // Update player reference
                    val playerIndex = players.indexOfFirst { it.playerId == matchPlayer.playerId }
                    players[playerIndex] = matchPlayer.copy(client = newMatchClient)
                }

                // =========================================================================
                // Verify lobby/tournament state is preserved for player with bye
                // =========================================================================

                // Find a player who got a bye (with 5 players, one will have a bye)
                val byePlayer = players.find { p ->
                    p.client.messages.any { it is ServerMessage.TournamentBye }
                }

                if (byePlayer != null) {
                    byePlayer.client.close()
                    activeClients.remove(byePlayer.client)

                    val newByeClient = createClient()
                    val reconnectedBye = newByeClient.reconnectAs(byePlayer.name, byePlayer.token)

                    // Should reconnect to tournament context
                    reconnectedBye.context shouldBe "tournament"
                    reconnectedBye.contextId shouldBe lobbyId

                    val playerIndex = players.indexOfFirst { it.playerId == byePlayer.playerId }
                    players[playerIndex] = byePlayer.copy(client = newByeClient)
                }
            }

            test("reconnection preserves lobby settings during WAITING_FOR_PLAYERS") {
                val client1 = createClient()
                val connected1 = client1.connectAs("Host")

                client1.send(ClientMessage.CreateTournamentLobby(
                    setCodes = listOf("POR"),
                    format = "SEALED",
                    boosterCount = 4, // Custom setting
                    maxPlayers = 6
                ))

                eventually(5.seconds) {
                    client1.latestLobbyUpdate() shouldNotBe null
                }

                val lobbyUpdate = client1.latestLobbyUpdate()!!
                lobbyUpdate.settings.boosterCount shouldBe 4
                lobbyUpdate.settings.maxPlayers shouldBe 6

                // Reconnect
                client1.close()
                activeClients.remove(client1)

                val newClient1 = createClient()
                val reconnected = newClient1.reconnectAs("Host", connected1.token)
                reconnected.context shouldBe "lobby"

                eventually(5.seconds) {
                    newClient1.latestLobbyUpdate() shouldNotBe null
                }

                // Settings should be preserved
                val lobbyAfter = newClient1.latestLobbyUpdate()!!
                lobbyAfter.settings.boosterCount shouldBe 4
                lobbyAfter.settings.maxPlayers shouldBe 6
            }

            test("multiple simultaneous reconnections are handled correctly") {
                val playerNames = listOf("P1", "P2", "P3")
                val players = mutableListOf<PlayerTestContext>()

                // Create lobby with 3 players
                val hostClient = createClient()
                val hostConnected = hostClient.connectAs(playerNames[0])
                hostClient.send(ClientMessage.CreateTournamentLobby(setCodes = listOf("POR"), format = "SEALED"))

                eventually(5.seconds) {
                    hostClient.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
                }

                val lobbyId = hostClient.messages
                    .filterIsInstance<ServerMessage.LobbyCreated>()
                    .first().lobbyId

                players.add(PlayerTestContext(playerNames[0], EntityId.of(hostConnected.playerId), hostConnected.token, hostClient, true))

                for (i in 1 until 3) {
                    val client = createClient()
                    val connected = client.connectAs(playerNames[i])
                    client.send(ClientMessage.JoinLobby(lobbyId))
                    eventually(5.seconds) {
                        client.latestLobbyUpdate() shouldNotBe null
                    }
                    players.add(PlayerTestContext(playerNames[i], EntityId.of(connected.playerId), connected.token, client, false))
                }

                // Close all connections simultaneously
                players.forEach { p ->
                    p.client.close()
                    activeClients.remove(p.client)
                }

                // Small delay
                delay(500)

                // Reconnect all simultaneously
                val newClients = players.map { p ->
                    val newClient = createClient()
                    val reconnected = newClient.reconnectAs(p.name, p.token)
                    reconnected.context shouldBe "lobby"
                    newClient
                }

                // All should see 3 players
                eventually(5.seconds) {
                    newClients.all { c ->
                        c.latestLobbyUpdate()?.players?.size == 3
                    } shouldBe true
                }
            }

            test("reconnection during deck building restores submitted deck status") {
                val playerNames = listOf("P1", "P2")
                val players = mutableListOf<PlayerTestContext>()

                // Create and start a 2-player lobby
                val hostClient = createClient()
                val hostConnected = hostClient.connectAs(playerNames[0])
                hostClient.send(ClientMessage.CreateTournamentLobby(setCodes = listOf("POR"), format = "SEALED", boosterCount = 6, maxPlayers = 2))

                eventually(5.seconds) {
                    hostClient.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
                }

                val lobbyId = hostClient.messages
                    .filterIsInstance<ServerMessage.LobbyCreated>()
                    .first().lobbyId

                players.add(PlayerTestContext(playerNames[0], EntityId.of(hostConnected.playerId), hostConnected.token, hostClient, true))

                val client2 = createClient()
                val connected2 = client2.connectAs(playerNames[1])
                client2.send(ClientMessage.JoinLobby(lobbyId))
                eventually(5.seconds) {
                    client2.latestLobbyUpdate() shouldNotBe null
                }
                players.add(PlayerTestContext(playerNames[1], EntityId.of(connected2.playerId), connected2.token, client2, false))

                // Start tournament
                hostClient.send(ClientMessage.StartTournamentLobby)

                eventually(10.seconds) {
                    players.all { p ->
                        p.client.messages.any { it is ServerMessage.SealedPoolGenerated }
                    } shouldBe true
                }

                // Player 1 submits deck
                val pool1 = hostClient.messages
                    .filterIsInstance<ServerMessage.SealedPoolGenerated>()
                    .first()
                val deck1 = mutableMapOf<String, Int>()
                pool1.cardPool.take(36).forEach { card ->
                    deck1[card.name] = (deck1[card.name] ?: 0) + 1
                }
                deck1["Forest"] = 24
                hostClient.send(ClientMessage.SubmitSealedDeck(deck1))

                // Wait for deck submission to be processed
                eventually(5.seconds) {
                    val lobbyUpdate = hostClient.latestLobbyUpdate()
                    lobbyUpdate?.players?.find { it.playerId == hostConnected.playerId }?.deckSubmitted shouldBe true
                }

                // Reconnect player 1
                hostClient.close()
                activeClients.remove(hostClient)

                val newHostClient = createClient()
                val reconnected = newHostClient.reconnectAs(playerNames[0], hostConnected.token)
                reconnected.context shouldBe "deckBuilding"

                // Verify deck submission status is preserved
                eventually(5.seconds) {
                    val lobbyUpdate = newHostClient.latestLobbyUpdate()
                    lobbyUpdate?.players?.find { it.playerId == hostConnected.playerId }?.deckSubmitted shouldBe true
                }
            }

            test("multi-round tournament with standings updates and round advancement") {
                // 3 players = 3 rounds in round-robin, 1 bye per round
                val playerNames = listOf("Alice", "Bob", "Charlie")
                val players = mutableListOf<PlayerTestContext>()

                // =========================================================================
                // Setup: Create lobby and join all players
                // =========================================================================

                val hostClient = createClient()
                val hostConnected = hostClient.connectAs(playerNames[0])
                hostClient.send(ClientMessage.CreateTournamentLobby(
                    setCodes = listOf("POR"),
                    format = "SEALED",
                    boosterCount = 6,
                    maxPlayers = 4
                ))

                eventually(5.seconds) {
                    hostClient.messages.any { it is ServerMessage.LobbyCreated } shouldBe true
                }

                val lobbyId = hostClient.messages
                    .filterIsInstance<ServerMessage.LobbyCreated>()
                    .first().lobbyId

                players.add(PlayerTestContext(
                    name = playerNames[0],
                    playerId = EntityId.of(hostConnected.playerId),
                    token = hostConnected.token,
                    client = hostClient,
                    isHost = true
                ))

                // Other players join
                for (i in 1 until 3) {
                    val client = createClient()
                    val connected = client.connectAs(playerNames[i])
                    client.send(ClientMessage.JoinLobby(lobbyId))
                    eventually(5.seconds) {
                        client.latestLobbyUpdate() shouldNotBe null
                    }
                    players.add(PlayerTestContext(
                        name = playerNames[i],
                        playerId = EntityId.of(connected.playerId),
                        token = connected.token,
                        client = client,
                        isHost = false
                    ))
                }

                // Verify all players see 3 players in lobby
                eventually(5.seconds) {
                    hostClient.latestLobbyUpdate()?.players?.size shouldBe 3
                }

                // =========================================================================
                // Start tournament and submit decks
                // =========================================================================

                hostClient.send(ClientMessage.StartTournamentLobby)

                eventually(10.seconds) {
                    players.all { p ->
                        p.client.messages.any { it is ServerMessage.SealedPoolGenerated }
                    } shouldBe true
                }

                // Submit decks for all players
                for (player in players) {
                    val pool = player.client.messages
                        .filterIsInstance<ServerMessage.SealedPoolGenerated>()
                        .first()
                    val deck = mutableMapOf<String, Int>()
                    pool.cardPool.take(36).forEach { card ->
                        deck[card.name] = (deck[card.name] ?: 0) + 1
                    }
                    deck["Forest"] = 24
                    player.client.send(ClientMessage.SubmitSealedDeck(deck))
                }

                // Wait for tournament to start
                eventually(10.seconds) {
                    players.any { p ->
                        p.client.messages.any { it is ServerMessage.TournamentStarted }
                    } shouldBe true
                }

                // Verify initial standings (all players at 0 points)
                val initialStandings = players[0].client.messages
                    .filterIsInstance<ServerMessage.TournamentStarted>()
                    .first().standings

                initialStandings.size shouldBe 3
                initialStandings.all { it.wins == 0 && it.losses == 0 && it.points == 0 } shouldBe true

                // =========================================================================
                // Round 1: Ready up to start the first round
                // =========================================================================

                // All players must signal ready to start round 1
                players.forEach { p -> p.client.send(ClientMessage.ReadyForNextRound) }

                // Wait for all players to receive either TournamentMatchStarting or TournamentBye
                eventually(10.seconds) {
                    players.all { p ->
                        p.client.messages.any {
                            it is ServerMessage.TournamentMatchStarting || it is ServerMessage.TournamentBye
                        }
                    } shouldBe true
                }

                // Find player with bye and players in match
                var byePlayer: PlayerTestContext? = null
                val matchPlayers = mutableListOf<PlayerTestContext>()

                for (player in players) {
                    val hasBye = player.client.messages.any { it is ServerMessage.TournamentBye }
                    val hasMatch = player.client.messages.any { it is ServerMessage.TournamentMatchStarting }
                    if (hasBye) {
                        byePlayer = player
                    } else if (hasMatch) {
                        matchPlayers.add(player)
                    }
                }

                // With 3 players, one should have a bye
                byePlayer shouldNotBe null
                matchPlayers.size shouldBe 2

                // Wait for both match players to be in mulligan phase
                eventually(5.seconds) {
                    matchPlayers.all { p ->
                        p.client.messages.any { it is ServerMessage.MulliganDecision }
                    } shouldBe true
                }

                // Have both players keep their hands
                matchPlayers.forEach { p -> p.client.send(ClientMessage.KeepHand) }

                eventually(5.seconds) {
                    matchPlayers.all { p ->
                        p.client.messages.any { it is ServerMessage.StateUpdate }
                    } shouldBe true
                }

                // Designate winner and loser for round 1
                val round1Winner = matchPlayers[0]
                val round1Loser = matchPlayers[1]

                // Loser concedes to end the game
                round1Loser.client.send(ClientMessage.Concede)

                // Wait for GameOver and RoundComplete
                eventually(10.seconds) {
                    matchPlayers.all { p ->
                        p.client.messages.any { it is ServerMessage.GameOver }
                    } shouldBe true
                }

                eventually(10.seconds) {
                    players.all { p ->
                        p.client.messages.any { it is ServerMessage.RoundComplete }
                    } shouldBe true
                }

                // Verify standings after round 1
                val round1Complete = players[0].client.messages
                    .filterIsInstance<ServerMessage.RoundComplete>()
                    .first { it.round == 1 }

                round1Complete.round shouldBe 1
                round1Complete.isTournamentComplete shouldBe false

                // Winner should have 3 points (1 win), loser should have 0 points (1 loss)
                val winnerStanding = round1Complete.standings.find { it.playerId == round1Winner.playerId.value }
                val loserStanding = round1Complete.standings.find { it.playerId == round1Loser.playerId.value }
                val byeStanding = round1Complete.standings.find { it.playerId == byePlayer!!.playerId.value }

                winnerStanding?.wins shouldBe 1
                winnerStanding?.losses shouldBe 0
                winnerStanding?.points shouldBe 3

                loserStanding?.wins shouldBe 0
                loserStanding?.losses shouldBe 1
                loserStanding?.points shouldBe 0

                // Bye player should have no wins/losses (byes don't award points)
                byeStanding?.wins shouldBe 0
                byeStanding?.losses shouldBe 0
                byeStanding?.points shouldBe 0

                // =========================================================================
                // Round 2: All players ready up, play next match
                // =========================================================================

                // Small delay to let server finish processing round completion
                delay(500)

                // Track message counts before round 2
                val round1MulliganCounts = players.associate { p ->
                    p.playerId to p.client.messages.filterIsInstance<ServerMessage.MulliganDecision>().size
                }

                // All players signal ready for next round (stagger slightly)
                players.forEach { p ->
                    p.client.send(ClientMessage.ReadyForNextRound)
                    delay(100)
                }

                // Wait for round 2 to start - all players should get either match or bye message
                eventually(10.seconds) {
                    players.all { p ->
                        p.client.messages.filterIsInstance<ServerMessage.TournamentMatchStarting>()
                            .any { m -> m.round == 2 } ||
                        p.client.messages.filterIsInstance<ServerMessage.TournamentBye>()
                            .any { b -> b.round == 2 }
                    } shouldBe true
                }

                // Find new matchups for round 2
                val round2ByePlayer = players.find { p ->
                    p.client.messages.filterIsInstance<ServerMessage.TournamentBye>()
                        .any { it.round == 2 }
                }
                val round2MatchPlayers = players.filter { p ->
                    p.client.messages.filterIsInstance<ServerMessage.TournamentMatchStarting>()
                        .any { it.round == 2 }
                }

                round2MatchPlayers.size shouldBe 2

                // Wait for NEW mulligan decisions (count should increase from round 1)
                eventually(10.seconds) {
                    round2MatchPlayers.all { p ->
                        val currentCount = p.client.messages.filterIsInstance<ServerMessage.MulliganDecision>().size
                        val previousCount = round1MulliganCounts[p.playerId] ?: 0
                        currentCount > previousCount
                    } shouldBe true
                }

                // Keep hands for round 2
                round2MatchPlayers.forEach { p -> p.client.send(ClientMessage.KeepHand) }

                // Track state update counts before waiting
                val round2StateUpdateCounts = round2MatchPlayers.associate { p ->
                    p.playerId to p.client.messages.filterIsInstance<ServerMessage.StateUpdate>().size
                }

                eventually(10.seconds) {
                    round2MatchPlayers.all { p ->
                        val currentCount = p.client.messages.filterIsInstance<ServerMessage.StateUpdate>().size
                        val previousCount = round2StateUpdateCounts[p.playerId] ?: 0
                        currentCount > previousCount
                    } shouldBe true
                }

                // =========================================================================
                // Test: Page refresh during active game preserves state
                // =========================================================================

                // Get the current game state before refresh
                val playerToRefresh = round2MatchPlayers[0]
                val stateBeforeRefresh = playerToRefresh.client.messages
                    .filterIsInstance<ServerMessage.StateUpdate>()
                    .last().state
                val lifeBeforeRefresh = stateBeforeRefresh.players.find {
                    it.playerId == playerToRefresh.playerId
                }?.life

                // Close connection (simulating page refresh)
                playerToRefresh.client.close()
                activeClients.remove(playerToRefresh.client)

                // Small delay to simulate refresh
                delay(500)

                // Reconnect
                val newClient = createClient()
                val reconnected = newClient.reconnectAs(playerToRefresh.name, playerToRefresh.token)

                // Should reconnect to tournament context (or game context)
                // During a tournament, the server may report "tournament" as the context
                (reconnected.context == "game" || reconnected.context == "tournament") shouldBe true

                // Wait for state update on reconnection (game state should be sent)
                eventually(10.seconds) {
                    newClient.messages.any { it is ServerMessage.StateUpdate } shouldBe true
                }

                // Verify game state is preserved
                val stateAfterRefresh = newClient.messages
                    .filterIsInstance<ServerMessage.StateUpdate>()
                    .last().state
                val lifeAfterRefresh = stateAfterRefresh.players.find {
                    it.playerId == playerToRefresh.playerId
                }?.life

                lifeAfterRefresh shouldBe lifeBeforeRefresh

                // Update player reference with new client
                val refreshedPlayerIndex = players.indexOfFirst { it.playerId == playerToRefresh.playerId }
                players[refreshedPlayerIndex] = players[refreshedPlayerIndex].copy(client = newClient)

                // Update round2MatchPlayers reference too
                val matchPlayerIndex = round2MatchPlayers.indexOfFirst { it.playerId == playerToRefresh.playerId }

                // Designate winner and loser for round 2
                val round2Winner = if (matchPlayerIndex == 0) {
                    players[refreshedPlayerIndex]
                } else {
                    round2MatchPlayers[0]
                }
                val round2Loser = if (matchPlayerIndex == 0) {
                    round2MatchPlayers[1]
                } else {
                    round2MatchPlayers[1]
                }

                // Loser concedes
                round2Loser.client.send(ClientMessage.Concede)

                // Wait for RoundComplete for round 2
                eventually(10.seconds) {
                    players.any { p ->
                        p.client.messages.filterIsInstance<ServerMessage.RoundComplete>()
                            .any { it.round == 2 }
                    } shouldBe true
                }

                val round2Complete = players[0].client.messages
                    .filterIsInstance<ServerMessage.RoundComplete>()
                    .first { it.round == 2 }

                round2Complete.round shouldBe 2
                round2Complete.isTournamentComplete shouldBe false

                // =========================================================================
                // Round 3: Final round
                // =========================================================================

                // Small delay to let server finish processing round completion
                delay(500)

                // Track message counts before round 3
                val round2MulliganCounts = players.associate { p ->
                    p.playerId to p.client.messages.filterIsInstance<ServerMessage.MulliganDecision>().size
                }

                // All players signal ready for next round (stagger slightly)
                players.forEach { p ->
                    p.client.send(ClientMessage.ReadyForNextRound)
                    delay(100)
                }

                // Wait for round 3 to start - all players should get either match or bye message
                eventually(10.seconds) {
                    players.all { p ->
                        p.client.messages.filterIsInstance<ServerMessage.TournamentMatchStarting>()
                            .any { it.round == 3 } ||
                        p.client.messages.filterIsInstance<ServerMessage.TournamentBye>()
                            .any { it.round == 3 }
                    } shouldBe true
                }

                val round3MatchPlayers = players.filter { p ->
                    p.client.messages.filterIsInstance<ServerMessage.TournamentMatchStarting>()
                        .any { it.round == 3 }
                }

                round3MatchPlayers.size shouldBe 2

                // Wait for NEW mulligan decisions
                eventually(10.seconds) {
                    round3MatchPlayers.all { p ->
                        val currentCount = p.client.messages.filterIsInstance<ServerMessage.MulliganDecision>().size
                        val previousCount = round2MulliganCounts[p.playerId] ?: 0
                        currentCount > previousCount
                    } shouldBe true
                }

                // Keep hands for round 3
                round3MatchPlayers.forEach { p -> p.client.send(ClientMessage.KeepHand) }

                // Track state update counts before waiting
                val round3StateUpdateCounts = round3MatchPlayers.associate { p ->
                    p.playerId to p.client.messages.filterIsInstance<ServerMessage.StateUpdate>().size
                }

                eventually(10.seconds) {
                    round3MatchPlayers.all { p ->
                        val currentCount = p.client.messages.filterIsInstance<ServerMessage.StateUpdate>().size
                        val previousCount = round3StateUpdateCounts[p.playerId] ?: 0
                        currentCount > previousCount
                    } shouldBe true
                }

                // One player concedes
                round3MatchPlayers[1].client.send(ClientMessage.Concede)

                // Wait for tournament complete
                eventually(10.seconds) {
                    players.any { p ->
                        p.client.messages.any { it is ServerMessage.TournamentComplete } ||
                        p.client.messages.filterIsInstance<ServerMessage.RoundComplete>()
                            .any { it.isTournamentComplete }
                    } shouldBe true
                }

                // Verify final standings
                val finalRoundComplete = players[0].client.messages
                    .filterIsInstance<ServerMessage.RoundComplete>()
                    .lastOrNull { it.isTournamentComplete }

                val tournamentComplete = players[0].client.messages
                    .filterIsInstance<ServerMessage.TournamentComplete>()
                    .firstOrNull()

                val finalStandings = finalRoundComplete?.standings
                    ?: tournamentComplete?.finalStandings

                finalStandings shouldNotBe null
                finalStandings!!.size shouldBe 3

                // All players should have played 2 games (in a 3-player round-robin, each plays 2 games)
                // One bye per player across all rounds
                finalStandings.forEach { standing ->
                    (standing.wins + standing.losses) shouldBe 2
                }

                // Standings should be sorted by points (descending)
                for (i in 0 until finalStandings.size - 1) {
                    (finalStandings[i].points >= finalStandings[i + 1].points) shouldBe true
                }
            }
        }
    }

    // =========================================================================
    // Test Helper Classes
    // =========================================================================

    data class PlayerTestContext(
        val name: String,
        val playerId: EntityId,
        val token: String,
        val client: TournamentTestClient,
        val isHost: Boolean
    )

    data class ConnectResult(
        val playerId: String,
        val token: String
    )

    data class ReconnectResult(
        val playerId: String,
        val token: String,
        val context: String?,
        val contextId: String?
    )

    inner class TournamentTestClient(
        private val json: Json,
        private val container: jakarta.websocket.WebSocketContainer,
        private val url: String
    ) {
        private var session: WebSocketSession? = null
        val messages = CopyOnWriteArrayList<ServerMessage>()
        private val connectLatch = CountDownLatch(1)
        private val closed = AtomicBoolean(false)
        private val stateUpdateCounter = AtomicInteger(0)

        val isOpen: Boolean get() = session?.isOpen == true && !closed.get()

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
                                val serverMessage = json.decodeFromString<ServerMessage>(message.payload)
                                messages.add(serverMessage)
                                if (serverMessage is ServerMessage.StateUpdate) {
                                    stateUpdateCounter.incrementAndGet()
                                }
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

        suspend fun reconnectAs(playerName: String, token: String): ReconnectResult {
            connect()
            send(ClientMessage.Connect(playerName, token))
            eventually(5.seconds) {
                messages.any { it is ServerMessage.Connected || it is ServerMessage.Reconnected } shouldBe true
            }

            val reconnected = messages.filterIsInstance<ServerMessage.Reconnected>().firstOrNull()
            if (reconnected != null) {
                return ReconnectResult(reconnected.playerId, reconnected.token, reconnected.context, reconnected.contextId)
            }

            val connected = messages.filterIsInstance<ServerMessage.Connected>().first()
            return ReconnectResult(connected.playerId, connected.token, null, null)
        }

        fun latestLobbyUpdate(): ServerMessage.LobbyUpdate? =
            messages.filterIsInstance<ServerMessage.LobbyUpdate>().lastOrNull()

        fun latestState(): com.wingedsheep.gameserver.dto.ClientGameState? =
            messages.filterIsInstance<ServerMessage.StateUpdate>().lastOrNull()?.state
    }
}
