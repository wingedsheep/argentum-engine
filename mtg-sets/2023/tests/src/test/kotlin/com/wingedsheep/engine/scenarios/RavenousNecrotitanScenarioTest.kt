package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.RavenousNecrotitan
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ravenous Necrotitan (ONE #106) — "Corrupted — When this creature enters, sacrifice a creature
 * unless an opponent has three or more poison counters."
 */
class RavenousNecrotitanScenarioTest : FunSpec({

    fun castNecrotitan(poison: Int): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RavenousNecrotitan))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        if (poison > 0) {
            driver.addComponent(driver.player2, CountersComponent(mapOf(CounterType.POISON to poison)))
        }
        val spell = driver.putCardInHand(driver.player1, "Ravenous Necrotitan")
        driver.giveMana(driver.player1, Color.BLACK, 4)
        driver.castSpell(driver.player1, spell).error shouldBe null
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve the enters trigger
        return driver
    }

    test("without corrupted, the only creature it can sacrifice is itself") {
        val driver = castNecrotitan(poison = 2)
        driver.findPermanent(driver.player1, "Ravenous Necrotitan") shouldBe null
        driver.getGraveyardCardNames(driver.player1) shouldContain "Ravenous Necrotitan"
    }

    test("with an opponent at three poison, nothing is sacrificed") {
        val driver = castNecrotitan(poison = 3)
        driver.findPermanent(driver.player1, "Ravenous Necrotitan") shouldNotBe null
        driver.state.stack.isEmpty() shouldBe true
    }
})
