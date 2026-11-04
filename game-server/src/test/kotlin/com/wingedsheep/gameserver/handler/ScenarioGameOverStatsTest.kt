package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.stats.MatchResultSink
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * A Scenario Builder game — hand-built, or a position forked out of a replay frame — is practice.
 * Its game over must not land in match history (the account stats and the replay list read it);
 * an ordinary game that ends the same way still does.
 */
class ScenarioGameOverStatsTest : FunSpec({

    fun endGame(isScenario: Boolean): MatchResultSink {
        val sink = mockk<MatchResultSink>(relaxed = true)
        val gameRepository = mockk<GameRepository>(relaxed = true) {
            every { getLobbyForGame(any()) } returns null
        }
        val game = mockk<GameSession>(relaxed = true) {
            every { sessionId } returns "scenario-game"
            every { this@mockk.isScenario } returns isScenario
            every { getWinnerId() } returns EntityId.of("p1")
            every { getWinnerIds() } returns listOf(EntityId.of("p1"))
            every { stallMessage() } returns null
            every { getPlayers() } returns emptyList()
            every { getSpectators() } returns emptySet()
            every { departedForMatch } returns mutableSetOf()
            every { getReplaySetup() } returns null
            every { getReplayFrameCount() } returns 0
            every { getRecordedActions() } returns emptyList()
            every { ranked } returns false
            every { playingForAnte } returns false
            every { quickGameSetCode } returns null
        }
        val handler = GamePlayHandler(
            sessionRegistry = mockk(relaxed = true),
            gameRepository = gameRepository,
            lobbyRepository = mockk(relaxed = true),
            sender = mockk(relaxed = true),
            cardRegistry = mockk(relaxed = true),
            printingRegistry = mockk(relaxed = true),
            tokenArtRegistry = mockk(relaxed = true),
            deckGenerator = mockk(relaxed = true),
            gameProperties = mockk(relaxed = true),
            replayService = mockk(relaxed = true),
            replayCheckpointFlusher = mockk(relaxed = true),
            engineVersion = mockk(relaxed = true),
            aiGameManager = mockk(relaxed = true),
            matchResultSink = sink,
            rankedResultSink = mockk(relaxed = true),
            deckProfiler = mockk(relaxed = true),
            anteSpoilsSink = mockk(relaxed = true),
        )
        handler.handleGameOver(game)
        return sink
    }

    test("a scenario game that reaches a winner records no match") {
        val sink = endGame(isScenario = true)
        verify(exactly = 0) { sink.record(any()) }
    }

    test("an ordinary game that reaches a winner is still recorded") {
        val sink = endGame(isScenario = false)
        verify(exactly = 1) { sink.record(any()) }
    }
})
