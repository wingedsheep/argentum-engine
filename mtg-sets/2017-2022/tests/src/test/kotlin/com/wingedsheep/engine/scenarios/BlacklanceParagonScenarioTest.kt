package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.BlacklanceParagon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Blacklance Paragon (ELD #79) — {1}{B} Creature — Human Knight 3/1.
 *
 * "Flash. When this creature enters, target Knight gains deathtouch and lifelink until end of turn."
 */
class BlacklanceParagonScenarioTest : FunSpec({

    val testKnight = card("Test Knight") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BlacklanceParagon, testKnight))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver
    }

    test("flashed in on the opponent's turn, the ETB gives a Knight deathtouch and lifelink") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        val me = driver.getOpponent(active)
        val knight = driver.putCreatureOnBattlefield(me, "Test Knight")

        val paragon = driver.putCardInHand(me, "Blacklance Paragon")
        driver.giveMana(me, Color.BLACK, 2)
        driver.passPriority(active)
        driver.castSpell(me, paragon)
        driver.bothPass() // resolve the creature -> ETB trigger
        if (driver.pendingDecision != null) driver.submitTargetSelection(me, listOf(knight))
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        val projected = driver.state.projectedState
        projected.hasKeyword(knight, Keyword.DEATHTOUCH) shouldBe true
        projected.hasKeyword(knight, Keyword.LIFELINK) shouldBe true
        val paragonId = driver.findPermanent(me, "Blacklance Paragon")!!
        projected.hasKeyword(paragonId, Keyword.DEATHTOUCH) shouldBe false
    }
})
