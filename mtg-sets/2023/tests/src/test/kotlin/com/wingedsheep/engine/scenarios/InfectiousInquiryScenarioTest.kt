package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.InfectiousInquiry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Infectious Inquiry (ONE #97) — {2}{B} Sorcery.
 *
 * "You draw two cards and you lose 2 life. Each opponent gets a poison counter."
 */
class InfectiousInquiryScenarioTest : FunSpec({

    fun poisonOf(driver: GameTestDriver, player: EntityId): Int =
        driver.state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    test("draws two, costs 2 life, and gives every opponent a poison counter") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(InfectiousInquiry))
        val players = driver.initMultiplayer(
            decks = List(3) { Deck.of("Swamp" to 40) },
            skipMulligans = true,
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (you, oppA, oppB) = players
        driver.addComponent(oppB, CountersComponent(mapOf(CounterType.POISON to 2)))

        val spell = driver.putCardInHand(you, "Infectious Inquiry")
        driver.giveMana(you, Color.BLACK, 3)
        val handBefore = driver.getHandSize(you)
        driver.castSpell(you, spell).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.passPriority(driver.state.priorityPlayerId!!)

        driver.getHandSize(you) shouldBe handBefore - 1 + 2
        driver.getLifeTotal(you) shouldBe 18
        driver.getLifeTotal(oppA) shouldBe 20
        driver.getLifeTotal(oppB) shouldBe 20
        poisonOf(driver, you) shouldBe 0
        poisonOf(driver, oppA) shouldBe 1
        poisonOf(driver, oppB) shouldBe 3
    }
})
