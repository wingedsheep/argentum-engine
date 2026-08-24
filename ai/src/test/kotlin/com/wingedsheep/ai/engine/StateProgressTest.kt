package com.wingedsheep.ai.engine

import com.wingedsheep.engine.state.components.battlefield.HasBecomeTappedComponent
import com.wingedsheep.engine.state.components.battlefield.TargetedByControllerThisTurnComponent
import com.wingedsheep.engine.state.components.player.EquipActivationsThisTurnComponent
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

    test("an it-happened memory on a permanent is bookkeeping, not a position") {
        // The other half of what `normalized` excludes: `IGNORED_COMPONENTS`, applied in the
        // per-object walk. Pinned here rather than only in LoopingActionAiTest so a refactor of the
        // ignore list fails in the file that documents it.
        //
        // Injected rather than produced by a real tap on purpose: `tap()` also sets
        // TappedComponent, which *is* a position change. What must be invisible is the stamp it
        // writes alongside — Aphetto Alchemist's self-untap pays its own cost back and leaves
        // nothing but that stamp behind, and a digest that read it would call the no-op progress.
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all)
            initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        }
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val base = driver.state
        val here = StateProgress.digest(base)

        withClue("HasBecomeTappedComponent") {
            val stamped = base.updateEntity(bears) {
                it.with(HasBecomeTappedComponent(base.turnNumber, timesThisTurn = 1))
            }
            StateProgress.digest(stamped) shouldBe here
        }
        withClue("TargetedByControllerThisTurnComponent") {
            val targeted = base.updateEntity(bears) {
                it.with(TargetedByControllerThisTurnComponent(setOf(driver.player1)))
            }
            StateProgress.digest(targeted) shouldBe here
        }
    }

    test("the equip tally counts its first activation and then stops counting") {
        // The one component that is neither read in full nor ignored wholesale, because its
        // readers only ask "any yet?": `CastPermissionUtils.applyFreeFirstEquipDiscount` tests
        // `count == 0`, so 0 -> 1 spends Forge Anew's free equip for the turn and is a game fact,
        // while 1 -> 2 -> 3 is bookkeeping no card can see. Reading the raw count is what let a
        // free equip aimed at the creature the Equipment was already on hash as a fresh position
        // every time round, which is the loop `saturated` exists to close.
        val base = state()
        val you = base.turnOrder[0]
        fun withEquips(count: Int) =
            base.updateEntity(you) { it.with(EquipActivationsThisTurnComponent(count)) }

        val none = StateProgress.digest(withEquips(0))
        val once = StateProgress.digest(withEquips(1))
        withClue("spending the turn's first equip is a change") { once shouldNotBe none }
        withClue("a second equip activation is not") { StateProgress.digest(withEquips(2)) shouldBe once }
        withClue("nor a third") { StateProgress.digest(withEquips(3)) shouldBe once }
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
