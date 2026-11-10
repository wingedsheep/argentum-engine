package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ogw.cards.BearerOfSilence
import com.wingedsheep.mtg.sets.definitions.ogw.cards.CultivatorDrone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Bearer of Silence — "When you cast this spell, you may pay {1}{C}. If you do, target opponent
 * sacrifices a creature." The trigger's {1}{C} is a cost paid as an effect resolves; Cultivator
 * Drone's ruling (2016-01-22) lets its mana pay it, and two Drones can pay the whole cost.
 */
class BearerOfSilenceScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BearerOfSilence, CultivatorDrone))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast Bearer from a floating {1}{B}, aim its cast trigger at [opponent], and resolve the trigger. */
    fun GameTestDriver.castBearerAndResolveTrigger(me: EntityId, opponent: EntityId) {
        val bearer = putCardInHand(me, "Bearer of Silence")
        giveMana(me, Color.BLACK, 1)
        giveColorlessMana(me, 1)
        submitSuccess(CastSpell(me, bearer, paymentStrategy = PaymentStrategy.FromPool))
        (pendingDecision as? ChooseTargetsDecision)?.let { submitTargetSelection(me, listOf(opponent)) }
        bothPass()
    }

    test("two Cultivator Drones pay the whole {1}{C} and the opponent sacrifices a creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val victim = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val drones = List(2) { driver.putCreatureOnBattlefield(me, "Cultivator Drone").also(driver::removeSummoningSickness) }

        driver.castBearerAndResolveTrigger(me, opponent)
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        val select = driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        select.availableSources.map { it.entityId } shouldContainExactlyInAnyOrder drones
        driver.submitManaAutoPayOrDecline(me, autoPay = true)

        drones.forEach { driver.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
        driver.getGraveyard(opponent) shouldContain victim
    }

    test("declining the payment leaves the opponent's creature alone") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val victim = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        repeat(2) { driver.removeSummoningSickness(driver.putCreatureOnBattlefield(me, "Cultivator Drone")) }

        driver.castBearerAndResolveTrigger(me, opponent)
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.getGraveyard(opponent) shouldNotContain victim
    }
})
