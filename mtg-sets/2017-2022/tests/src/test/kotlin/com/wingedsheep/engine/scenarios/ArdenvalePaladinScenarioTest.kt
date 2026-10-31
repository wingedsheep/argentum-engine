package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.ArdenvalePaladin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ardenvale Paladin — Adamant: "If at least three white mana was spent to cast this spell, this
 * creature enters with a +1/+1 counter on it." White mana spent on the generic part counts.
 */
class ArdenvalePaladinScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(ArdenvalePaladin))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("three white mana spent (two on generic): enters with a +1/+1 counter") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Ardenvale Paladin")
        driver.giveMana(p, Color.WHITE, 3)
        driver.giveColorlessMana(p, 1)

        driver.castSpell(p, spell).outcome shouldBe Outcome.Done
        driver.bothPass()

        plusCounters(driver, spell) shouldBe 1
    }

    test("only two white mana spent: enters with no counter") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val spell = driver.putCardInHand(p, "Ardenvale Paladin")
        driver.giveMana(p, Color.WHITE, 2)
        driver.giveColorlessMana(p, 2)

        driver.castSpell(p, spell).outcome shouldBe Outcome.Done
        driver.bothPass()

        plusCounters(driver, spell) shouldBe 0
    }
})
