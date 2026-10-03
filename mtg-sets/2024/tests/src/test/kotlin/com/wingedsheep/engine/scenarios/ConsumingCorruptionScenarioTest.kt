package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ConsumingCorruption
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Consuming Corruption — {B}{B} Instant
 *
 * "Consuming Corruption deals X damage to target creature or planeswalker and you gain X life,
 *  where X is the number of Swamps you control."
 */
class ConsumingCorruptionScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ConsumingCorruption)
        return driver
    }

    test("X counts only Swamps you control, for both damage and life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putLandOnBattlefield(me, "Swamp")
        driver.putLandOnBattlefield(me, "Swamp")
        driver.putLandOnBattlefield(me, "Mountain")
        // Opponent's Swamps don't count.
        driver.putLandOnBattlefield(opponent, "Swamp")
        driver.putLandOnBattlefield(opponent, "Swamp")
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Consuming Corruption")
        driver.giveMana(me, Color.BLACK, 2)
        driver.castSpell(me, spell, targets = listOf(bears))
        driver.bothPass()

        driver.getLifeTotal(me) shouldBe 22
        driver.getGraveyardCardNames(opponent) shouldContain "Grizzly Bears"
    }

    test("one Swamp deals 1 damage and gains 1 life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putLandOnBattlefield(me, "Swamp")
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Consuming Corruption")
        driver.giveMana(me, Color.BLACK, 2)
        driver.castSpell(me, spell, targets = listOf(bears))
        driver.bothPass()

        driver.getLifeTotal(me) shouldBe 21
        driver.getGraveyardCardNames(opponent) shouldNotContain "Grizzly Bears"
    }
})
