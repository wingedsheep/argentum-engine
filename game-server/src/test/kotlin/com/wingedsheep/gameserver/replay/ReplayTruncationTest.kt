package com.wingedsheep.gameserver.replay

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.GameStallGuard
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * A recording that had to stop early is still a real replay — it just isn't the whole game.
 *
 * The failure this guards against is not losing the tail; it's a record that *looks* complete.
 * Nothing downstream can tell a game that ended at frame 25,000 from a game whose recording did, so
 * the flag has to survive storage and reach the viewer as words a player can read.
 */
class ReplayTruncationTest : ScenarioTestBase() {

    private fun mockWs(id: String): WebSocketSession =
        mockk(relaxed = true) { every { this@mockk.id } returns id }

    /** A real recorded game whose log was frozen after [cap] actions. */
    private fun truncatedGame(cap: Int): GameSession {
        val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 2)
        session.tightenBackstopsForTesting(GameStallGuard(), cap)
        val p1 = EntityId.of("trunc-p1")
        val p2 = EntityId.of("trunc-p2")
        session.addPlayer(PlayerSession(mockWs("trunc-ws1"), p1, "Alice"), mapOf("Forest" to 40))
        session.addPlayer(PlayerSession(mockWs("trunc-ws2"), p2, "Bob"), mapOf("Forest" to 40))
        session.startGame()
        session.keepHand(p1)
        session.keepHand(p2)
        repeat(30) {
            val state = session.getStateForTesting() ?: return@repeat
            if (state.gameOver) return@repeat
            state.priorityPlayerId?.let { session.executeAutoPass(it) }
        }
        return session
    }

    private fun GameSession.record(truncated: Boolean = isReplayTruncated()): CompactReplay {
        val snapshot = replayRecordingSnapshot().shouldNotBeNull()
        return CompactReplay(
            gameId = sessionId,
            players = getPlayers().map { ReplayPlayerInfo(it.playerId.value, it.playerName) },
            startedAt = "2026-01-01T00:00:00Z",
            endedAt = "2026-01-01T00:30:00Z",
            winnerName = null,
            setup = snapshot.setup,
            actions = snapshot.actions,
            yields = snapshot.yields,
            checkpoints = snapshot.checkpoints,
            truncated = truncated,
        )
    }

    init {
        test("the flag survives the store's gzip+base64 round trip") {
            val record = truncatedGame(cap = 8).record()
            record.truncated shouldBe true

            ReplayCodec.decode(ReplayCodec.encode(record)).truncated shouldBe true
        }

        test("a record written before the flag existed reads as the complete game it was") {
            // Decoding is deliberately tolerant so a rolling deploy can read both ways; the failure
            // direction that matters is a pre-existing complete game being labelled partial. Older
            // records have no such key at all, so drop it to get one.
            val whole = truncatedGame(cap = 10_000).record(truncated = false)
            val legacy = com.wingedsheep.gameserver.persistence.persistenceJson
                .encodeToString(CompactReplay.serializer(), whole)
                .replace("\"truncated\":false,", "")
                .replace(",\"truncated\":false", "")
            legacy shouldNotContain "truncated"

            ReplayCodec.decode(ReplayCodec.encodeText(legacy)).truncated shouldBe false
        }

        test("the viewer is told the recording stops before the game did") {
            val record = truncatedGame(cap = 8).record()
            val store = InMemoryReplayStore().apply {
                save(StoredReplay(record, ReplayStatus.FINISHED))
            }
            val service = ReplayService(
                store,
                ReplayReconstructor(cardRegistry, null),
                mockk(relaxed = true),
            )

            val payload = service.viewerPayload(record.gameId).shouldNotBeNull()

            // The prefix re-simulates perfectly — this is not a fidelity problem (the frames came
            // back complete, or `viewerPayload` would have taken the diverged branch), and the
            // position is still reproducible, so "share as scenario" keeps working. There are simply
            // fewer frames than were played, and the viewer has to say so.
            payload.stateReproducible shouldBe true
            payload.degradedReason.shouldNotBeNull() shouldContain "recorded"
        }

        test("an untruncated replay is served without a caveat") {
            val record = truncatedGame(cap = 10_000).record()
            record.truncated shouldBe false
            val store = InMemoryReplayStore().apply {
                save(StoredReplay(record, ReplayStatus.FINISHED))
            }
            val service = ReplayService(
                store,
                ReplayReconstructor(cardRegistry, null),
                mockk(relaxed = true),
            )

            service.viewerPayload(record.gameId).shouldNotBeNull().degradedReason shouldBe null
        }
    }
}
