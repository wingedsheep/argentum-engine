package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.CodespellCleric
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Codespell Cleric — {W} Creature — Human Cleric 1/1 (KHM).
 *
 *   Vigilance
 *   When this creature enters, if it was the second spell you cast this turn, put a +1/+1 counter
 *   on target creature.
 *
 * The intervening "if" reads the Cleric's own position in its controller's cast history, so these
 * tests pin the moments a resolution-time count would get wrong.
 */
class CodespellClericScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CodespellCleric))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("cast as your second spell, it puts a +1/+1 counter on target creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val lions = driver.putCardInHand(me, "Savannah Lions")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, lions).error shouldBe null
        driver.bothPass()
        val lionsOnField = driver.findPermanent(me, "Savannah Lions").shouldNotBeNull()

        val cleric = driver.putCardInHand(me, "Codespell Cleric")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, cleric).error shouldBe null
        driver.bothPass() // the Cleric resolves; its enters trigger asks for a target

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(lionsOnField))
        driver.bothPass() // the trigger resolves

        driver.state.projectedState.getPower(lionsOnField) shouldBe 2
        driver.state.projectedState.getToughness(lionsOnField) shouldBe 2
    }

    test("cast as your first spell, it doesn't trigger") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Savannah Lions")
        val cleric = driver.putCardInHand(me, "Codespell Cleric")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, cleric).error shouldBe null
        driver.bothPass()

        driver.findPermanent(me, "Codespell Cleric").shouldNotBeNull()
        driver.pendingDecision.shouldBeNull()
        driver.state.stack.size shouldBe 0
    }

    test("a spell cast in response doesn't stop the second-cast Cleric from triggering") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val lions = driver.putCardInHand(me, "Savannah Lions")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, lions).error shouldBe null
        driver.bothPass()
        val lionsOnField = driver.findPermanent(me, "Savannah Lions").shouldNotBeNull()

        val cleric = driver.putCardInHand(me, "Codespell Cleric")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, cleric).error shouldBe null
        // Third spell, cast while the Cleric is still on the stack: the total is now 3, but the
        // Cleric was still the second spell cast.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(opponent)).error shouldBe null
        driver.bothPass() // Bolt resolves
        driver.bothPass() // the Cleric resolves

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(lionsOnField))
        driver.bothPass()

        driver.state.projectedState.getPower(lionsOnField) shouldBe 2
    }

    test("cast as your third spell, it doesn't trigger") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        repeat(2) {
            val lions = driver.putCardInHand(me, "Savannah Lions")
            driver.giveMana(me, Color.WHITE, 1)
            driver.castSpell(me, lions).error shouldBe null
            driver.bothPass()
        }

        val cleric = driver.putCardInHand(me, "Codespell Cleric")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, cleric).error shouldBe null
        driver.bothPass()

        driver.findPermanent(me, "Codespell Cleric").shouldNotBeNull()
        driver.pendingDecision.shouldBeNull()
        driver.state.stack.size shouldBe 0
    }

    test("put onto the battlefield without being cast, it doesn't trigger even after one spell") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val lions = driver.putCardInHand(me, "Savannah Lions")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, lions).error shouldBe null
        driver.bothPass()

        driver.putPermanentOnBattlefield(me, "Codespell Cleric")
        driver.pendingDecision.shouldBeNull()
        driver.state.stack.size shouldBe 0
    }
})
