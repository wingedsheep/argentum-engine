package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.TheMasterOfLakeTown
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Master of Lake-town {1}{B}{B} — Legendary Creature — Human Advisor 3/2
 *   Deathtouch
 *   Whenever a player loses life, that player mills that many cards.
 *   When The Master of Lake-town dies, draw a card for each graveyard with seven or more cards.
 *
 * [com.wingedsheep.sdk.dsl.Triggers.AnyPlayerLosesLife] has no other card using it, so the first
 * two tests pin down that it fires for *any* player and mills the player who lost the life rather
 * than the controller. The last two pin the "each graveyard" count, which reads every player's
 * graveyard including the controller's own.
 */
class TheMasterOfLakeTownScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheMasterOfLakeTown))
        return driver
    }

    test("an opponent losing 3 life mills that opponent 3 cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "The Master of Lake-town")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(opponent)).isSuccess shouldBe true
        driver.bothPass() // Lightning Bolt: opponent 20 -> 17
        driver.bothPass() // the mill trigger

        driver.assertLifeTotal(opponent, 17)
        driver.getGraveyard(opponent).size shouldBe 3
    }

    test("the controller losing life mills the controller, not the opponent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "The Master of Lake-town")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(you)).isSuccess shouldBe true
        driver.bothPass() // Lightning Bolt: you 20 -> 17
        driver.bothPass() // the mill trigger

        driver.assertLifeTotal(you, 17)
        // Lightning Bolt itself is the 4th card in your graveyard; the mill added 3 more.
        driver.getGraveyard(you).size shouldBe 4
        driver.getGraveyard(opponent).size shouldBe 0
    }

    test("dying draws one card for each graveyard holding seven or more cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        repeat(7) { driver.putCardInGraveyard(you, "Swamp") }
        repeat(7) { driver.putCardInGraveyard(opponent, "Swamp") }

        driver.putCreatureOnBattlefield(you, "The Master of Lake-town")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        val handBefore = driver.getHandSize(you)
        driver.castSpell(you, bolt, listOf(driver.findPermanent(you, "The Master of Lake-town")!!))
            .isSuccess shouldBe true
        driver.bothPass() // 3 damage kills the 3/2
        driver.bothPass() // the dies trigger

        // Both graveyards are over seven, so the trigger draws two. The Bolt left hand to pay for it.
        driver.getHandSize(you) shouldBe handBefore - 1 + 2
    }

    test("dying draws nothing when no graveyard has seven cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "The Master of Lake-town")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        val handBefore = driver.getHandSize(you)
        driver.castSpell(you, bolt, listOf(driver.findPermanent(you, "The Master of Lake-town")!!))
            .isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass()

        // Your graveyard holds only the Bolt and the Master — two cards, so no draws.
        driver.getHandSize(you) shouldBe handBefore - 1
    }
})
