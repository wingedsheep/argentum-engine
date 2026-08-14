package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.FarsightMask
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Farsight Mask (MRD #170).
 *
 * "Whenever a source an opponent controls deals damage to you, if this artifact is untapped,
 * you may draw a card."
 *
 * Covers the source-filtered damage-to-you trigger
 * ([com.wingedsheep.sdk.dsl.Triggers.damageDealtToYou]) — a shape that previously routed into the
 * general observer index, where `RecipientFilter.You` never matches, so the ability never fired.
 */
class FarsightMaskScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FarsightMask))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        return Triple(driver, active, driver.getOpponent(active))
    }

    /** Drain the stack: the spell resolves, then the damage trigger it caused resolves too. */
    fun GameTestDriver.resolveAll(max: Int = 10) {
        var i = 0
        while (state.stack.isNotEmpty() && pendingDecision == null && i++ < max) bothPass()
    }

    test("an opponent's burn spell offers the draw, and accepting draws a card") {
        // The Mask's controller is the non-active player, so the opponent (active) can bolt them.
        val (driver, opponent, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Farsight Mask")
        val handBefore = driver.getHandSize(you)

        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Player(you)))
        driver.resolveAll()

        driver.submitYesNo(you, true)

        driver.getLifeTotal(you) shouldBe 17
        driver.getHandSize(you) shouldBe handBefore + 1
    }

    test("declining the may draws nothing") {
        val (driver, opponent, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Farsight Mask")
        val handBefore = driver.getHandSize(you)

        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Player(you)))
        driver.resolveAll()

        driver.submitYesNo(you, false)

        driver.getHandSize(you) shouldBe handBefore
    }

    test("a tapped Mask never triggers — the intervening 'if' fails at trigger time") {
        val (driver, opponent, you) = newGame()
        val mask = driver.putPermanentOnBattlefield(you, "Farsight Mask")
        driver.tapPermanent(mask)
        val handBefore = driver.getHandSize(you)

        driver.giveMana(opponent, Color.RED, 1)
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.castSpellWithTargets(opponent, bolt, listOf(ChosenTarget.Player(you)))
        driver.resolveAll()

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(you) shouldBe 17
        driver.getHandSize(you) shouldBe handBefore
    }

    test("damage from a source you control doesn't trigger it") {
        // Here the Mask's controller is the active player, so they can bolt themselves — their own
        // source, which the "an opponent controls" filter excludes.
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Farsight Mask")
        val handBefore = driver.getHandSize(you)

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(you)))
        driver.resolveAll()

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(you) shouldBe 17
        driver.getHandSize(you) shouldBe handBefore
    }
})
