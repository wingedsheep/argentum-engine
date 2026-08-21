package com.wingedsheep.gameserver.tournament

import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Cross-round matchmaking liveness for the shape that actually stalls in production: one human and
 * seven AI seats, where the human finishes their round-1 match long before the AI-vs-AI games around
 * them.
 *
 * The mechanism behind that stall is [TournamentManager.startableMatches] versus
 * [TournamentManager.hasIncompleteMatchBefore]: a pair can go from blocked to startable purely
 * because a *third* pair's earlier-round game finished, with no change at all to the ready set. The
 * handler used to look for a startable match only on a false→true ready transition, so once every AI
 * was marked ready nothing ever re-asked and the round-2 slate sat idle — permanently, whenever the
 * round-complete fallback was skipped because the round had already been advanced.
 */
class TournamentProgressionTest : FunSpec({

    val human = EntityId("human")
    val ai = (1..7).map { EntityId("ai$it") }
    val everyone = (listOf(human) + ai).toSet()

    fun tournament(): TournamentManager = TournamentManager(
        lobbyId = "lobby-1952",
        players = (listOf(human to "Vincent") + ai.mapIndexed { i, id -> id to "AI ${i + 1}" }),
        gamesPerMatch = 1
    )

    fun TournamentRound.matchFor(playerId: EntityId): TournamentMatch =
        matches.first { it.player1Id == playerId || it.player2Id == playerId }

    fun TournamentMatch.opponentOf(playerId: EntityId): EntityId =
        (if (player1Id == playerId) player2Id else player1Id)!!

    // Launches a match the way the handler does: stamping a session id is what takes it out of both
    // `startableMatches` and `getNextMatchForPlayer`.
    fun TournamentManager.startAll(round: TournamentRound) {
        round.matches.forEachIndexed { i, match -> match.gameSessionId = "r${round.roundNumber}-g$i" }
    }

    test("the schedule opens on round 1, not round 2") {
        val manager = tournament()
        val first = manager.startNextRound()!!
        first.roundNumber shouldBe 1
        manager.currentRound?.roundNumber shouldBe 1
        // 8 players, no BYE: four concurrent matches per round, seven rounds of round robin.
        first.matches.size shouldBe 4
        first.matches.none { it.isBye } shouldBe true
        manager.totalRounds shouldBe 7
    }

    test("a pair blocked by an earlier-round match becomes startable when that match finishes") {
        val manager = tournament()
        val round1 = manager.startNextRound()!!

        // Everyone readies and all four round-1 matches start; starting consumes the ready flags.
        manager.startableMatches(everyone).size shouldBe 4
        manager.startAll(round1)
        manager.startableMatches(everyone).shouldBeEmpty()

        // The human wins early while the other three round-1 games are still running.
        val humanRound1 = round1.matchFor(human)
        val humansRound1Opponent = humanRound1.opponentOf(human)
        manager.reportMatchResult(humanRound1.gameSessionId!!, human)
        manager.isRoundComplete() shouldBe false

        // Both early finishers ready up for round 2 — the human by clicking Ready after dismissing the
        // game-over overlay, the AI through the auto-ready pass.
        val ready = mutableSetOf(human, humansRound1Opponent)

        // Their round-2 opponents are still mid-round-1, so nothing may start. That is the
        // `hasIncompleteMatchBefore` invariant and it has to stay intact.
        manager.startableMatches(ready).shouldBeEmpty()

        // The auto-ready pass sweeps every AI, including the six still playing round 1. Still nothing
        // may start: every candidate pair has a seat that owes a round-1 game.
        ready += ai
        manager.startableMatches(ready).shouldBeEmpty()

        // Now the blocker lands — the human's round-2 opponent finishes their round-1 match. No ready
        // flag changed, so a transition-triggered retry would never look again; re-examining the ready
        // set has to find the pair.
        val (round2, humansRound2) = manager.getNextMatchForPlayer(human)!!
        round2.roundNumber shouldBe 2
        val humansRound2Opponent = humansRound2.opponentOf(human)
        manager.hasIncompleteMatchBefore(humansRound2Opponent, 2) shouldBe true
        manager.reportMatchResult(round1.matchFor(humansRound2Opponent).gameSessionId!!, humansRound2Opponent)

        manager.isRoundComplete() shouldBe false
        manager.startableMatches(ready).map { it.second } shouldContainExactly listOf(humansRound2)
        manager.startableMatches(ready).single().first.roundNumber shouldBe 2
    }

    test("no seat is offered two matches at once, so the whole sweep can be launched in one pass") {
        val manager = tournament()
        val round1 = manager.startNextRound()!!

        // Every round-1 game is decided and everybody is ready. Rounds 2..7 are all unplayed, but only
        // round 2 may start: later rounds are held back by the round-2 games nobody has played yet.
        manager.startAll(round1)
        round1.matches.forEach { manager.reportMatchResult(it.gameSessionId!!, it.player1Id) }

        val startable = manager.startableMatches(everyone)
        startable.map { it.first.roundNumber }.toSet() shouldBe setOf(2)
        startable.size shouldBe 4
        startable.flatMap { (_, m) -> listOfNotNull(m.player1Id, m.player2Id) }.toSet() shouldBe everyone
    }

    test("a finished round keeps answering isRoundComplete until it is advanced past") {
        // Why the round-complete path advances the bracket itself. It used to get that for free: it
        // cleared the ready set, which made the AI-ready pass find work, which called startNextRound.
        // With readies surviving a round boundary that guard is false, and an un-advanced `currentRound`
        // is not cosmetic — `isRoundComplete()` reads it, so it answers true forever.
        val manager = tournament()
        val round1 = manager.startNextRound()!!
        manager.startAll(round1)
        round1.matches.forEach { manager.reportMatchResult(it.gameSessionId!!, it.player1Id) }

        manager.isRoundComplete() shouldBe true
        manager.currentRound?.roundNumber shouldBe 1

        manager.startNextRound()!!.roundNumber shouldBe 2
        manager.isRoundComplete() shouldBe false
    }

    test("a later round's result is attributable to that round, not to the current one") {
        // The other half: matches from later rounds run concurrently with the current round, so a
        // result arriving while `currentRound` still points at a finished round must not be taken as
        // closing it — that re-announces a closed round and clears every player's game-session pointer,
        // including seats that are mid-game. `getRoundForMatch` is what tells the two apart.
        val manager = tournament()
        val round1 = manager.startNextRound()!!
        manager.startAll(round1)
        round1.matches.forEach { manager.reportMatchResult(it.gameSessionId!!, it.player1Id) }

        // Round 1 is done but nothing advanced yet — exactly the window the sweep runs in.
        val (round2, someRound2Match) = manager.getNextMatchForPlayer(human)!!
        round2.roundNumber shouldBe 2
        someRound2Match.gameSessionId = "r2-eager"
        manager.reportMatchResult("r2-eager", human)

        manager.currentRound?.roundNumber shouldBe 1
        manager.getRoundForMatch("r2-eager")?.roundNumber shouldBe 2
        manager.getRoundForMatch(round1.matches.first().gameSessionId!!)?.roundNumber shouldBe 1
    }

    test("a player mid-match is distinguishable from one waiting, so a stale Ready can be refused") {
        // The handler refuses a ready click from a player whose match is still running: they can't have
        // dismissed a game-over overlay yet, and banking it would consent to the match *after* this one,
        // which the sweep would then launch the instant this one ended.
        val manager = tournament()
        val round1 = manager.startNextRound()!!
        val humanRound1 = round1.matchFor(human)

        manager.hasActiveMatch(human) shouldBe false
        manager.startAll(round1)
        manager.hasActiveMatch(human) shouldBe true
        manager.reportMatchResult(humanRound1.gameSessionId!!, human)
        manager.hasActiveMatch(human) shouldBe false
    }

    test("a seat that is not ready keeps its match out of the sweep") {
        val manager = tournament()
        val round1 = manager.startNextRound()!!
        val humanRound1 = round1.matchFor(human)
        val opponent = humanRound1.opponentOf(human)

        // The human hasn't dismissed the game-over overlay yet, so only their opponent is ready.
        manager.startableMatches(setOf(opponent)).shouldBeEmpty()
        manager.startableMatches(setOf(human)).shouldBeEmpty()
        manager.startableMatches(setOf(human, opponent)).map { it.second } shouldContainExactly listOf(humanRound1)
    }
})
