package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rumbling Ruin — {5}{R} 6/6. "When this creature enters, count the number of +1/+1 counters on
 * creatures you control. Creatures your opponents control with power less than or equal to that
 * number can't block this turn."
 *
 * The count is frozen when the trigger resolves; which creatures fall under it stays live.
 */
class RumblingRuinScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.setCounters(id: EntityId, n: Int) =
        replaceState(state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to n))) })

    fun GameTestDriver.castRuin() {
        val me = activePlayer!!
        giveMana(me, Color.RED, 6)
        castSpell(me, putCardInHand(me, "Rumbling Ruin"))
        bothPass() // resolve the creature spell
        bothPass() // resolve the enters trigger
        state.stack.isEmpty() shouldBe true
    }

    test("opponents' creatures with power at most the counter total can't block") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.setCounters(bears, 2)
        val small = d.putCreatureOnBattlefield(opp, "Grizzly Bears") // 2/2
        val big = d.putCreatureOnBattlefield(opp, "Hill Giant") // 3/3
        val mine = d.putCreatureOnBattlefield(me, "Savannah Lions") // 2/1, mine

        d.castRuin()

        d.state.projectedState.cantBlock(small) shouldBe true
        d.state.projectedState.cantBlock(big) shouldBe false
        d.state.projectedState.cantBlock(mine) shouldBe false
    }

    test("the number is fixed at resolution, but the affected set follows power") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.setCounters(bears, 2)
        val small = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val big = d.putCreatureOnBattlefield(opp, "Hill Giant")

        d.castRuin()

        // More counters later this turn don't raise the cap.
        d.setCounters(bears, 5)
        d.state.projectedState.cantBlock(big) shouldBe false
        // A creature that later has power at most 2 is caught, and one pumped above it escapes.
        d.setCounters(small, 1) // 3/3 now
        d.state.projectedState.cantBlock(small) shouldBe false
        val newcomer = d.putCreatureOnBattlefield(opp, "Savannah Lions")
        d.state.projectedState.cantBlock(newcomer) shouldBe true
    }

    test("with no counters, only creatures with power 0 or less can't block") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.castRuin()

        d.state.projectedState.cantBlock(bears) shouldBe false
    }
})
