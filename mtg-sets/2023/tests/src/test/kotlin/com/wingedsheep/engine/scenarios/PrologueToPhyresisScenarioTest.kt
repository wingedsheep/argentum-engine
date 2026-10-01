package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.PrologueToPhyresis
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Prologue to Phyresis (ONE #65) — {1}{U} Instant.
 *
 * "Each opponent gets a poison counter. Draw a card."
 */
class PrologueToPhyresisScenarioTest : FunSpec({

    fun poisonOf(driver: GameTestDriver, player: EntityId): Int =
        driver.state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    test("gives every opponent a poison counter and the caster draws a card") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PrologueToPhyresis))
        val players = driver.initMultiplayer(
            decks = List(3) { Deck.of("Island" to 40) },
            skipMulligans = true,
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (you, oppA, oppB) = players
        driver.addComponent(oppB, CountersComponent(mapOf(CounterType.POISON to 2)))

        val spell = driver.putCardInHand(you, "Prologue to Phyresis")
        driver.giveMana(you, Color.BLUE, 2)
        val handBefore = driver.getHandSize(you)
        val oppAHandBefore = driver.getHandSize(oppA)
        driver.castSpell(you, spell).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.passPriority(driver.state.priorityPlayerId!!)

        driver.getHandSize(you) shouldBe handBefore - 1 + 1
        driver.getHandSize(oppA) shouldBe oppAHandBefore
        poisonOf(driver, you) shouldBe 0
        poisonOf(driver, oppA) shouldBe 1
        poisonOf(driver, oppB) shouldBe 3
    }
})
