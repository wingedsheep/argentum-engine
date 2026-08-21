package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Timesifter
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Timesifter (MRD #262) — "At the beginning of each upkeep, each player exiles the top card of
 * their library. The player who exiled the card with the greatest mana value takes an extra turn
 * after this one. If two or more players' cards are tied for greatest, the tied players repeat
 * this process until the tie is broken."
 *
 * The engine models "take an extra turn" as every *other* player skipping their next turn
 * (`SkipNextTurnComponent`), so that component is what these tests read to identify the winner —
 * asserting on the exiled cards alone would pass even if the extra turn went to the wrong player.
 *
 * Three things are worth pinning beyond the happy path, because each is a way the contest can go
 * subtly wrong: the tie must repeat among the **tied players only** (not the whole table, and not
 * once more for everyone), a player with an empty library exiles nothing and therefore **can't
 * win**, and a contest nobody can contest must end with **no winner at all** rather than defaulting
 * to Timesifter's controller — which is exactly what an unguarded pipeline target would do.
 */
class TimesifterScenarioTest : FunSpec({

    // Hoisted out of the test bodies: `TestCards.all` forces a ClassGraph scan of the whole corpus,
    // and paying that inside a test puts it under the per-test timeout.
    val cards = TestCards.all + Timesifter

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Stack a player's library top-down: `stackLibrary(p, "A", "B")` exiles A first, then B. */
    fun GameTestDriver.stackLibrary(playerId: EntityId, vararg topDown: String) {
        topDown.reversed().forEach { putCardOnTopOfLibrary(playerId, it) }
    }

    fun GameTestDriver.emptyLibrary(playerId: EntityId) {
        replaceState(state.copy(zones = state.zones + (ZoneKey(playerId, Zone.LIBRARY) to emptyList())))
    }

    fun GameTestDriver.skippedTurns(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<SkipNextTurnComponent>()?.turns ?: 0

    /** Roll into the opponent's upkeep and let the (choice-free) contest resolve. */
    fun GameTestDriver.runContestAtNextUpkeep() = passPriorityUntil(Step.DRAW)

    test("the player who exiled the greatest mana value takes the extra turn") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Timesifter")
        d.stackLibrary(d.player1, "Bonesplitter")  // {1}
        d.stackLibrary(d.player2, "Alpha Myr")     // {2}

        d.runContestAtNextUpkeep()

        withClue("each player exiled exactly the top card of their library") {
            d.getExileCardNames(d.player1) shouldContainExactly listOf("Bonesplitter")
            d.getExileCardNames(d.player2) shouldContainExactly listOf("Alpha Myr")
        }
        withClue("{2} beats {1}, so player 2 takes the extra turn — modelled as everyone else skipping theirs") {
            d.skippedTurns(d.player1) shouldBe 1
            d.skippedTurns(d.player2) shouldBe 0
        }
    }

    test("a tie repeats the process for the tied players until it is broken") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Timesifter")
        // Round 1 ties at {2}; round 2 breaks it {4} to {1}.
        d.stackLibrary(d.player1, "Alpha Myr", "Bonesplitter")  // {2} then {1}
        d.stackLibrary(d.player2, "Alpha Myr", "Duskworker")    // {2} then {4}

        d.runContestAtNextUpkeep()

        withClue("the tied round is repeated, so a second card leaves each library — in order") {
            d.getExileCardNames(d.player1) shouldContainExactly listOf("Alpha Myr", "Bonesplitter")
            d.getExileCardNames(d.player2) shouldContainExactly listOf("Alpha Myr", "Duskworker")
        }
        withClue("the second round decides it: {4} over {1}") {
            d.skippedTurns(d.player1) shouldBe 1
            d.skippedTurns(d.player2) shouldBe 0
        }
    }

    test("a player with an empty library exiles nothing and so cannot win the contest") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Timesifter")
        d.emptyLibrary(d.player1)
        d.stackLibrary(d.player2, "Ornithopter")   // {0} — the lowest possible mana value

        d.runContestAtNextUpkeep()

        withClue("nothing to exile from an empty library") {
            d.getExileCardNames(d.player1) shouldContainExactly emptyList()
            d.getExileCardNames(d.player2) shouldContainExactly listOf("Ornithopter")
        }
        withClue("a {0} card still beats exiling nothing at all — the player who exiled no card can't be the one who exiled the greatest mana value") {
            d.skippedTurns(d.player1) shouldBe 1
            d.skippedTurns(d.player2) shouldBe 0
        }
    }

    test("with no card exiled at all the contest ends undecided — nobody takes an extra turn") {
        val d = driver()
        val timesifter = d.putPermanentOnBattlefield(d.player1, "Timesifter")
        timesifter shouldNotBe null
        d.emptyLibrary(d.player1)
        d.emptyLibrary(d.player2)

        d.runContestAtNextUpkeep()

        withClue("no winner means no extra turn — in particular it must NOT fall back to Timesifter's controller") {
            d.skippedTurns(d.player1) shouldBe 0
            d.skippedTurns(d.player2) shouldBe 0
        }
    }
})
