package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.engine.core.PassPriority
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

class GameMulliganTest : GameServerTestBase() {
    init {
        context("Mulligan") {
            test("players receive mulligan decision after game starts") {
                val ctx = setupGame(monoGreenLands, skipMulligan = false)
                withClue("Player 1 should receive MulliganDecision") {
                    val decision1 = ctx.player1.client.latestMulliganDecision()
                    decision1 shouldNotBe null
                    decision1!!.hand shouldHaveSize 7
                    decision1.mulliganCount shouldBe 0
                }
            }

            test("keeping opening hand with no mulligans completes immediately") {
                val ctx = setupGame(monoGreenLands, skipMulligan = false)
                val client = ctx.player1.client
                client.send(ClientMessage.KeepHand)

                eventually(5.seconds) {
                    client.messages.any { it is ServerMessage.MulliganComplete } shouldBe true
                }
                val msg = client.messages.filterIsInstance<ServerMessage.MulliganComplete>().first()
                msg.finalHandSize shouldBe 7
            }

            test("mulligan gives new hand with same size") {
                val ctx = setupGame(monoGreenLands, skipMulligan = false)
                val client = ctx.player1.client
                val originalHand = client.latestMulliganDecision()!!.hand
                val countBefore = client.messages.filterIsInstance<ServerMessage.MulliganDecision>().size

                client.send(ClientMessage.Mulligan)

                eventually(5.seconds) {
                    client.messages.filterIsInstance<ServerMessage.MulliganDecision>().size shouldBeGreaterThan countBefore
                }

                val newDecision = client.latestMulliganDecision()!!
                newDecision.hand shouldHaveSize 7
                newDecision.mulliganCount shouldBe 1
                newDecision.cardsToPutOnBottom shouldBe 1
                (newDecision.hand != originalHand) shouldBe true
            }

            test("keeping after mulligan requires choosing cards to put on bottom") {
                val ctx = setupGame(monoGreenLands, skipMulligan = false)
                val client = ctx.player1.client

                // Mulligan first
                val countBefore = client.messages.filterIsInstance<ServerMessage.MulliganDecision>().size
                client.send(ClientMessage.Mulligan)

                eventually(5.seconds) {
                    client.messages.filterIsInstance<ServerMessage.MulliganDecision>().size shouldBeGreaterThan countBefore
                }

                // Then keep
                client.send(ClientMessage.KeepHand)
                eventually(5.seconds) {
                    client.messages.filterIsInstance<ServerMessage.ChooseBottomCards>().isNotEmpty() shouldBe true
                }

                val chooseMsg = client.messages.filterIsInstance<ServerMessage.ChooseBottomCards>().last()
                chooseMsg.hand shouldHaveSize 7
                chooseMsg.cardsToPutOnBottom shouldBe 1
            }

            test("cannot submit game action during mulligan phase") {
                val ctx = setupGame(monoGreenLands, skipMulligan = false)
                ctx.player1.client.send(ClientMessage.SubmitAction(PassPriority(ctx.player1.id)))

                eventually(5.seconds) {
                    ctx.player1.client.allErrors().isNotEmpty() shouldBe true
                }
                ctx.player1.client.latestError()?.code shouldBe ErrorCode.INVALID_ACTION
            }
        }
    }
}
