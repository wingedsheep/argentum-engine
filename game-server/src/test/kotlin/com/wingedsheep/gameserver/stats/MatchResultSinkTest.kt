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

    test("tournaments with at least one human seat are recorded; AI-only are skipped") {
        val repo = mockk<TournamentRepository>(relaxed = true)
        val saved = slot<TournamentRow>()
        every { repo.save(capture(saved)) } answers { saved.captured }
        val sink = JdbcTournamentResultSink(repo)

        sink.record(tournament(RecordedTournamentParticipant(null, "Bot", isAi = true, placement = 1, wins = 3, losses = 0, draws = 0)))
        verify(exactly = 0) { repo.save(any()) }

        sink.record(tournament(RecordedTournamentParticipant(SOME_USER, "Carol", isAi = false, placement = 1, wins = 3, losses = 0, draws = 0)))
        verify(exactly = 1) { repo.save(any()) }
        saved.captured.winnerName shouldBe "Carol"
    }
})

private fun tournament(vararg p: RecordedTournamentParticipant) = RecordedTournament(
    lobbyId = "l", name = "Test", format = "SEALED", gameMode = "TOURNAMENT", setCodes = "DSK",
    playerCount = p.size, rounds = 3, gamesPerMatch = 1, winnerName = "Carol",
    startedAt = null, endedAt = Instant.now(), participants = p.toList(),
)
