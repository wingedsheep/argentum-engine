package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * The admin Live overview reads a game through [GameSession.adminSnapshot] and
 * [GameSession.lastActionAt]: who is seated (AI or human, connected or not), where the game stands,
 * and whether anyone has acted lately — the inputs to "would a restart interrupt anyone?".
 */
class GameSessionAdminSnapshotTest : ScenarioTestBase() {

    private fun ws(id: String, open: Boolean = true): WebSocketSession = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { isOpen } returns open
    }

    init {
        test("before the game starts, seats are listed with no life and no activity") {
            val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 2)
            val human = EntityId.of("admin-human")
            val ai = EntityId.of("admin-ai")
            session.addPlayer(PlayerSession(ws("ws-h", open = false), human, "Alice"), mapOf("Forest" to 40))
            session.addPlayer(PlayerSession(ws("ws-ai"), ai, "Bot"), mapOf("Forest" to 40))
            session.setPlayerPersistenceInfo(ai, "Bot", "t-ai", isAi = true)

            val snapshot = session.adminSnapshot()

            snapshot.started shouldBe false
            snapshot.turnNumber.shouldBeNull()
            snapshot.seats shouldContainExactlyInAnyOrder listOf(
                GameSession.AdminSeat(name = "Alice", isAi = false, connected = false, life = null),
                GameSession.AdminSeat(name = "Bot", isAi = true, connected = true, life = null),
            )
            session.lastActionAt.shouldBeNull()
        }

        test("a started game reports life, turn, active player, and advances lastActionAt on each action") {
            val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 2)
            val p1 = EntityId.of("admin-p1")
            val p2 = EntityId.of("admin-p2")
            session.addPlayer(PlayerSession(ws("ws-1"), p1, "Alice"), mapOf("Forest" to 40))
            session.addPlayer(PlayerSession(ws("ws-2"), p2, "Bob"), mapOf("Forest" to 40))
            session.startGame()

            val startedAt = session.lastActionAt.shouldNotBeNull()
            Thread.sleep(5)
            session.keepHand(p1)
            session.lastActionAt.shouldNotBeNull().isAfter(startedAt) shouldBe true

            session.keepHand(p2)
            val snapshot = session.adminSnapshot()
            snapshot.started shouldBe true
            snapshot.gameOver shouldBe false
            snapshot.turnNumber.shouldNotBeNull()
            snapshot.step.shouldNotBeNull()
            snapshot.seats.map { it.life } shouldBe listOf(20, 20)
            (snapshot.activePlayerName in setOf("Alice", "Bob")) shouldBe true
        }
    }
}
