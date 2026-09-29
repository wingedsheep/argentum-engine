package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.FeedTheInfection
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Feed the Infection (ONE #93) — {3}{B} Sorcery.
 *
 * "You draw three cards and you lose 3 life.
 *  Corrupted — Each opponent who has three or more poison counters loses 3 life."
 *
 * Proof card for `Conditions.PoisonCountersAtLeast` under a per-opponent rebind: in a three-player
 * game only the opponent at the threshold loses life, the one below it doesn't.
 */
class FeedTheInfectionScenarioTest : FunSpec({

    test("draws three, costs 3 life, and drains only the opponents with three or more poison") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FeedTheInfection))
        val players = driver.initMultiplayer(
            decks = List(3) { Deck.of("Swamp" to 40) },
            skipMulligans = true,
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (you, poisoned, clean) = players
        driver.addComponent(poisoned, CountersComponent(mapOf(CounterType.POISON to 3)))
        driver.addComponent(clean, CountersComponent(mapOf(CounterType.POISON to 2)))

        val spell = driver.putCardInHand(you, "Feed the Infection")
        driver.giveMana(you, Color.BLACK, 4)
        val handBefore = driver.getHandSize(you)
        driver.castSpell(you, spell).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.passPriority(driver.state.priorityPlayerId!!)

        driver.getHandSize(you) shouldBe handBefore - 1 + 3
        driver.getLifeTotal(you) shouldBe 17
        driver.getLifeTotal(poisoned) shouldBe 17
        driver.getLifeTotal(clean) shouldBe 20
    }

    test("in a two-player game the corrupted clause is inert below three poison") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FeedTheInfection))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.addComponent(driver.player2, CountersComponent(mapOf(CounterType.POISON to 2)))

        val spell = driver.putCardInHand(driver.player1, "Feed the Infection")
        driver.giveMana(driver.player1, Color.BLACK, 4)
        driver.castSpell(driver.player1, spell).error shouldBe null
        driver.bothPass()

        driver.getLifeTotal(driver.player1) shouldBe 17
        driver.getLifeTotal(driver.player2) shouldBe 20
    }
})
