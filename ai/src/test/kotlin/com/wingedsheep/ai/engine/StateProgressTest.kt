package com.wingedsheep.ai.engine

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.GameRng
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * What [StateProgress.digest] must and must not notice.
 *
 * The digest decides whether an action accomplished anything, and [Strategist] permanently refuses
 * an action that accomplished nothing — so a game fact the digest is blind to is not a rounding
 * error, it is an ability the AI can never use again. That asymmetry is why `normalized` names the
 * fields it *excludes* rather than the ones it reads, and this is the test that keeps it honest:
 * the excluded list is short and fixed, so it can be checked exhaustively, while the fields that
 * must count are open-ended and covered by spot-checking the turn-level riders an ability can set
 * without touching any permanent.
 */
class StateProgressTest : FunSpec({

    fun state() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
    }.state

    test("bookkeeping that every action advances is not a change") {
        val base = state()
        val here = StateProgress.digest(base)

        // Each of these moves whenever anything resolves at all, so reading them would make every
        // inert action look like progress — the exact misreading the guard exists to avoid.
        withClue("rng") { StateProgress.digest(base.copy(rng = GameRng(0x5EED))) shouldBe here }
        withClue("nextEntityId") { StateProgress.digest(base.copy(nextEntityId = 9_999L)) shouldBe here }
        withClue("timestamp") { StateProgress.digest(base.copy(timestamp = 9_999L)) shouldBe here }

        // Whose turn it is to speak is not what is true of the board. Being blind to it is what
        // makes an action's own resolution comparable with the position it started from.
        withClue("priorityPlayerId") {
            StateProgress.digest(base.copy(priorityPlayerId = base.turnOrder[1])) shouldBe here
        }
        withClue("priorityPassedBy") {
            StateProgress.digest(base.copy(priorityPassedBy = base.turnOrder.toSet())) shouldBe here
        }
    }

    test("a turn-level rider an ability can set without touching a permanent is a change") {
        val base = state()
        val here = StateProgress.digest(base)

        // None of these live on a permanent, so nothing in the per-object walk would catch them.
        // Under the read-list this replaced they were all invisible, which would have made an
        // ability whose only effect is one of them permanently un-takeable.
        withClue("damageCantBePreventedThisTurn") {
            StateProgress.digest(base.copy(damageCantBePreventedThisTurn = true)) shouldNotBe here
        }
        withClue("spellWarpedThisTurn") {
            StateProgress.digest(base.copy(spellWarpedThisTurn = true)) shouldNotBe here
        }
        withClue("nonlandPermanentLeftBattlefieldThisTurn") {
            StateProgress.digest(base.copy(nonlandPermanentLeftBattlefieldThisTurn = true)) shouldNotBe here
        }
        withClue("playersWhoCommittedCrimeThisTurn") {
            StateProgress.digest(base.copy(playersWhoCommittedCrimeThisTurn = setOf(base.turnOrder[0]))) shouldNotBe here
        }
        withClue("dayNight") {
            StateProgress.digest(base.copy(dayNight = DayNight.NIGHT)) shouldNotBe here
        }
    }

    test("turn and step are part of the position, so a digest can only recur inside one window") {
        val base = state()
        val here = StateProgress.digest(base)

        // This is what bounds `Strategist.positionsActedFrom`: the same board one turn later is a
        // different position, so a remembered entry can only ever match while matching means
        // going in circles.
        StateProgress.digest(base.copy(turnNumber = base.turnNumber + 1)) shouldNotBe here
    }
})
