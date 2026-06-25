package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.ZimoneAllQuestioning
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Zimone, All-Questioning ({1}{G}{U}, 1/1):
 * "At the beginning of your end step, if a land entered the battlefield under your control this
 *  turn and you control a prime number of lands, create Primo, the Indivisible, a legendary 0/0
 *  green and blue Fractal creature token, then put that many +1/+1 counters on it."
 *
 * Exercises the new unary numeric-predicate condition `NumberMatches(amount, NumberProperty.Prime)`
 * (`Conditions.AmountIsPrime`) as the prime half of the trigger's intervening-if, ANDed with the
 * existing `LANDS_ENTERED_UNDER_CONTROL` tracker. Every clause of the oracle + the "1 is not prime"
 * ruling has a paired assertion:
 *  - prime count + land entered → Primo with `count` +1/+1 counters,
 *  - non-prime count → no token,
 *  - no land entered this turn → trigger doesn't fire even at a prime count,
 *  - 1 land (entered) → not prime, no token,
 *  - 2 lands (smallest prime) → Primo with two counters.
 *
 * `putLandOnBattlefield` adds lands without firing the entry tracker, so only the single land
 * played via `playLand` counts as "entered this turn" — letting each test pin the land count and
 * the entered-this-turn flag independently.
 */
class ZimoneAllQuestioningTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(ZimoneAllQuestioning)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun advanceToEndStep(d: GameTestDriver, me: EntityId) {
        d.passPriorityUntil(Step.END, maxPasses = 300)
        d.activePlayer shouldBe me
        d.currentStep shouldBe Step.END
    }

    /** Plays one Forest from hand this turn (records the entered-this-turn tracker). */
    fun playOneLand(d: GameTestDriver, me: EntityId) {
        val forest = d.putCardInHand(me, "Forest")
        d.playLand(me, forest)
    }

    fun primoCounters(d: GameTestDriver, me: EntityId): Int? {
        val primo = d.findPermanent(me, "Primo, the Indivisible") ?: return null
        return d.state.getEntity(primo)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    test("prime land count (5) with a land entered this turn: create Primo with five +1/+1 counters") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, ZimoneAllQuestioning.name)
        repeat(4) { d.putLandOnBattlefield(me, "Forest") } // 4 pre-placed (not "entered this turn")
        playOneLand(d, me)                                  // +1 played → 5 lands, entered this turn

        advanceToEndStep(d, me)
        d.bothPass() // resolve the end-step trigger

        primoCounters(d, me) shouldBe 5
        // 0/0 base + five +1/+1 counters = 5/5, and it is the only Primo (legendary, named token).
        val primo = d.findPermanent(me, "Primo, the Indivisible")!!
        d.state.projectedState.getPower(primo) shouldBe 5
        d.state.projectedState.getToughness(primo) shouldBe 5
    }

    test("non-prime land count (4): the prime gate fails, no token") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, ZimoneAllQuestioning.name)
        repeat(3) { d.putLandOnBattlefield(me, "Forest") }
        playOneLand(d, me) // 4 lands total, entered this turn

        advanceToEndStep(d, me)
        d.bothPass()

        d.findPermanent(me, "Primo, the Indivisible") shouldBe null
    }

    test("prime land count (5) but no land entered this turn: trigger does not fire") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, ZimoneAllQuestioning.name)
        repeat(5) { d.putLandOnBattlefield(me, "Forest") } // 5 lands, none "entered this turn"

        advanceToEndStep(d, me)
        d.bothPass()

        d.findPermanent(me, "Primo, the Indivisible") shouldBe null
    }

    test("1 land that entered this turn: 1 is not prime, so no token") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, ZimoneAllQuestioning.name)
        playOneLand(d, me) // exactly 1 land, entered this turn

        advanceToEndStep(d, me)
        d.bothPass()

        d.findPermanent(me, "Primo, the Indivisible") shouldBe null
    }

    test("2 lands (smallest prime) with a land entered: Primo with two +1/+1 counters") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, ZimoneAllQuestioning.name)
        d.putLandOnBattlefield(me, "Forest")
        playOneLand(d, me) // 2 lands total, entered this turn

        advanceToEndStep(d, me)
        d.bothPass()

        primoCounters(d, me) shouldBe 2
    }
})
