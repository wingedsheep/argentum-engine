package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.Skulduggery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Skulduggery (XLN #123 / OTJ #107) — {B} Instant.
 *
 *   "Until end of turn, target creature you control gets +1/+1 and target creature an opponent
 *    controls gets -1/-1."
 *
 * Two distinct targets: one creature you control (buffed), one an opponent controls (debuffed).
 */
class SkulduggeryScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Skulduggery))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("buffs your creature +1/+1 and debuffs the opponent's -1/-1") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val mine = driver.putCreatureOnBattlefield(me, "Savannah Lions")    // 1/1 (test card)
        val theirs = driver.putCreatureOnBattlefield(opp, "Centaur Courser") // 3/3
        val spell = driver.putCardInHand(me, "Skulduggery")
        driver.giveMana(me, Color.BLACK, 1)

        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        (result.error == null) shouldBe true
        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.getPower(mine) shouldBe 2   // 1 + 1
        projected.getToughness(mine) shouldBe 2 // 1 + 1
        projected.getPower(theirs) shouldBe 2  // 3 - 1
        projected.getToughness(theirs) shouldBe 2 // 3 - 1
    }

    test("the -1/-1 can kill a 1-toughness opposing creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val mine = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.putCreatureOnBattlefield(opp, "Savannah Lions") // 2/1
        val theirs = driver.getCreatures(opp).first()
        val spell = driver.putCardInHand(me, "Skulduggery")
        driver.giveMana(me, Color.BLACK, 1)

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        // 2/1 with -1/-1 = 1/0, dies as a state-based action.
        driver.assertInGraveyard(opp, "Savannah Lions")
    }

    test("the buff and debuff wear off at end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val mine = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val theirs = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val spell = driver.putCardInHand(me, "Skulduggery")
        driver.giveMana(me, Color.BLACK, 1)

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val projected = projector.project(driver.state)
        projected.getPower(mine) shouldBe 1
        projected.getToughness(mine) shouldBe 1
        projected.getPower(theirs) shouldBe 3
        projected.getToughness(theirs) shouldBe 3
    }
})
