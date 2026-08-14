package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.lobby.AiDeckSpec
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.core.DeckFormat
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class QuickGameLobbyCommanderAiTest : GameServerTestBase() {

    init {
        test("host can fill and reopen a normal 1v1 lobby with an AI opponent") {
            val client = createClient()
            client.connectAs("Flexible Host")
            client.send(ClientMessage.CreateQuickGameLobby())
            eventually(5.seconds) {
                client.messages.filterIsInstance<ServerMessage.QuickGameLobbyState>()
                    .lastOrNull()?.players?.size shouldBe 1
            }

            client.send(ClientMessage.AddQuickGameAi)
            eventually(5.seconds) {
                val state = client.messages.filterIsInstance<ServerMessage.QuickGameLobbyState>().lastOrNull()
                state?.vsAi shouldBe true
                state?.players?.count { it.isAi } shouldBe 1
            }

            client.send(ClientMessage.RemoveQuickGameAi)
            eventually(5.seconds) {
                val state = client.messages.filterIsInstance<ServerMessage.QuickGameLobbyState>().lastOrNull()
                state?.vsAi shouldBe false
                state?.players?.size shouldBe 1
            }
            client.allErrors() shouldBe emptyList()
        }

        test("AI quick lobby starts Commander with a host-supplied commander deck") {
            val client = createClient()
            client.connectAs("Commander Host")
            val library = mapOf("Plains" to 99)
            val commander = "Zetalpa, Primal Dawn"

            client.send(ClientMessage.CreateQuickGameLobby(vsAi = true, format = DeckFormat.COMMANDER))
            eventually(5.seconds) {
                client.messages.any { it is ServerMessage.QuickGameLobbyState } shouldBe true
            }

            client.send(
                ClientMessage.SetQuickGameAiDeck(
                    AiDeckSpec.Fixed(
                        deckList = library,
                        label = "Zetalpa",
                        commander = commander,
                    )
                )
            )
            client.send(
                ClientMessage.SubmitQuickGameLobbyDeck(
                    deckList = library,
                    commander = commander,
                )
            )
            client.send(ClientMessage.SetQuickGameLobbyReady(true))

            eventually(10.seconds) {
                client.messages.any { it is ServerMessage.GameCreated } shouldBe true
            }
            client.allErrors() shouldBe emptyList()
        }

        test("AI quick lobby starts Commander with a generated AI deck") {
            // The Auto spec — the default, and what the lobby seats when the host never picks a
            // deck for the AI. It used to be unstartable under Commander rules: no generated deck
            // named a commander, and the engine refuses to initialise a commander game without one.
            val client = createClient()
            client.connectAs("Auto Commander Host")

            client.send(ClientMessage.CreateQuickGameLobby(vsAi = true, format = DeckFormat.COMMANDER))
            eventually(5.seconds) {
                client.messages.any { it is ServerMessage.QuickGameLobbyState } shouldBe true
            }

            client.send(
                ClientMessage.SubmitQuickGameLobbyDeck(
                    deckList = mapOf("Plains" to 99),
                    commander = "Zetalpa, Primal Dawn",
                )
            )
            client.send(ClientMessage.SetQuickGameLobbyReady(true))

            eventually(30.seconds) {
                client.messages.any { it is ServerMessage.GameCreated } shouldBe true
            }

            // "Start *and complete*": the AI has to keep its hand and take turns, not just be seated.
            // A seat holding a commander-less deck would have failed at game init instead.
            eventually(30.seconds) {
                client.messages.any { it is ServerMessage.MulliganDecision } shouldBe true
            }
            client.send(ClientMessage.KeepHand)
            eventually(30.seconds) {
                client.messages.any { it is ServerMessage.StateUpdate } shouldBe true
            }
            // Routine updates are deltas; resync for a full state to assert the table's shape on.
            client.send(ClientMessage.RequestResync)
            eventually(30.seconds) {
                val state = client.latestState()
                state.shouldNotBeNull()
                state.players shouldHaveSize 2
                // CR 903.7 — 40 life each — and CR 903.6: every seat's commander in its own
                // command zone, the AI's generated one included.
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
            client.allErrors() shouldBe emptyList()
        }

        test("a Random-pool human seat gets a commander too under Commander rules") {
            // An empty deck list is "Random pool". Under Commander rules that now means a generated
            // deck *and* its commander for the human seat as well, rather than a 40-card sealed pool
            // the engine then refuses to seat.
            val client = createClient()
            client.connectAs("Random Commander Host")

            client.send(ClientMessage.CreateQuickGameLobby(vsAi = true, format = DeckFormat.COMMANDER))
            eventually(5.seconds) {
                client.messages.any { it is ServerMessage.QuickGameLobbyState } shouldBe true
            }

            client.send(ClientMessage.SubmitQuickGameLobbyDeck(deckList = emptyMap()))
            client.send(ClientMessage.SetQuickGameLobbyReady(true))

            eventually(30.seconds) {
                client.messages.any { it is ServerMessage.GameCreated } shouldBe true
            }
            client.allErrors() shouldBe emptyList()
        }

        test("commander-shaped AI deck is rejected without a designated commander") {
            val client = createClient()
            client.connectAs("Commander Host")
            client.send(ClientMessage.CreateQuickGameLobby(vsAi = true, format = DeckFormat.COMMANDER))
            eventually(5.seconds) {
                client.messages.any { it is ServerMessage.QuickGameLobbyState } shouldBe true
            }

            client.send(
                ClientMessage.SetQuickGameAiDeck(
                    AiDeckSpec.Fixed(deckList = mapOf("Plains" to 100))
                )
            )

            eventually(5.seconds) {
                client.latestError()?.message?.contains("commander", ignoreCase = true) shouldBe true
            }
        }
    }
}
