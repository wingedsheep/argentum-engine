package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh1.cards.GoodFortuneUnicorn
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Good-Fortune Unicorn — {1}{G}{W} 2/2 Creature — Unicorn
 *
 * "Whenever another creature you control enters, put a +1/+1 counter on that creature."
 *
 * Covers the two things the wording pins down: the Unicorn's own entry must not trigger it
 * ("another"), and the counter goes on the creature that entered ("that creature"), not on
 * the Unicorn.
 */
class GoodFortuneUnicornScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GoodFortuneUnicorn))
        return driver
    }

    fun plusCounters(driver: GameTestDriver, entity: EntityId): Int =
        driver.state.getEntity(entity)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("Good-Fortune Unicorn does not put a +1/+1 counter on itself when it enters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val unicornCard = driver.putCardInHand(me, "Good-Fortune Unicorn")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, unicornCard)
        driver.bothPass() // resolve the Unicorn
        driver.passPriorityUntil(Step.END)

        val unicorn = driver.findPermanent(me, "Good-Fortune Unicorn")!!
        plusCounters(driver, unicorn) shouldBe 0
        driver.state.projectedState.getPower(unicorn) shouldBe 2
        driver.state.projectedState.getToughness(unicorn) shouldBe 2
    }

    test("another creature you control entering gets the counter, not the Unicorn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(me, "Good-Fortune Unicorn")

        // Savannah Lions (1/1) enters -> Unicorn trigger puts a counter on the Lions.
        val lionsCard = driver.putCardInHand(me, "Savannah Lions")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, lionsCard)
        driver.bothPass() // resolve Savannah Lions -> trigger goes on the stack
        driver.bothPass() // resolve the trigger

        val lions = driver.findPermanent(me, "Savannah Lions")!!
        val unicorn = driver.findPermanent(me, "Good-Fortune Unicorn")!!

        plusCounters(driver, lions) shouldBe 1
        driver.state.projectedState.getPower(lions) shouldBe 2
        driver.state.projectedState.getToughness(lions) shouldBe 2

        plusCounters(driver, unicorn) shouldBe 0
        driver.state.projectedState.getPower(unicorn) shouldBe 2
    }

    test("a creature an opponent controls entering does not trigger the Unicorn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(me, "Good-Fortune Unicorn")
        driver.passPriorityUntil(Step.END)

        // Opponent casts Savannah Lions at instant speed is illegal; hand them the creature
        // during their own main phase instead.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val lionsCard = driver.putCardInHand(opponent, "Savannah Lions")
        driver.giveMana(opponent, Color.WHITE, 1)
        driver.castSpell(opponent, lionsCard)
        driver.bothPass()

        val lions = driver.findPermanent(opponent, "Savannah Lions")!!
        val unicorn = driver.findPermanent(me, "Good-Fortune Unicorn")!!

        plusCounters(driver, lions) shouldBe 0
        plusCounters(driver, unicorn) shouldBe 0
    }
})
