package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.state.YieldKind
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.ai.AiReplayHistory
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AbilityIdentity
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * The two runaway backstops, wired end to end through a real recorded session.
 *
 * Both are reached here with tightened thresholds ([GameSession.tightenBackstopsForTesting]) rather
 * than by playing 25,000 actions, so what these prove is the *wiring*: that the replay log really
 * stops growing and says so, that the game really ends and explains itself, and — the part that is
 * easy to get backwards — that the recording gives up without taking the game down with it.
 */
class GameSessionBackstopTest : ScenarioTestBase() {

    private fun mockWs(id: String): WebSocketSession =
        mockk(relaxed = true) { every { this@mockk.id } returns id }

    /** A real two-seat recorded game, with the backstops shrunk to test size before it starts. */
    private fun startedGame(guard: GameStallGuard, replayCap: Int): GameSession {
        val session = GameSession(cardRegistry = cardRegistry, maxPlayers = 2)
        session.tightenBackstopsForTesting(guard, replayCap)
        val p1 = com.wingedsheep.sdk.model.EntityId.of("backstop-p1")
        val p2 = com.wingedsheep.sdk.model.EntityId.of("backstop-p2")
        session.addPlayer(PlayerSession(mockWs("backstop-ws1"), p1, "Alice"), mapOf("Forest" to 40))
        session.addPlayer(PlayerSession(mockWs("backstop-ws2"), p2, "Bob"), mapOf("Forest" to 40))
        session.startGame()
        session.keepHand(p1)
        session.keepHand(p2)
        return session
    }

    /** Auto-pass until the game ends or [rounds] passes run out. */
    private fun GameSession.autoPass(rounds: Int) {
        repeat(rounds) {
            val state = getStateForTesting() ?: return
            if (state.gameOver) return
            state.priorityPlayerId?.let { executeAutoPass(it) }
        }
    }

    init {
        test("the replay recording freezes at its cap, and the game plays on") {
            val session = startedGame(GameStallGuard(), replayCap = 6)

            session.autoPass(40)

            // The record is a prefix and admits it. Frame count is 1 + actions, so it describes the
            // recording rather than the game — which is exactly what the viewer needs to know.
            session.getRecordedActions().size shouldBe 6
            session.isReplayTruncated() shouldBe true
            session.getReplayFrameCount() shouldBe 7
            // The game itself is untouched: dropping the *record* of a pathological game is cheap,
            // ending the game somebody is playing is not, so this ordering is load-bearing.
            session.isGameOver() shouldBe false

            // And the flush snapshot carries the flag, so the stored row stops claiming to be whole.
            session.replayRecordingSnapshot().shouldNotBeNull().truncated shouldBe true

            // The AI API must make the same distinction structurally. Yield recording freezes with
            // the action prefix; otherwise the purported prefix could contain mutations from later
            // in the live game.
            val yieldCountAtTruncation = session.getReplayYields().size
            session.setAbilityYield(
                EntityId.of("backstop-p1"),
                AbilityIdentity("Test#1", AbilityId("ability_after_truncation")),
                YieldKind.ALWAYS_ANSWER_YES,
            )
            session.getReplayYields().size shouldBe yieldCountAtTruncation
            session.getAiRuntimeSnapshot().shouldNotBeNull().replayHistory
                .shouldBeInstanceOf<AiReplayHistory.TruncatedPrefix>()
        }

        test("a game that stops making progress is ended as a draw that explains itself") {
            // keepHand ×2 are already recorded, so this trips a few auto-passes in.
            val session = startedGame(GameStallGuard(maxActions = 8), replayCap = 10_000)

            session.autoPass(40)

            session.isGameOver() shouldBe true
            // A draw, not a win for whoever happened to hold priority when the clock ran out.
            session.getWinnerId() shouldBe null
            session.getGameOverReason() shouldBe GameOverReason.DRAW
            session.stallMessage().shouldNotBeNull() shouldContain "draw"
            // Nobody is eliminated by a draw — the Free-for-All standings read this order, and a
            // seat listed as eliminated would be reported as having lost a game that nobody lost.
            session.getEliminationOrder() shouldBe emptyList()
        }

        test("an abandoned game ends as a draw that explains itself, once") {
            val session = startedGame(GameStallGuard(), replayCap = 10_000)

            session.abandon("Nobody is here.") shouldBe true

            session.isGameOver() shouldBe true
            session.getWinnerId() shouldBe null
            session.getGameOverReason() shouldBe GameOverReason.DRAW
            session.stallMessage() shouldBe "Nobody is here."
            // A game that already ended keeps its ending — the sweeper racing a real result must lose.
            session.abandon("Again.") shouldBe false
            session.stallMessage() shouldBe "Nobody is here."
        }

        test("an unstarted game can't be abandoned through the session") {
            GameSession(cardRegistry = cardRegistry, maxPlayers = 2).abandon("Nobody is here.") shouldBe false
        }

        test("an ordinary game is neither truncated nor called stalled") {
            val session = startedGame(GameStallGuard(), replayCap = 10_000)

            session.autoPass(40)

            session.setAbilityYield(
                EntityId.of("backstop-p1"),
                AbilityIdentity("Test#1", AbilityId("ability_in_complete_history")),
                YieldKind.ALWAYS_ANSWER_YES,
            )

            session.stallMessage() shouldBe null
            session.isReplayTruncated() shouldBe false
            val history = session.getAiRuntimeSnapshot().shouldNotBeNull().replayHistory
                .shouldBeInstanceOf<AiReplayHistory.Complete>()
            history.actions shouldBe session.getRecordedActions()
            history.yields shouldBe session.getReplayYields()
        }
    }
}
