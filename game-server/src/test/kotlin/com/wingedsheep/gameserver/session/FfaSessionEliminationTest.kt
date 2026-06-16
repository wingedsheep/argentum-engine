package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * GameSession-level invariants behind Free-for-All standings: the session records the
 * elimination order as players lose, a mid-game concede leaves the game running
 * (CR 800.4a), and the final concede ends it with the survivor as winner.
 */
class FfaSessionEliminationTest : ScenarioTestBase() {

    private fun mockWs(id: String): WebSocketSession =
        mockk(relaxed = true) { every { this@mockk.id } returns id }

    private fun startedSession(count: Int): Pair<GameSession, List<EntityId>> {
        val session = GameSession(cardRegistry = cardRegistry, maxPlayers = count)
        val ids = (1..count).map { EntityId.of("ffa-player-$it") }
        ids.forEachIndexed { i, id ->
            session.addPlayer(PlayerSession(mockWs("ffa-ws$i"), id, "Player${i + 1}"), mapOf("Forest" to 40))
        }
        session.startGame()
        ids.forEach { session.keepHand(it) }
        return session to ids
    }

    init {
        test("mid-game concede continues a 3-player game and is recorded in elimination order") {
            val (session, ids) = startedSession(3)

            session.playerConcedes(ids[2])
            session.isGameOver() shouldBe false
            session.getEliminationOrder() shouldBe listOf(ids[2])

            session.playerConcedes(ids[1])
            session.isGameOver() shouldBe true
            session.getWinnerId() shouldBe ids[0]
            session.getEliminationOrder() shouldBe listOf(ids[2], ids[1])
        }

        test("two-player degenerate case: a single concede ends the game") {
            val (session, ids) = startedSession(2)

            session.playerConcedes(ids[1])
            session.isGameOver() shouldBe true
            session.getWinnerId() shouldBe ids[0]
            session.getEliminationOrder() shouldBe listOf(ids[1])
        }
    }
}
