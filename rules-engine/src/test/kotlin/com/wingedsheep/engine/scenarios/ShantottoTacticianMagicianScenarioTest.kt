package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.ShantottoTacticianMagician
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Shantotto, Tactician Magician (FIN #241).
 *
 * "{1}{U}{R} Legendary Creature — Dwarf Wizard 0/4. Whenever you cast a noncreature spell, Shantotto
 *  gets +X/+0 until end of turn, where X is the amount of mana spent to cast that spell. If X is 4 or
 *  more, draw a card."
 *
 * Verifies the +X/+0 scales by the mana actually spent on the *triggering* noncreature spell, and
 * that the conditional card draw fires only when 4+ mana was spent. Creature spells don't trigger it.
 */
class ShantottoTacticianMagicianScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(ShantottoTacticianMagician))
        return driver
    }

    fun startTurn(driver: GameTestDriver): EntityId {
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver.activePlayer!!
    }

    fun projectedPower(driver: GameTestDriver, id: EntityId): Int =
        StateProjector().getProjectedPower(driver.state, id)

    test("a 1-mana noncreature spell pumps Shantotto +1/+0 and does not draw") {
        val driver = createDriver()
        val p = startTurn(driver)
        val shantotto = driver.putCreatureOnBattlefield(p, "Shantotto, Tactician Magician") // 0/4
        val opp = driver.getOpponent(p)
        val victim = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        val handBefore = driver.getHandSize(p)
        val bolt = driver.putCardInHand(p, "Lightning Bolt") // {R} → 1 mana
        driver.giveMana(p, Color.RED, 1)

        driver.castSpell(p, bolt, targets = listOf(victim)).isSuccess shouldBe true
        driver.bothPass() // Bolt resolves
        driver.bothPass() // Shantotto cast-trigger resolves

        // 1 mana spent → +1/+0. Shantotto 0/4 → 1/4.
        projectedPower(driver, shantotto) shouldBe 1
        // X < 4 → no draw (hand is back to its pre-Bolt size, Bolt having left the hand).
        driver.getHandSize(p) shouldBe handBefore
    }

    test("a 4-mana noncreature spell pumps Shantotto +4/+0 and draws a card") {
        val driver = createDriver()
        val p = startTurn(driver)
        val shantotto = driver.putCreatureOnBattlefield(p, "Shantotto, Tactician Magician") // 0/4
        val opp = driver.getOpponent(p)
        val victim = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        // Stoke the Flames {2}{R}{R}; pay entirely with mana so exactly 4 mana is spent.
        val stoke = driver.putCardInHand(p, "Stoke the Flames")
        val handBefore = driver.getHandSize(p) // counts Stoke still in hand
        driver.giveMana(p, Color.RED, 4)

        driver.castSpell(p, stoke, targets = listOf(victim)).isSuccess shouldBe true
        driver.bothPass() // Stoke resolves
        driver.bothPass() // Shantotto cast-trigger resolves

        // 4 mana spent → +4/+0. Shantotto 0/4 → 4/4.
        projectedPower(driver, shantotto) shouldBe 4
        // X >= 4 → draw a card. Stoke left the hand (−1) and the draw added one (+1) → net unchanged.
        driver.getHandSize(p) shouldBe handBefore
    }

    test("casting a creature spell does not trigger Shantotto") {
        val driver = createDriver()
        val p = startTurn(driver)
        val shantotto = driver.putCreatureOnBattlefield(p, "Shantotto, Tactician Magician") // 0/4

        val courser = driver.putCardInHand(p, "Centaur Courser") // a creature spell, {2}{G}
        driver.giveMana(p, Color.GREEN, 3)

        driver.castSpell(p, courser).isSuccess shouldBe true
        driver.bothPass() // Courser resolves
        driver.bothPass()

        // No noncreature spell cast → Shantotto stays 0 power.
        projectedPower(driver, shantotto) shouldBe 0
    }
})
