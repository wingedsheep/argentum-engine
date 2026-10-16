package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.ServerMessage
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * A spectator watching a game to its end sees how it ended: the server sends them the same public
 * [ServerMessage.GameOver] the players get (winner, reason, message), and leaves the board up
 * instead of ejecting them with [ServerMessage.SpectatingStopped]. The spectator leaves when they
 * (or their client's auto-return) send [ClientMessage.StopSpectating].
 */
class SpectatorGameOverTest : GameServerTestBase() {

    init {
        test("a spectator receives the game-over result and stays on the final board") {
            val ctx = setupGame()
            val spectator = createClient()
            spectator.connectAs("Carol")

            spectator.send(ClientMessage.SpectateGame(ctx.sessionId))
            eventually(5.seconds) {
                spectator.messages.any { it is ServerMessage.SpectatorStateUpdate } shouldBe true
            }

            ctx.player1.client.send(ClientMessage.Concede)

            eventually(5.seconds) {
                spectator.messages.any { it is ServerMessage.GameOver } shouldBe true
            }
            val gameOver = spectator.messages.filterIsInstance<ServerMessage.GameOver>().single()
            gameOver.gameId shouldBe ctx.sessionId
            gameOver.winnerId shouldBe ctx.player2.id
            gameOver.winnerIds shouldContainExactly listOf(ctx.player2.id)
            gameOver.reason shouldBe GameOverReason.CONCESSION
            // Not ejected: the client holds the final board until the spectator leaves.
            spectator.messages.filterIsInstance<ServerMessage.SpectatingStopped>().shouldBeEmpty()

            // Leaving afterwards still works and confirms the stop.
            spectator.send(ClientMessage.StopSpectating)
            eventually(5.seconds) {
                spectator.messages.any { it is ServerMessage.SpectatingStopped } shouldBe true
            }
        }
    }
}
