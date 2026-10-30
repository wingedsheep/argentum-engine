package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.Flutterfox
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Flutterfox (ELD #12) — {1}{W} Creature — Fox 2/2.
 *
 * "As long as you control an artifact or enchantment, this creature has flying."
 */
class FlutterfoxScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Flutterfox))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("no flying without an artifact or enchantment; an opponent's enchantment doesn't count") {
        val driver = createDriver()
        val me = driver.player1
        val fox = driver.putCreatureOnBattlefield(me, "Flutterfox")
        driver.state.projectedState.hasKeyword(fox, Keyword.FLYING) shouldBe false

        driver.putPermanentOnBattlefield(driver.getOpponent(me), "Test Enchantment")
        driver.state.projectedState.hasKeyword(fox, Keyword.FLYING) shouldBe false
    }

    test("controlling an enchantment grants flying") {
        val driver = createDriver()
        val me = driver.player1
        val fox = driver.putCreatureOnBattlefield(me, "Flutterfox")
        driver.putPermanentOnBattlefield(me, "Test Enchantment")
        driver.state.projectedState.hasKeyword(fox, Keyword.FLYING) shouldBe true
    }

    test("controlling an artifact grants flying") {
        val driver = createDriver()
        val me = driver.player1
        val fox = driver.putCreatureOnBattlefield(me, "Flutterfox")
        driver.putCreatureOnBattlefield(me, "Artifact Creature")
        driver.state.projectedState.hasKeyword(fox, Keyword.FLYING) shouldBe true
    }
})
