package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Vizier of Remedies (AKH #38) — {1}{W} 2/1.
 *
 * "If one or more -1/-1 counters would be put on a creature you control, that many -1/-1 counters
 *  minus one are put on it instead."
 *
 * The first static card on a *negative* [com.wingedsheep.sdk.scripting.ModifyCounterPlacement]:
 * three counters become two, one becomes none, a second Vizier subtracts another, and an
 * opponent's creature is untouched.
 */
class VizierOfRemediesScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.minusCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0

    /** Cast Channeler Initiate and point its "put three -1/-1 counters" trigger at itself. */
    fun GameTestDriver.castChannelerOnItself(caster: EntityId): EntityId {
        val channeler = putCardInHand(caster, "Channeler Initiate")
        giveMana(caster, Color.GREEN); giveColorlessMana(caster, 1)
        castSpell(caster, channeler).error shouldBe null
        bothPass().error shouldBe null // resolve the creature spell; the enters trigger wants a target
        submitTargetSelection(caster, listOf(channeler)).error shouldBe null
        bothPass().error shouldBe null // resolve the enters trigger
        return channeler
    }

    test("three -1/-1 counters on a creature you control become two") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Vizier of Remedies")

        val channeler = d.castChannelerOnItself(me)
        d.minusCounters(channeler) shouldBe 2
        d.state.projectedState.getPower(channeler) shouldBe 1
        d.state.projectedState.getToughness(channeler) shouldBe 2
    }

    test("an opponent's Vizier doesn't reduce counters on your creatures") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(d.getOpponent(me), "Vizier of Remedies")

        val channeler = d.castChannelerOnItself(me)
        d.minusCounters(channeler) shouldBe 3
    }

    test("each additional Vizier subtracts one more") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Vizier of Remedies")
        d.putCreatureOnBattlefield(me, "Vizier of Remedies")

        val channeler = d.castChannelerOnItself(me)
        d.minusCounters(channeler) shouldBe 1
    }

    test("a single -1/-1 counter paid as a cost is replaced to nothing") {
        val d = driver()
        val me = d.player1
        d.putCreatureOnBattlefield(me, "Vizier of Remedies")
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val sting = d.putCardInHand(me, "Lethal Sting")
        d.giveMana(me, Color.BLACK, 3)

        d.submit(
            CastSpell(
                playerId = me,
                cardId = sting,
                targets = listOf(ChosenTarget.Permanent(giant)),
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(counterPlacementTargets = listOf(bear)),
            )
        ).error shouldBe null
        d.minusCounters(bear) shouldBe 0
        d.bothPass()

        (giant in d.state.getBattlefield()) shouldBe false
        d.state.projectedState.getPower(bear) shouldBe 2
    }
})
