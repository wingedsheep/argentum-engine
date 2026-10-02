package com.wingedsheep.gameserver.replay

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.GZIPOutputStream

/**
 * A replay exported as a file and uploaded back is the same game: same inputs, same re-simulation.
 * And because an upload is untrusted input the server then *runs*, every dimension that scales the
 * cost of that run is refused up front.
 */
class ReplayFileTest : ScenarioTestBase() {

    private fun mockWs(id: String): WebSocketSession =
        mockk(relaxed = true) { every { this@mockk.id } returns id }

    private fun recordGame(): CompactReplay {
        val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 2)
        val p1 = EntityId.of("file-p1")
        val p2 = EntityId.of("file-p2")
        val deck = mapOf("Forest" to 30, "Llanowar Elves" to 10)
        session.addPlayer(PlayerSession(mockWs("file-ws1"), p1, "Alice"), deck)
        session.addPlayer(PlayerSession(mockWs("file-ws2"), p2, "Bob"), deck)
        session.startGame()
        session.keepHand(p1)
        session.keepHand(p2)
        repeat(40) {
            val state = session.getStateForTesting() ?: return@repeat
            if (state.gameOver) return@repeat
            state.priorityPlayerId?.let { session.executeAutoPass(it) }
        }
        return CompactReplay(
            gameId = session.sessionId,
            players = session.getPlayers().map { ReplayPlayerInfo(it.playerId.value, it.playerName) },
            startedAt = Instant.now().toString(),
            endedAt = Instant.now().toString(),
            winnerName = null,
            setup = session.getReplaySetup().shouldNotBeNull(),
            actions = session.getRecordedActions(),
            yields = session.getReplayYields(),
            pinnedCards = session.getPinnedCards(),
            checkpoints = session.getReplayCheckpoints(),
        )
    }

    private fun gzip(text: String): ByteArray =
        ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(text.toByteArray()) } }
            .toByteArray()

    init {
        test("an exported replay uploads back as the same game, plain, gzipped or codec-encoded") {
            val replay = recordGame()
            val exported = ReplayFile.export(replay)

            ReplayFile.parse(exported.toByteArray()) shouldBe replay
            ReplayFile.parse(gzip(exported)) shouldBe replay
            ReplayFile.parse(ReplayCodec.encode(replay).toByteArray()) shouldBe replay
            ReplayReconstructor(cardRegistry, null).reconstruct(ReplayFile.parse(exported.toByteArray()))
                .fidelity shouldBe ReplayFidelity.EXACT
        }

        test("an uploaded replay plays, but offers no scenario sharing — it isn't in the store") {
            val replay = recordGame()
            val service = ReplayService(InMemoryReplayStore(), ReplayReconstructor(cardRegistry, null), mockk(relaxed = true))

            val payload = service.viewerPayloadForUpload(replay).shouldNotBeNull()
            payload.fidelity shouldBe ReplayFidelity.EXACT
            payload.frameCount shouldBe replay.frameCount
            payload.stateReproducible shouldBe false
            // Nothing was saved by watching it.
            service.findStored(replay.gameId).shouldBeNull()
        }

        test("an in-progress game's full state is not served — it would reveal hidden hands") {
            val replay = recordGame()
            val store = InMemoryReplayStore()
            val service = ReplayService(store, ReplayReconstructor(cardRegistry, null), mockk(relaxed = true))

            service.saveInProgress(replay, resumeFingerprint = null)
            service.reconstructStateAt(replay.gameId, 1).shouldBeNull()

            store.save(StoredReplay(replay, ReplayStatus.FINISHED))
            service.reconstructStateAt(replay.gameId, 1).shouldNotBeNull()
        }

        test("files that aren't replays, or would cost too much to run, are refused with a reason") {
            val replay = recordGame()

            shouldThrow<ReplayFileException> { ReplayFile.parse(ByteArray(0)) }
            shouldThrow<ReplayFileException> { ReplayFile.parse("""{"hello":"world"}""".toByteArray()) }
            shouldThrow<ReplayFileException> { ReplayFile.parse(byteArrayOf(0x1f, 0x8b.toByte(), 1, 2, 3)) }

            val tooLong = replay.copy(
                actions = List(ReplayRecordingPolicy.MAX_RECORDED_ACTIONS + 1) { replay.actions.first() }
            )
            shouldThrow<ReplayFileException> { ReplayFile.parse(ReplayFile.export(tooLong).toByteArray()) }
                .message shouldContain "actions"

            val hugeDeck = replay.copy(
                setup = replay.setup.copy(
                    players = replay.setup.players.map {
                        it.copy(deck = it.deck.copy(cards = List(ReplayFile.MAX_CARDS_PER_DECK + 1) { "Forest" }, cardEntries = emptyList()))
                    }
                )
            )
            shouldThrow<ReplayFileException> { ReplayFile.parse(ReplayFile.export(hugeDeck).toByteArray()) }
                .message shouldContain "Deck"

            val oversized = ByteArray(ReplayFile.MAX_UPLOAD_BYTES + 1) { ' '.code.toByte() }
            shouldThrow<ReplayFileException> { ReplayFile.read(ByteArrayInputStream(oversized)) }
                .message shouldContain "too large"
        }
    }
}
