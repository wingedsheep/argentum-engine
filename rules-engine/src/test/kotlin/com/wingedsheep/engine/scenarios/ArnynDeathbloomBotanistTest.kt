package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.ArnynDeathbloomBotanist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Arnyn, Deathbloom Botanist {2}{B} — Legendary Creature — Vampire Druid 2/2
 *   Deathtouch
 *   Whenever a creature you control with power or toughness 1 or less dies,
 *   target opponent loses 2 life and you gain 2 life.
 *
 * Exercises the new [CardPredicate.PowerOrToughnessAtMost] OR cap on a dies trigger:
 *  - a 1-toughness creature dying triggers Arnyn (toughness 1 ≤ 1, even though power is 2),
 *  - a 2/2 creature dying does NOT trigger (neither power nor toughness ≤ 1).
 */
class ArnynDeathbloomBotanistTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ArnynDeathbloomBotanist))
        return driver
    }

    fun drainStack(driver: GameTestDriver, opponentToTarget: com.wingedsheep.sdk.model.EntityId) {
        var safety = 0
        while (driver.stackSize > 0 && safety < 30) {
            val pending = driver.state.pendingDecision
            if (pending != null) {
                // The trigger's "target opponent" choice — pick the lone opponent.
                driver.submitTargetSelection(pending.playerId, listOf(opponentToTarget))
            } else {
                driver.bothPass()
            }
            safety++
        }
    }

    test("creature with toughness 1 or less dying triggers the drain") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val youLifeBefore = driver.getLifeTotal(you)
        val oppLifeBefore = driver.getLifeTotal(opponent)

        driver.putCreatureOnBattlefield(you, "Arnyn, Deathbloom Botanist")
        // Goblin Guide is 2/1 → toughness 1 ≤ 1 qualifies (power is 2, proving the OR cap
        // matches on toughness alone).
        val frail = driver.putCreatureOnBattlefield(you, "Goblin Guide")

        // Opponent bolts the 1/1 you control.
        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.passPriority(you)
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Permanent(frail))).error shouldBe null

        drainStack(driver, opponent)

        // Drain resolved: opponent lost 2, you gained 2.
        driver.getLifeTotal(opponent) shouldBe (oppLifeBefore - 2)
        driver.getLifeTotal(you) shouldBe (youLifeBefore + 2)
    }

    test("creature with both power and toughness above 1 dying does NOT trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val youLifeBefore = driver.getLifeTotal(you)
        val oppLifeBefore = driver.getLifeTotal(opponent)

        driver.putCreatureOnBattlefield(you, "Arnyn, Deathbloom Botanist")
        val bears = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3

        // Kill the 2/2 with two bolts (3 damage > 2 toughness, one bolt suffices).
        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.passPriority(you)
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Permanent(bears))).error shouldBe null

        // Drain whatever is on the stack (just the bolt — no Arnyn trigger expected).
        var safety = 0
        while (driver.stackSize > 0 && driver.state.pendingDecision == null && safety < 20) {
            driver.bothPass(); safety++
        }

        // No drain happened.
        driver.getLifeTotal(opponent) shouldBe oppLifeBefore
        driver.getLifeTotal(you) shouldBe youLifeBefore
    }
})
