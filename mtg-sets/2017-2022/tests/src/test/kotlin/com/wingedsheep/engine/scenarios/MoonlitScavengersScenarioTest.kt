package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.MoonlitScavengers
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * Moonlit Scavengers (ELD #57) — {5}{U} Creature — Merfolk Rogue 4/5.
 *
 * "When this creature enters, if you control an artifact or enchantment, return target creature
 *  an opponent controls to its owner's hand."
 */
class MoonlitScavengersScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MoonlitScavengers))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        return driver
    }

    fun castScavengers(driver: GameTestDriver, me: EntityId, target: EntityId) {
        val scavengers = driver.putCardInHand(me, "Moonlit Scavengers")
        driver.giveMana(me, Color.BLUE, 6)
        driver.castSpell(me, scavengers)
        driver.bothPass() // resolve the creature -> ETB trigger (if its condition holds)
        if (driver.pendingDecision != null) driver.submitTargetSelection(me, listOf(target))
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
    }

    test("with an artifact, the ETB bounces an opponent's creature") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Legend Rule Waiver")
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        castScavengers(driver, me, bears)

        driver.getCreatures(opponent) shouldNotContain bears
        driver.getHand(opponent).map { driver.getCardName(it) } shouldContain "Grizzly Bears"
    }

    test("without an artifact or enchantment, nothing is returned") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        castScavengers(driver, me, bears)

        driver.getCreatures(opponent) shouldContain bears
    }
})
