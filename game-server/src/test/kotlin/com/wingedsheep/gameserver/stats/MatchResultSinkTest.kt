package com.wingedsheep.gameserver.stats

import com.wingedsheep.gameserver.persistence.MatchResultRepository
import com.wingedsheep.gameserver.persistence.MatchResultRow
import com.wingedsheep.gameserver.persistence.TournamentRepository
import com.wingedsheep.gameserver.persistence.TournamentRow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.util.UUID

private val SOME_USER: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000aa")

class MatchResultSinkTest : FunSpec({

    fun match(vararg participants: RecordedParticipant) = RecordedMatch(
        gameId = "g", format = "Standard", tournamentName = null, gameMode = "CASUAL",
        frameCount = 20, turnCount = 6, startedAt = Instant.now(), endedAt = Instant.now(),
        participants = participants.toList(),
    )

    test("AI-only games are not recorded") {
        val repo = mockk<MatchResultRepository>(relaxed = true)
        JdbcMatchResultSink(repo).record(
            match(
                RecordedParticipant(userId = null, playerName = "AI 1", won = true, isAi = true),
                RecordedParticipant(userId = null, playerName = "AI 2", won = false, isAi = true),
            )
        )
        verify(exactly = 0) { repo.save(any()) }
    }

    test("a game with a human seat is recorded with its deck cards") {
        val repo = mockk<MatchResultRepository>(relaxed = true)
        val saved = slot<MatchResultRow>()
        every { repo.save(capture(saved)) } answers { saved.captured }
        JdbcMatchResultSink(repo).record(
            match(
                RecordedParticipant(
                    userId = SOME_USER, playerName = "Alice", won = true, colors = "WU", setCodes = "DSK",
                    isAi = false, clientIp = "1.2.3.4", deckCards = mapOf("Plains" to 10, "Island" to 8),
                ),
                RecordedParticipant(userId = null, playerName = "AI", won = false, isAi = true),
            )
        )
        verify(exactly = 1) { repo.save(any()) }
        val alice = saved.captured.participants.first { it.playerName == "Alice" }
        alice.clientIp shouldBe "1.2.3.4"
        alice.cards shouldHaveSize 2
        alice.cards.first { it.cardName == "Plains" }.copies shouldBe 10
    }

    test("guest-only games (no AI, no account) are still recorded for global stats") {
        val repo = mockk<MatchResultRepository>(relaxed = true)
        every { repo.save(any()) } answers { firstArg() }
        JdbcMatchResultSink(repo).record(
            match(
                RecordedParticipant(userId = null, playerName = "Guest 1", won = true, isAi = false),
                RecordedParticipant(userId = null, playerName = "Guest 2", won = false, isAi = false),
            )
        )
        verify(exactly = 1) { repo.save(any()) }
    }

    test("recordStarted persists an in-progress row for a human seat but skips AI-only") {
        val repo = mockk<TournamentRepository>(relaxed = true)
        every { repo.findFirstByLobbyIdOrderByIdDesc(any()) } returns null
        val saved = slot<TournamentRow>()
        every { repo.save(capture(saved)) } answers { saved.captured }
        val sink = JdbcTournamentResultSink(repo)

        sink.recordStarted(tournament(RecordedTournamentParticipant(null, "Bot", isAi = true, placement = 0, wins = 0, losses = 0, draws = 0)))
        verify(exactly = 0) { repo.save(any()) }

        sink.recordStarted(tournament(RecordedTournamentParticipant(SOME_USER, "Carol", isAi = false, placement = 0, wins = 0, losses = 0, draws = 0)))
        verify(exactly = 1) { repo.save(any()) }
        saved.captured.status shouldBe TournamentStatus.IN_PROGRESS.name
    }

    test("recordStarted is idempotent — a second start for the same lobby does not insert again") {
        val repo = mockk<TournamentRepository>(relaxed = true)
        every { repo.findFirstByLobbyIdOrderByIdDesc(any()) } returns
            TournamentRow(id = 1, lobbyId = "l", status = "IN_PROGRESS")
        JdbcTournamentResultSink(repo).recordStarted(
            tournament(RecordedTournamentParticipant(SOME_USER, "Carol", isAi = false, placement = 0, wins = 0, losses = 0, draws = 0))
        )
        verify(exactly = 0) { repo.save(any()) }
    }

    test("recordCompleted upserts the in-progress row, preserving its id and start time") {
        val repo = mockk<TournamentRepository>(relaxed = true)
        val startedAt = Instant.now()
        every { repo.findFirstByLobbyIdOrderByIdDesc("l") } returns
            TournamentRow(id = 7, lobbyId = "l", status = "IN_PROGRESS", startedAt = startedAt)
        val saved = slot<TournamentRow>()
        every { repo.save(capture(saved)) } answers { saved.captured }

        JdbcTournamentResultSink(repo).recordCompleted(
            tournament(RecordedTournamentParticipant(SOME_USER, "Carol", isAi = false, placement = 1, wins = 3, losses = 0, draws = 0))
        )
        saved.captured.id shouldBe 7
        saved.captured.status shouldBe TournamentStatus.COMPLETED.name
        saved.captured.startedAt shouldBe startedAt
        saved.captured.winnerName shouldBe "Carol"
    }

    test("recordProgress refreshes an in-progress row's standings, keeping its id and start time") {
        val repo = mockk<TournamentRepository>(relaxed = true)
        val startedAt = Instant.now()
        every { repo.findFirstByLobbyIdOrderByIdDesc("l") } returns
            TournamentRow(id = 7, lobbyId = "l", status = "IN_PROGRESS", startedAt = startedAt)
        val saved = slot<TournamentRow>()
        every { repo.save(capture(saved)) } answers { saved.captured }

        JdbcTournamentResultSink(repo).recordProgress(
            progress(RecordedTournamentParticipant(SOME_USER, "Carol", isAi = false, placement = 1, wins = 2, losses = 1, draws = 0))
        )
        saved.captured.id shouldBe 7
        saved.captured.status shouldBe TournamentStatus.IN_PROGRESS.name
        saved.captured.startedAt shouldBe startedAt
        val carol = saved.captured.participants.first { it.playerName == "Carol" }
        carol.wins shouldBe 2
        carol.losses shouldBe 1
        carol.placement shouldBe 1
    }

    test("recordProgress does not resurrect a completed or abandoned tournament, nor create a missing one") {
        val repo = mockk<TournamentRepository>(relaxed = true)
        every { repo.save(any()) } answers { firstArg() }
        val sink = JdbcTournamentResultSink(repo)
        val human = progress(RecordedTournamentParticipant(SOME_USER, "Carol", isAi = false, placement = 1, wins = 1, losses = 0, draws = 0))

        // No row yet → nothing to refresh (start hasn't been recorded).
        every { repo.findFirstByLobbyIdOrderByIdDesc("l") } returns null
        sink.recordProgress(human)

        // Already completed → left untouched.
        every { repo.findFirstByLobbyIdOrderByIdDesc("l") } returns TournamentRow(id = 1, lobbyId = "l", status = "COMPLETED")
        sink.recordProgress(human)

        // Already abandoned → left untouched.
        every { repo.findFirstByLobbyIdOrderByIdDesc("l") } returns TournamentRow(id = 1, lobbyId = "l", status = "ABANDONED")
        sink.recordProgress(human)

        verify(exactly = 0) { repo.save(any()) }
    }

    test("recordProgress skips AI-only tournaments") {
        val repo = mockk<TournamentRepository>(relaxed = true)
        JdbcTournamentResultSink(repo).recordProgress(
            progress(RecordedTournamentParticipant(null, "Bot", isAi = true, placement = 1, wins = 1, losses = 0, draws = 0))
        )
        verify(exactly = 0) { repo.save(any()) }
    }

    test("recordAbandoned flips only in-progress rows to ABANDONED") {
        val repo = mockk<TournamentRepository>(relaxed = true)
        val saved = slot<TournamentRow>()
        every { repo.save(capture(saved)) } answers { saved.captured }

        // No row recorded yet → nothing to abandon.
        every { repo.findFirstByLobbyIdOrderByIdDesc("l") } returns null
        JdbcTournamentResultSink(repo).recordAbandoned("l")
        verify(exactly = 0) { repo.save(any()) }

        // Already completed → left untouched.
        every { repo.findFirstByLobbyIdOrderByIdDesc("l") } returns TournamentRow(id = 1, lobbyId = "l", status = "COMPLETED")
        JdbcTournamentResultSink(repo).recordAbandoned("l")
        verify(exactly = 0) { repo.save(any()) }

        // In progress → marked abandoned.
        every { repo.findFirstByLobbyIdOrderByIdDesc("l") } returns TournamentRow(id = 1, lobbyId = "l", status = "IN_PROGRESS")
        JdbcTournamentResultSink(repo).recordAbandoned("l")
        saved.captured.status shouldBe TournamentStatus.ABANDONED.name
    }
})

private fun tournament(vararg p: RecordedTournamentParticipant) = RecordedTournament(
    lobbyId = "l", name = "Test", format = "SEALED", gameMode = "TOURNAMENT", setCodes = "DSK",
    playerCount = p.size, rounds = 3, gamesPerMatch = 1, winnerName = "Carol",
    startedAt = null, endedAt = Instant.now(), participants = p.toList(),
)

/** An in-progress snapshot: no end time and no winner yet, as [TournamentMatchHandler] builds it. */
private fun progress(vararg p: RecordedTournamentParticipant) = RecordedTournament(
    lobbyId = "l", name = "Test", format = "SEALED", gameMode = "TOURNAMENT", setCodes = "DSK",
    playerCount = p.size, rounds = 1, gamesPerMatch = 1, winnerName = null,
    startedAt = null, endedAt = null, participants = p.toList(),
)
