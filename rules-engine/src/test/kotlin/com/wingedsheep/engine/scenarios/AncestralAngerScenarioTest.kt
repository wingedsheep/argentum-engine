package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.vow.cards.AncestralAnger
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ancestral Anger {R} Sorcery (VOW canonical, reprinted in SOS).
 *
 * Target creature gains trample and gets +X/+0 until end of turn, where X is 1 plus the
 * number of cards named Ancestral Anger in your graveyard. Draw a card.
 */
class AncestralAngerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AncestralAnger))
        return driver
    }

    test("X = 1 with empty graveyard: +1/+0, trample, draw a card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3

        driver.giveMana(me, Color.RED, 1)
        val spell = driver.putCardInHand(me, "Ancestral Anger")
        val handWithSpell = driver.getHandSize(me)
        driver.castSpell(me, spell, targets = listOf(bear)).error shouldBe null
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(bear) shouldBe 4 // 3 + (1 + 0 in graveyard)
        projected.getToughness(bear) shouldBe 3
        projected.hasKeyword(bear, com.wingedsheep.sdk.core.Keyword.TRAMPLE) shouldBe true
        // Spent the spell (-1) then drew a card (+1) => hand size unchanged from "with spell".
        driver.getHandSize(me) shouldBe handWithSpell
    }

    test("X scales with copies of Ancestral Anger already in the graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20, "Forest" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3

        // Seed two earlier copies into the graveyard.
        repeat(2) {
            val gy = driver.putCardInHand(me, "Ancestral Anger")
            driver.moveToGraveyard(gy)
        }

        driver.giveMana(me, Color.RED, 1)
        val spell = driver.putCardInHand(me, "Ancestral Anger")
        driver.castSpell(me, spell, targets = listOf(bear)).error shouldBe null
        driver.bothPass()

        // X = 1 + 2 copies in graveyard = 3 => 3/3 becomes 6/3.
        val projected = driver.state.projectedState
        projected.getPower(bear) shouldBe 6
        projected.getToughness(bear) shouldBe 3
    }
})
