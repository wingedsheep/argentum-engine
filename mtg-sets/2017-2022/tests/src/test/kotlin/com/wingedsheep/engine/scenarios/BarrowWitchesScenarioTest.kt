package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.BarrowWitches
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * Barrow Witches (ELD #77) — {4}{B} Creature — Human Warlock 3/4.
 *
 * "When this creature enters, return target Knight card from your graveyard to your hand."
 */
class BarrowWitchesScenarioTest : FunSpec({

    val testKnight = card("Test Knight") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BarrowWitches, testKnight))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver
    }

    fun castWitches(driver: GameTestDriver, me: com.wingedsheep.sdk.model.EntityId, target: com.wingedsheep.sdk.model.EntityId?) {
        val witches = driver.putCardInHand(me, "Barrow Witches")
        driver.giveMana(me, Color.BLACK, 5)
        driver.castSpell(me, witches)
        driver.bothPass() // resolve the creature -> ETB trigger
        if (target != null && driver.pendingDecision != null) driver.submitTargetSelection(me, listOf(target))
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
    }

    test("ETB returns a Knight card from your graveyard to your hand") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val knight = driver.putCardInGraveyard(me, "Test Knight")

        castWitches(driver, me, knight)

        driver.getHand(me) shouldContain knight
        driver.getGraveyard(me) shouldNotContain knight
    }

    test("a non-Knight creature card in the graveyard isn't a legal target") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")

        castWitches(driver, me, null)

        driver.getGraveyard(me) shouldContain bears
        driver.getHand(me) shouldNotContain bears
    }
})
