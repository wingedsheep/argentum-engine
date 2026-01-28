package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

class GameConnectionTest : GameServerTestBase() {
    init {
        context("Connection and Game Setup") {
            test("player connects and receives player ID") {
                val client = createClient()
                val playerId = client.connectAs("TestPlayer")
                playerId shouldNotBe null
                playerId.isNotEmpty() shouldBe true

                val connected = client.messages.filterIsInstance<ServerMessage.Connected>().first()
                connected.playerId shouldBe playerId
            }

            test("two players can create and join a game") {
                val ctx = setupGame(skipMulligan = false)
                ctx.sessionId shouldNotBe null
                ctx.player1.id shouldNotBe ctx.player2.id

                val started1 = ctx.player1.client.messages.filterIsInstance<ServerMessage.GameStarted>().first()
                val started2 = ctx.player2.client.messages.filterIsInstance<ServerMessage.GameStarted>().first()

                started1.opponentName shouldBe ctx.player2.name
                started2.opponentName shouldBe ctx.player1.name
            }

            test("joining non-existent game returns GAME_NOT_FOUND error") {
                val client = createClient()
                client.connectAs("Alice")
                client.send(ClientMessage.JoinGame("non-existent-session", monoGreenLands))

                eventually(5.seconds) {
                    client.allErrors().isNotEmpty() shouldBe true
                }
                client.latestError()?.code shouldBe ErrorCode.GAME_NOT_FOUND
            }

            test("creating game with empty deck generates random deck") {
                val client = createClient()
                client.connectAs("Alice")
                client.send(ClientMessage.CreateGame(emptyMap()))

                eventually(5.seconds) {
                    client.messages.any { it is ServerMessage.GameCreated } shouldBe true
                }
                val gameCreated = client.messages.filterIsInstance<ServerMessage.GameCreated>().first()
                gameCreated.sessionId.isNotEmpty() shouldBe true
            }

            test("connecting twice returns ALREADY_CONNECTED error") {
                val client = createClient()
                client.connectAs("Alice")
                client.send(ClientMessage.Connect("Alice2"))

                eventually(5.seconds) {
                    client.allErrors().isNotEmpty() shouldBe true
                }
                client.latestError()?.code shouldBe ErrorCode.ALREADY_CONNECTED
            }
        }

        context("Concurrent Access") {
            test("multiple games can run on the same server") {
                val ctx1 = setupGame(player1Name = "Alice1", player2Name = "Bob1")
                val ctx2 = setupGame(player1Name = "Alice2", player2Name = "Bob2")

                ctx1.sessionId shouldNotBe ctx2.sessionId

                val game1State = ctx1.player1.client.requireLatestState()
                val game2State = ctx2.player1.client.requireLatestState()

                game1State.player(ctx1.player2.id).life shouldBe 20
                game2State.player(ctx2.player2.id).life shouldBe 20

                game1State.viewingPlayerId shouldNotBe game2State.viewingPlayerId
            }
        }
    }
}
