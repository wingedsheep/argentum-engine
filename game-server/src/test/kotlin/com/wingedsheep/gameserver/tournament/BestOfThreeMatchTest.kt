package com.wingedsheep.gameserver.tournament

import com.wingedsheep.gameserver.persistence.restoreTournamentManager
import com.wingedsheep.gameserver.persistence.toPersistent
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Best-of-three matches: first to two game wins takes the match, and between games the loser of the
 * previous game chooses who goes first (CR 103.1) — after a drawn game, whoever chose last time.
 */
class BestOfThreeMatchTest : FunSpec({

    val alice = EntityId("A")
    val bob = EntityId("B")

    fun twoPlayerBo3(): Pair<TournamentManager, TournamentMatch> {
        val manager = TournamentManager("lobby", listOf(alice to "Alice", bob to "Bob"), bestOf = 3)
        val round = manager.startNextRound()!!
        return manager to round.matches.single()
    }

    var gameCounter = 0
    fun TournamentManager.play(match: TournamentMatch, winner: EntityId?, starter: EntityId = match.player1Id): GameReport {
        val session = "game-${gameCounter++}"
        match.gameSessionId = session
        match.currentStarterId = starter
        return reportMatchResult(session, winner, winnerLifeRemaining = 5)
    }

    test("a single game doesn't decide a best-of-three; the match waits between games") {
        val (manager, match) = twoPlayerBo3()

        manager.play(match, alice).shouldBeInstanceOf<GameReport.NextGame>()

        match.isComplete shouldBe false
        match.isBetweenGames shouldBe true
        match.gameSessionId shouldBe null
        match.player1GameWins shouldBe 1
        manager.hasActiveMatch(alice) shouldBe true
        manager.hasActiveMatch(bob) shouldBe true
        // Between games the match is started by sideboarding, never by the round's ready set.
        manager.startableMatches(setOf(alice, bob)) shouldBe emptyList()
        manager.getNextMatchForPlayer(alice) shouldBe null
        // No match points yet — only the game.
        manager.getStandings().first { it.playerId == alice }.wins shouldBe 0
        manager.getStandings().first { it.playerId == alice }.gamesWon shouldBe 1
    }

    test("the loser of a game chooses who takes the first turn in the next (CR 103.1)") {
        val (manager, match) = twoPlayerBo3()
        manager.play(match, alice)
        match.playDrawChooserId shouldBe bob

        manager.play(match, bob)
        match.playDrawChooserId shouldBe alice
    }

    test("after a drawn game the player who chose for that game chooses again") {
        val (manager, match) = twoPlayerBo3()
        manager.play(match, alice)                 // Bob lost → Bob chooses game 2
        match.playDrawChooserId shouldBe bob
        manager.play(match, null, starter = alice) // game 2 drawn
        match.playDrawChooserId shouldBe bob
    }

    test("a drawn first game: whoever went first made the choice, so they choose again") {
        val (manager, match) = twoPlayerBo3()
        manager.play(match, null, starter = bob)
        match.playDrawChooserId shouldBe bob
    }

    test("two game wins take the match: 2-0") {
        val (manager, match) = twoPlayerBo3()
        manager.play(match, alice)
        manager.play(match, alice).shouldBeInstanceOf<GameReport.MatchDecided>()

        match.isComplete shouldBe true
        match.winnerId shouldBe alice
        val a = manager.getStandings().first { it.playerId == alice }
        val b = manager.getStandings().first { it.playerId == bob }
        a.wins shouldBe 1; a.gamesWon shouldBe 2; a.points shouldBe 3
        b.losses shouldBe 1; b.gamesLost shouldBe 2
        manager.hasActiveMatch(alice) shouldBe false
    }

    test("2-1 records every game and the match once") {
        val (manager, match) = twoPlayerBo3()
        manager.play(match, alice)
        manager.play(match, bob)
        manager.play(match, bob).shouldBeInstanceOf<GameReport.MatchDecided>()

        match.winnerId shouldBe bob
        match.player1GameWins shouldBe 1
        match.player2GameWins shouldBe 2
        match.completedGameSessionIds.size shouldBe 3
        manager.getCompletedGameSessionIds().size shouldBe 3
        val b = manager.getStandings().first { it.playerId == bob }
        b.wins shouldBe 1; b.gamesWon shouldBe 2; b.gamesLost shouldBe 1
        manager.getCurrentRoundResults().single().player2GameWins shouldBe 2
    }

    test("three games played without a second win: more game wins takes it, equal game wins is a draw") {
        val (manager, match) = twoPlayerBo3()
        manager.play(match, alice)
        manager.play(match, null)
        manager.play(match, null).shouldBeInstanceOf<GameReport.MatchDecided>()
        match.winnerId shouldBe alice

        val (drawManager, drawnMatch) = twoPlayerBo3()
        drawManager.play(drawnMatch, alice)
        drawManager.play(drawnMatch, bob)
        drawManager.play(drawnMatch, null).shouldBeInstanceOf<GameReport.MatchDecided>()
        drawnMatch.isDraw shouldBe true
        drawManager.getStandings().all { it.draws == 1 } shouldBe true
    }

    test("a drawn game is worth one game point in GW%") {
        val (manager, match) = twoPlayerBo3()
        manager.play(match, alice)
        manager.play(match, null)
        manager.play(match, alice)
        // Alice: 2 wins + 1 draw over 3 games = (6 + 1) / 9.
        manager.getStandingsInfo().first { it.playerId == alice.value }.gwPercent shouldBe (7.0 / 9.0 plusOrMinus 1e-9)
    }

    test("sideboarded decks are match-scoped: deciding the match drops them") {
        val (manager, match) = twoPlayerBo3()
        manager.play(match, alice)
        match.matchDecks[bob] = mapOf("Forest" to 40)
        match.sideboardReady.add(bob)
        manager.play(match, alice)
        match.matchDecks shouldBe emptyMap()
        match.sideboardReady shouldBe emptySet()
    }

    test("a player leaving between games forfeits the match") {
        val (manager, match) = twoPlayerBo3()
        manager.play(match, alice)
        manager.recordAbandon(alice)
        match.isComplete shouldBe true
        match.winnerId shouldBe bob
    }

    test("a best-of-one match is still decided by its single game") {
        val manager = TournamentManager("lobby", listOf(alice to "Alice", bob to "Bob"))
        val match = manager.startNextRound()!!.matches.single()
        manager.play(match, bob).shouldBeInstanceOf<GameReport.MatchDecided>()
        match.winnerId shouldBe bob
        match.gameSessionId shouldBe "game-${gameCounter - 1}"
    }

    test("between-games state survives a persistence round-trip") {
        val (manager, match) = twoPlayerBo3()
        manager.play(match, alice)
        match.matchDecks[bob] = mapOf("Forest" to 17, "Grizzly Bears" to 23)
        match.sideboardReady.add(bob)
        match.nextStarterId = bob

        val restored = restoreTournamentManager(manager.toPersistent("lobby"))
        restored.bestOf shouldBe 3
        val restoredMatch = restored.underwayMatchFor(bob)!!
        restoredMatch.isBetweenGames shouldBe true
        restoredMatch.playDrawChooserId shouldBe bob
        restoredMatch.lastGameWinnerId shouldBe alice
        restoredMatch.matchDecks[bob] shouldBe mapOf("Forest" to 17, "Grizzly Bears" to 23)
        restoredMatch.sideboardReady shouldBe setOf(bob)
        restoredMatch.nextStarterId shouldBe bob
        restoredMatch.completedGameSessionIds.size shouldBe 1
    }
})
