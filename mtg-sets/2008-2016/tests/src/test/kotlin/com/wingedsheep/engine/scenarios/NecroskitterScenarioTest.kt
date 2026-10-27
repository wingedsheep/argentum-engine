package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eve.cards.Necroskitter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Necroskitter: Wither. Whenever a creature an opponent controls with a -1/-1 counter on it dies,
 * you may return that card to the battlefield under your control.
 *
 * Pins the last-known controller + counter check on the dying creature, the optional return under
 * your control (without its counters), and that a counterless creature doesn't trigger it.
 */
class NecroskitterScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Necroskitter)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.addMinusCounter(entityId: EntityId) {
        replaceState(state.updateEntity(entityId) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.MINUS_ONE_MINUS_ONE, 1))
        })
    }

    fun GameTestDriver.boltOpposingCourser(me: EntityId, courser: EntityId) {
        val bolt = putCardInHand(me, "Lightning Bolt")
        giveMana(me, Color.RED, 1)
        castSpell(me, bolt, listOf(courser))
        bothPass() // resolve Lightning Bolt; the creature dies
    }

    test("an opposing creature with a -1/-1 counter dies and returns under your control") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Necroskitter")
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.addMinusCounter(courser)

        driver.boltOpposingCourser(me, courser)
        driver.bothPass() // resolve Necroskitter's trigger
        driver.submitYesNo(me, true)

        val returned = driver.findPermanent(me, "Centaur Courser")
        returned shouldNotBe null
        driver.getGraveyardCardNames(opponent).contains("Centaur Courser") shouldBe false
        (driver.state.getEntity(returned!!)?.get<CountersComponent>()
            ?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0) shouldBe 0
    }

    test("declining leaves the card in its owner's graveyard") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Necroskitter")
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.addMinusCounter(courser)

        driver.boltOpposingCourser(me, courser)
        driver.bothPass()
        driver.submitYesNo(me, false)

        driver.findPermanent(me, "Centaur Courser") shouldBe null
        driver.getGraveyardCardNames(opponent).contains("Centaur Courser") shouldBe true
    }

    test("an opposing creature without a -1/-1 counter doesn't trigger it") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Necroskitter")
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.boltOpposingCourser(me, courser)

        driver.pendingDecision shouldBe null
        driver.state.stack.isEmpty() shouldBe true
        driver.getGraveyardCardNames(opponent).contains("Centaur Courser") shouldBe true
    }
})
