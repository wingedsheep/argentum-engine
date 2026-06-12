package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.TakeTheFall
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Take the Fall (OTJ #73) — {U} Instant.
 *
 *   "Target creature gets -1/-0 until end of turn. It gets -4/-0 until end of turn instead if
 *    you control an outlaw. (Assassins, Mercenaries, Pirates, Rogues, and Warlocks are outlaws.)
 *    Draw a card."
 *
 * The outlaw check is a resolution-time state test choosing -4/-0 *instead of* -1/-0 (never
 * both); the draw is unconditional.
 */
class TakeTheFallScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        // Ragavan, Nimble Pilferer is a Monkey Pirate — Pirate makes it an outlaw.
        driver.registerCards(TestCards.all + listOf(TakeTheFall))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("with no outlaw, gives -1/-0 and draws a card") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val target = driver.putCreatureOnBattlefield(opp, "Centaur Courser") // 3/3
        val spell = driver.putCardInHand(me, "Take the Fall")
        driver.giveMana(me, Color.BLUE, 1)
        val handBefore = driver.getHandSize(me) - 1 // spell leaves hand on cast

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(target)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.getPower(target) shouldBe 2   // 3 - 1
        projected.getToughness(target) shouldBe 3 // unchanged
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("with an outlaw you control, gives -4/-0 instead (and still draws)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // I control an outlaw (Pirate).
        driver.putCreatureOnBattlefield(me, "Ragavan, Nimble Pilferer")
        val target = driver.putCreatureOnBattlefield(opp, "Centaur Courser") // 3/3
        val spell = driver.putCardInHand(me, "Take the Fall")
        driver.giveMana(me, Color.BLUE, 1)
        val handBefore = driver.getHandSize(me) - 1

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(target)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.getPower(target) shouldBe -1   // 3 - 4, clamped to >= 0 for combat but raw is -1
        projected.getToughness(target) shouldBe 3
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("the outlaw must be one you control — an opponent's outlaw doesn't upgrade it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // The outlaw belongs to the opponent — should NOT trigger the -4/-0 branch.
        driver.putCreatureOnBattlefield(opp, "Ragavan, Nimble Pilferer")
        val target = driver.putCreatureOnBattlefield(opp, "Centaur Courser") // 3/3
        val spell = driver.putCardInHand(me, "Take the Fall")
        driver.giveMana(me, Color.BLUE, 1)

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(target)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.getPower(target) shouldBe 2 // only -1/-0
    }
})
