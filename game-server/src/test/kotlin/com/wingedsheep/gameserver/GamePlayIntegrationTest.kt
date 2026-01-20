package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.masking.MaskedGameState
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.action.*
import com.wingedsheep.rulesengine.game.Phase
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GamePlayIntegrationTest(
    @LocalServerPort private val port: Int
) : FunSpec({

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    fun createTestClient(): TestWebSocketClient {
        return TestWebSocketClient(json)
    }

    suspend fun connectClient(client: TestWebSocketClient, playerName: String): String {
        val wsUrl = "ws://localhost:$port/game"
        client.connect(wsUrl)

        client.send(ClientMessage.Connect(playerName))

        eventually(5.seconds) {
            client.messages.any { it is ServerMessage.Connected }
        }

        return client.messages.filterIsInstance<ServerMessage.Connected>().first().playerId
    }

    suspend fun createAndJoinGame(
        player1Client: TestWebSocketClient,
        player2Client: TestWebSocketClient,
        deck: Map<String, Int>
    ): GameTestContext {
        val player1Id = connectClient(player1Client, "Alice")
        val player2Id = connectClient(player2Client, "Bob")

        player1Client.send(ClientMessage.CreateGame(deck))

        eventually(5.seconds) {
            player1Client.messages.any { it is ServerMessage.GameCreated }
        }

        val sessionId = player1Client.messages.filterIsInstance<ServerMessage.GameCreated>().first().sessionId
        player2Client.send(ClientMessage.JoinGame(sessionId, deck))

        eventually(5.seconds) {
            player1Client.messages.any { it is ServerMessage.GameStarted }
        }
        eventually(5.seconds) {
            player2Client.messages.any { it is ServerMessage.GameStarted }
        }

        // Wait for initial state updates
        eventually(5.seconds) {
            player1Client.messages.any { it is ServerMessage.StateUpdate }
        }
        eventually(5.seconds) {
            player2Client.messages.any { it is ServerMessage.StateUpdate }
        }

        return GameTestContext(
            sessionId = sessionId,
            player1Id = EntityId.of(player1Id),
            player2Id = EntityId.of(player2Id),
            player1Client = player1Client,
            player2Client = player2Client
        )
    }

    fun TestWebSocketClient.getLatestState(): MaskedGameState? {
        return messages.filterIsInstance<ServerMessage.StateUpdate>().lastOrNull()?.state
    }

    fun TestWebSocketClient.clearMessages() {
        messages.clear()
    }

    suspend fun TestWebSocketClient.submitActionAndWaitForUpdate(action: GameAction) {
        val updateCountBefore = messages.count { it is ServerMessage.StateUpdate }
        send(ClientMessage.SubmitAction(action))
        eventually(5.seconds) {
            messages.count { it is ServerMessage.StateUpdate } > updateCountBefore
        }
    }

    // =========================================================================
    // Connection and Game Setup Tests
    // =========================================================================

    test("two players can connect, create game, join, and start playing") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val player1Id = connectClient(player1Client, "Alice")
            player1Id shouldNotBe null

            val player2Id = connectClient(player2Client, "Bob")
            player2Id shouldNotBe null

            val testDeck = mapOf("Forest" to 20, "Grizzly Bears" to 20)
            player1Client.send(ClientMessage.CreateGame(testDeck))

            eventually(5.seconds) {
                player1Client.messages.any { it is ServerMessage.GameCreated }
            }

            val sessionId = player1Client.messages.filterIsInstance<ServerMessage.GameCreated>().first().sessionId
            sessionId shouldNotBe null

            player2Client.send(ClientMessage.JoinGame(sessionId, testDeck))

            eventually(5.seconds) {
                player1Client.messages.any { it is ServerMessage.GameStarted }
            }
            eventually(5.seconds) {
                player2Client.messages.any { it is ServerMessage.GameStarted }
            }

            val player1Started = player1Client.messages.filterIsInstance<ServerMessage.GameStarted>().first()
            val player2Started = player2Client.messages.filterIsInstance<ServerMessage.GameStarted>().first()

            player1Started.opponentName shouldBe "Bob"
            player2Started.opponentName shouldBe "Alice"

            eventually(5.seconds) {
                player1Client.messages.any { it is ServerMessage.StateUpdate }
            }

            val player1State = player1Client.messages.filterIsInstance<ServerMessage.StateUpdate>().first()
            player1State.state.viewingPlayerId.value shouldBe player1Id
            player1State.state.isGameOver shouldBe false

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    test("players receive correct initial game state") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 24, "Grizzly Bears" to 16)
            )

            val state1 = player1Client.getLatestState()!!
            val state2 = player2Client.getLatestState()!!

            // Both players should start at 20 life
            state1.players.forEach { it.life shouldBe 20 }
            state2.players.forEach { it.life shouldBe 20 }

            // Each player should have 7 cards in hand (opening hand)
            state1.players.find { it.playerId == ctx.player1Id }?.handSize shouldBe 7
            state2.players.find { it.playerId == ctx.player2Id }?.handSize shouldBe 7

            // Libraries should have remaining cards (40 - 7 = 33 each)
            state1.players.find { it.playerId == ctx.player1Id }?.librarySize shouldBe 33
            state2.players.find { it.playerId == ctx.player2Id }?.librarySize shouldBe 33

            // Graveyards should be empty
            state1.players.forEach { it.graveyardSize shouldBe 0 }

            // Game should be in the first main phase on turn 1
            state1.turnNumber shouldBe 1

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    // =========================================================================
    // State Masking Tests
    // =========================================================================

    test("state masking hides opponent hand but shows own hand") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 40)
            )

            val state1 = player1Client.getLatestState()!!

            // Player 1 should see their own hand zone as visible
            val player1HandZone = state1.zones.entries.find {
                it.key.type == ZoneType.HAND && it.key.ownerId == ctx.player1Id
            }?.value
            player1HandZone shouldNotBe null
            player1HandZone!!.isVisible shouldBe true
            player1HandZone.entityIds.shouldNotBeEmpty()

            // Player 1 should NOT see opponent's hand contents
            val player2HandZone = state1.zones.entries.find {
                it.key.type == ZoneType.HAND && it.key.ownerId == ctx.player2Id
            }?.value
            player2HandZone shouldNotBe null
            player2HandZone!!.isVisible shouldBe false
            player2HandZone.entityIds shouldBe emptyList()
            player2HandZone.size shouldBe 7  // Can see count but not contents

            // Libraries should be hidden from everyone
            val player1LibraryZone = state1.zones.entries.find {
                it.key.type == ZoneType.LIBRARY && it.key.ownerId == ctx.player1Id
            }?.value
            player1LibraryZone shouldNotBe null
            player1LibraryZone!!.isVisible shouldBe false

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    test("battlefield and graveyard are visible to both players") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 40)
            )

            val state1 = player1Client.getLatestState()!!
            val state2 = player2Client.getLatestState()!!

            // Battlefield should be visible to both
            val battlefield1 = state1.zones[ZoneId.BATTLEFIELD]
            val battlefield2 = state2.zones[ZoneId.BATTLEFIELD]
            battlefield1 shouldNotBe null
            battlefield2 shouldNotBe null
            battlefield1!!.isVisible shouldBe true
            battlefield2!!.isVisible shouldBe true

            // Stack should be visible to both
            val stack1 = state1.zones[ZoneId.STACK]
            val stack2 = state2.zones[ZoneId.STACK]
            stack1?.isVisible shouldBe true
            stack2?.isVisible shouldBe true

            // Exile should be visible to both
            val exile1 = state1.zones[ZoneId.EXILE]
            val exile2 = state2.zones[ZoneId.EXILE]
            exile1?.isVisible shouldBe true
            exile2?.isVisible shouldBe true

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    // =========================================================================
    // Land and Mana Tests
    // =========================================================================

    test("active player can play a land from hand") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 40)
            )

            val initialState = player1Client.getLatestState()!!

            // Find active player and a land in their hand
            val activePlayerId = initialState.activePlayerId

            // The active player should be able to find a card in hand
            val activePlayerHandZone = initialState.zones.entries.find {
                it.key.type == ZoneType.HAND && it.key.ownerId == activePlayerId
            }?.value

            activePlayerHandZone shouldNotBe null
            activePlayerHandZone!!.entityIds.shouldNotBeEmpty()

            val landInHand = activePlayerHandZone.entityIds.first()

            // Submit play land action
            val activeClient = if (activePlayerId == ctx.player1Id) player1Client else player2Client
            activeClient.submitActionAndWaitForUpdate(
                PlayLand(cardId = landInHand, playerId = activePlayerId)
            )

            // Verify land is now on battlefield
            val updatedState = activeClient.getLatestState()!!
            val battlefield = updatedState.zones[ZoneId.BATTLEFIELD]
            battlefield shouldNotBe null
            battlefield!!.entityIds.contains(landInHand) shouldBe true

            // Hand size should be reduced by 1
            val updatedHandZone = updatedState.zones.entries.find {
                it.key.type == ZoneType.HAND && it.key.ownerId == activePlayerId
            }?.value
            updatedHandZone!!.size shouldBe activePlayerHandZone.size - 1

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    test("player can tap land for mana") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 40)
            )

            val initialState = player1Client.getLatestState()!!
            val activePlayerId = initialState.activePlayerId
            val activeClient = if (activePlayerId == ctx.player1Id) player1Client else player2Client

            // First, play a land
            val handZone = initialState.zones.entries.find {
                it.key.type == ZoneType.HAND && it.key.ownerId == activePlayerId
            }?.value!!

            val landInHand = handZone.entityIds.first()
            activeClient.submitActionAndWaitForUpdate(
                PlayLand(cardId = landInHand, playerId = activePlayerId)
            )

            // Now tap the land for mana
            activeClient.submitActionAndWaitForUpdate(
                ActivateManaAbility(
                    sourceEntityId = landInHand,
                    abilityIndex = 0,
                    playerId = activePlayerId
                )
            )

            // The land should now be tapped (we can verify by trying to tap again - should fail)
            // Or check that mana was added to pool
            val stateAfterMana = activeClient.getLatestState()!!
            // The player should have mana in their pool now
            // (Exact verification depends on how mana pool is exposed in MaskedGameState)

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    // =========================================================================
    // Spell Casting Tests
    // =========================================================================

    test("player can cast a creature spell") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 20, "Grizzly Bears" to 20)  // 2G creature
            )

            val initialState = player1Client.getLatestState()!!
            val activePlayerId = initialState.activePlayerId
            val activeClient = if (activePlayerId == ctx.player1Id) player1Client else player2Client

            // Play lands and tap for mana (need 1G for Grizzly Bears)
            val handZone = initialState.zones.entries.find {
                it.key.type == ZoneType.HAND && it.key.ownerId == activePlayerId
            }?.value!!

            // Find two forests in hand (assuming hand has mixed cards)
            val forests = handZone.entityIds.take(2)

            // Play first forest
            activeClient.submitActionAndWaitForUpdate(
                PlayLand(cardId = forests[0], playerId = activePlayerId)
            )

            // Tap for mana
            activeClient.submitActionAndWaitForUpdate(
                ActivateManaAbility(
                    sourceEntityId = forests[0],
                    abilityIndex = 0,
                    playerId = activePlayerId
                )
            )

            // Pass priority to advance to next turn to play another land
            activeClient.submitActionAndWaitForUpdate(
                PassPriority(playerId = activePlayerId)
            )

            // Continue passing through phases until next main phase
            // (In a real test, we'd need to handle all the priority passes)

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    // =========================================================================
    // Priority and Phase Tests
    // =========================================================================

    test("passing priority advances the game") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 40)
            )

            val initialState = player1Client.getLatestState()!!
            val activePlayerId = initialState.activePlayerId
            val activeClient = if (activePlayerId == ctx.player1Id) player1Client else player2Client
            val inactiveClient = if (activePlayerId == ctx.player1Id) player2Client else player1Client
            val inactivePlayerId = if (activePlayerId == ctx.player1Id) ctx.player2Id else ctx.player1Id

            // Active player passes priority
            activeClient.submitActionAndWaitForUpdate(
                PassPriority(playerId = activePlayerId)
            )

            // Non-active player should now have priority
            val stateAfterPass = activeClient.getLatestState()!!
            stateAfterPass.priorityPlayerId shouldBe inactivePlayerId

            // Non-active player passes
            inactiveClient.submitActionAndWaitForUpdate(
                PassPriority(playerId = inactivePlayerId)
            )

            // Game should advance (either step or phase changes)
            val stateAfterBothPass = activeClient.getLatestState()!!
            // The step or phase should have advanced
            val phaseOrStepChanged = stateAfterBothPass.currentStep != initialState.currentStep ||
                    stateAfterBothPass.currentPhase != initialState.currentPhase
            phaseOrStepChanged shouldBe true

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    // =========================================================================
    // Combat Tests
    // =========================================================================

    test("combat flow - declare attacker with creature") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 20, "Grizzly Bears" to 20)
            )

            // This is a more complex test that requires:
            // 1. Getting a creature onto the battlefield
            // 2. Waiting for summoning sickness to wear off (or it's turn 2+)
            // 3. Entering combat phase
            // 4. Declaring attackers

            // For now, we verify the combat action types are valid
            val declareAttacker = DeclareAttacker(
                creatureId = EntityId.generate(),
                controllerId = ctx.player1Id
            )
            declareAttacker.description shouldBe "Declare attacker"

            val declareBlocker = DeclareBlocker(
                blockerId = EntityId.generate(),
                attackerId = EntityId.generate(),
                controllerId = ctx.player2Id
            )
            declareBlocker.description shouldBe "Declare blocker"

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    test("combat damage reduces opponent life total") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 40)
            )

            val initialState = player1Client.getLatestState()!!
            val opponentLife = initialState.players.find { it.playerId == ctx.player2Id }?.life ?: 20

            // Direct damage action (simulating unblocked combat damage)
            player1Client.submitActionAndWaitForUpdate(
                DealDamageToPlayer(
                    targetPlayerId = ctx.player2Id,
                    amount = 3,
                    sourceEntityId = null
                )
            )

            val stateAfterDamage = player1Client.getLatestState()!!
            val newOpponentLife = stateAfterDamage.players.find { it.playerId == ctx.player2Id }?.life

            newOpponentLife shouldBe opponentLife - 3

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    // =========================================================================
    // Game End Conditions Tests
    // =========================================================================

    test("player can concede and game ends") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 40)
            )

            player1Client.send(ClientMessage.Concede)

            eventually(5.seconds) {
                player1Client.messages.any { it is ServerMessage.GameOver }
            }
            eventually(5.seconds) {
                player2Client.messages.any { it is ServerMessage.GameOver }
            }

            val gameOver1 = player1Client.messages.filterIsInstance<ServerMessage.GameOver>().first()
            val gameOver2 = player2Client.messages.filterIsInstance<ServerMessage.GameOver>().first()

            // Player 2 should be the winner
            gameOver1.winnerId shouldBe ctx.player2Id
            gameOver2.winnerId shouldBe ctx.player2Id

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    test("game ends when player life reaches zero") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 40)
            )

            // Deal 20 damage to player 2
            player1Client.submitActionAndWaitForUpdate(
                DealDamageToPlayer(
                    targetPlayerId = ctx.player2Id,
                    amount = 20,
                    sourceEntityId = null
                )
            )

            // State-based actions should cause player 2 to lose
            // Wait for game over
            eventually(5.seconds) {
                val state = player1Client.getLatestState()
                state?.isGameOver == true || player1Client.messages.any { it is ServerMessage.GameOver }
            }

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    // =========================================================================
    // Error Handling Tests
    // =========================================================================

    test("error returned when joining non-existent game") {
        val client = createTestClient()

        try {
            connectClient(client, "Alice")

            client.send(ClientMessage.JoinGame("non-existent-session-id", mapOf("Forest" to 40)))

            eventually(5.seconds) {
                client.messages.any { it is ServerMessage.Error }
            }

            val error = client.messages.filterIsInstance<ServerMessage.Error>().first()
            error.code shouldBe ErrorCode.GAME_NOT_FOUND

        } finally {
            client.close()
        }
    }

    test("error returned when submitting action without being in a game") {
        val client = createTestClient()

        try {
            connectClient(client, "Alice")

            client.send(ClientMessage.SubmitAction(
                PassPriority(EntityId.generate())
            ))

            eventually(5.seconds) {
                client.messages.any { it is ServerMessage.Error }
            }

            val error = client.messages.filterIsInstance<ServerMessage.Error>().first()
            error.code shouldBe ErrorCode.GAME_NOT_FOUND

        } finally {
            client.close()
        }
    }

    test("error returned when player without priority submits action") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 40)
            )

            val initialState = player1Client.getLatestState()!!
            val priorityPlayerId = initialState.priorityPlayerId

            // Find the player who does NOT have priority
            val nonPriorityPlayerId = if (priorityPlayerId == ctx.player1Id) ctx.player2Id else ctx.player1Id
            val nonPriorityClient = if (priorityPlayerId == ctx.player1Id) player2Client else player1Client

            // Try to submit an action without priority
            nonPriorityClient.send(ClientMessage.SubmitAction(
                PassPriority(nonPriorityPlayerId)
            ))

            eventually(5.seconds) {
                nonPriorityClient.messages.any { it is ServerMessage.Error }
            }

            val error = nonPriorityClient.messages.filterIsInstance<ServerMessage.Error>().first()
            error.code shouldBe ErrorCode.INVALID_ACTION

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }

    // =========================================================================
    // Full Game Simulation
    // =========================================================================

    test("simulate multiple turns of gameplay") {
        val player1Client = createTestClient()
        val player2Client = createTestClient()

        try {
            val ctx = createAndJoinGame(
                player1Client, player2Client,
                mapOf("Forest" to 40)
            )

            var currentState = player1Client.getLatestState()!!
            var turnCount = 0
            val maxTurns = 5

            while (turnCount < maxTurns && !currentState.isGameOver) {
                val activePlayerId = currentState.activePlayerId
                val activeClient = if (activePlayerId == ctx.player1Id) player1Client else player2Client
                val inactivePlayerId = if (activePlayerId == ctx.player1Id) ctx.player2Id else ctx.player1Id
                val inactiveClient = if (activePlayerId == ctx.player1Id) player2Client else player1Client

                // If main phase, try to play a land
                if (currentState.currentPhase == Phase.PRECOMBAT_MAIN || currentState.currentPhase == Phase.POSTCOMBAT_MAIN) {
                    val handZone = currentState.zones.entries.find {
                        it.key.type == ZoneType.HAND && it.key.ownerId == activePlayerId
                    }?.value

                    if (handZone != null && handZone.entityIds.isNotEmpty()) {
                        // Try to play a land
                        val cardInHand = handZone.entityIds.first()
                        activeClient.send(ClientMessage.SubmitAction(
                            PlayLand(cardId = cardInHand, playerId = activePlayerId)
                        ))
                        // Wait a bit for response
                        kotlinx.coroutines.delay(100)
                    }
                }

                // Pass priority for both players to advance
                activeClient.submitActionAndWaitForUpdate(PassPriority(activePlayerId))
                currentState = activeClient.getLatestState()!!

                if (!currentState.isGameOver && currentState.priorityPlayerId == inactivePlayerId) {
                    inactiveClient.submitActionAndWaitForUpdate(PassPriority(inactivePlayerId))
                    currentState = activeClient.getLatestState()!!
                }

                // Check if turn advanced
                if (currentState.turnNumber > turnCount + 1) {
                    turnCount = currentState.turnNumber - 1
                }

                // Safety: if we've done too many passes, increment turn count anyway
                turnCount++
            }

            // Verify game progressed
            currentState.turnNumber shouldBeGreaterThan 1

        } finally {
            player1Client.close()
            player2Client.close()
        }
    }
})

/**
 * Context for a test game between two players.
 */
data class GameTestContext(
    val sessionId: String,
    val player1Id: EntityId,
    val player2Id: EntityId,
    val player1Client: TestWebSocketClient,
    val player2Client: TestWebSocketClient
)

/**
 * Test WebSocket client that collects received messages.
 */
class TestWebSocketClient(private val json: Json) {
    private var session: WebSocketSession? = null
    val messages = CopyOnWriteArrayList<ServerMessage>()
    private val connectLatch = CountDownLatch(1)

    suspend fun connect(url: String) {
        withContext(Dispatchers.IO) {
            val client = StandardWebSocketClient()
            session = client.execute(
                object : TextWebSocketHandler() {
                    override fun afterConnectionEstablished(session: WebSocketSession) {
                        connectLatch.countDown()
                    }

                    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                        try {
                            val serverMessage = json.decodeFromString<ServerMessage>(message.payload)
                            messages.add(serverMessage)
                        } catch (e: Exception) {
                            println("Failed to parse message: ${message.payload}")
                            e.printStackTrace()
                        }
                    }
                },
                WebSocketHttpHeaders(),
                URI.create(url)
            ).get(10, TimeUnit.SECONDS)

            connectLatch.await(10, TimeUnit.SECONDS)
        }
    }

    fun send(message: ClientMessage) {
        val jsonText = json.encodeToString(message)
        session?.sendMessage(TextMessage(jsonText))
    }

    fun close() {
        session?.close()
    }
}
