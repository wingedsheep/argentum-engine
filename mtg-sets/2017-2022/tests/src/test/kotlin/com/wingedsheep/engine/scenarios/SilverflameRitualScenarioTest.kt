package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.SilverflameRitual
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Silverflame Ritual — counters on each creature you control; Adamant (three white mana spent on
 * the whole cost, generic included) also grants those creatures vigilance until end of turn. The
 * Adamant check reads the resolving sorcery's own cast payment.
 */
class SilverflameRitualScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(SilverflameRitual))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("three white mana spent: counters and vigilance on creatures you control only") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val opp = driver.getOpponent(p)
        val lions = driver.putCreatureOnBattlefield(p, "Savannah Lions")
        val courser = driver.putCreatureOnBattlefield(p, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(opp, "Goblin Guide")
        val spell = driver.putCardInHand(p, "Silverflame Ritual")
        driver.giveMana(p, Color.WHITE, 3)
        driver.giveColorlessMana(p, 1)

        driver.castSpell(p, spell).outcome shouldBe Outcome.Done
        driver.bothPass()

        plusCounters(driver, lions) shouldBe 1
        plusCounters(driver, courser) shouldBe 1
        plusCounters(driver, theirs) shouldBe 0
        val projected = driver.state.projectedState
        projected.hasKeyword(lions, Keyword.VIGILANCE) shouldBe true
        projected.hasKeyword(courser, Keyword.VIGILANCE) shouldBe true
        projected.hasKeyword(theirs, Keyword.VIGILANCE) shouldBe false
    }

    test("only two white mana spent: counters but no vigilance") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val lions = driver.putCreatureOnBattlefield(p, "Savannah Lions")
        val spell = driver.putCardInHand(p, "Silverflame Ritual")
        driver.giveMana(p, Color.WHITE, 2)
        driver.giveColorlessMana(p, 2)

        driver.castSpell(p, spell).outcome shouldBe Outcome.Done
        driver.bothPass()

        plusCounters(driver, lions) shouldBe 1
        driver.state.projectedState.hasKeyword(lions, Keyword.VIGILANCE) shouldBe false
    }
})
