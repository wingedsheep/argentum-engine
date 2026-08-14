package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * A seat's decklist has to survive everything that can happen to its *socket* during a game, because
 * match history reads it at game over — a lost decklist shows up in a player's profile as a game whose
 * deck was never recorded. This bit multiplayer hardest: a Free-for-All pod runs long enough that
 * several seats reconnect at least once, and the reconnect path used to un-seat the player (dropping
 * their deck and sideboard) before re-seating them, leaving history with only the decks of the players
 * who happened never to reconnect.
 */
class SeatDeckRetentionTest : ScenarioTestBase() {

    private fun mockWs(id: String): WebSocketSession =
        mockk(relaxed = true) { every { this@mockk.id } returns id }

    /** A started [count]-seat session; seat i plays i+30 Forests so the decks are distinguishable. */
    private fun startedSession(count: Int): Pair<GameSession, List<EntityId>> {
        val session = GameSession(cardRegistry = cardRegistry, maxPlayers = count)
        val ids = (1..count).map { EntityId.of("player-$it") }
        ids.forEachIndexed { i, id ->
            session.addPlayer(
                PlayerSession(mockWs("ws$i"), id, "Player${i + 1}"),
                mapOf("Forest" to 30 + i),
                sideboard = mapOf("Mountain" to 2),
            )
        }
        session.startGame()
        return session to ids
    }

    init {
        test("reseating a reconnecting player keeps their deck and sideboard") {
            val (session, ids) = startedSession(4)
            val reconnecting = ids[1]

            session.associatePlayer(PlayerSession(mockWs("ws-new"), reconnecting, "Player2"))

            session.getPlayers() shouldHaveSize 4
            session.getPlayerSession(reconnecting)!!.webSocketSession.id shouldBe "ws-new"
            session.getDeckList(reconnecting).shouldNotBeNull() shouldHaveSize 31
            session.getSideboardsForPersistence()[reconnecting].shouldNotBeNull() shouldHaveSize 2
        }

        test("every seat still reports its starting deck after the whole pod reconnects") {
            val (session, ids) = startedSession(4)

            ids.forEachIndexed { i, id ->
                session.associatePlayer(PlayerSession(mockWs("ws-new$i"), id, "Player${i + 1}"))
            }

            ids.forEachIndexed { i, id ->
                session.getStartingDeckList(id).shouldNotBeNull() shouldHaveSize 30 + i
            }
        }

        test("a seat un-seated mid-game keeps its deck; before the game starts it hands it back") {
            val (started, startedIds) = startedSession(3)
            started.removePlayer(startedIds[2])
            started.getStartingDeckList(startedIds[2]).shouldNotBeNull() shouldHaveSize 32

            val pregame = GameSession(cardRegistry = cardRegistry, maxPlayers = 3)
            val leaver = EntityId.of("player-x")
            pregame.addPlayer(PlayerSession(mockWs("wsx"), leaver, "Leaver"), mapOf("Forest" to 40))
            pregame.removePlayer(leaver)
            pregame.getDeckList(leaver) shouldBe null
        }
    }
}
