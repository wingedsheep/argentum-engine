package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.ForgottenRealmsSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dragon Turtle — {1}{U}{U} Creature — Dragon Turtle 3/5
 * Flash
 * Drag Below — When this creature enters, tap it and up to one target creature an opponent
 * controls. They don't untap during their controllers' next untap steps.
 */
class DragonTurtleScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ForgottenRealmsSet.cards)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("taps itself and the target; each stays tapped through its own controller's next untap step") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)

        val victim = d.putCreatureOnBattlefield(p2, "Centaur Courser")
        val turtle = d.putCardInHand(p1, "Dragon Turtle")
        d.giveMana(p1, Color.BLUE, 3)
        d.castSpell(p1, turtle).error shouldBe null
        d.bothPass()

        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(p1, listOf(victim)).error shouldBe null
        d.bothPass()

        d.isTapped(turtle) shouldBe true
        d.isTapped(victim) shouldBe true

        // p2's untap step: the victim stays tapped.
        d.passPriorityUntil(Step.UPKEEP, p2)
        d.isTapped(victim) shouldBe true
        d.isTapped(turtle) shouldBe true

        // p1's untap step: Dragon Turtle stays tapped.
        d.passPriorityUntil(Step.UPKEEP, p1)
        d.isTapped(turtle) shouldBe true
    }

    test("with no target chosen the ability still taps and locks Dragon Turtle") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)

        d.putCreatureOnBattlefield(p2, "Centaur Courser")
        val turtle = d.putCardInHand(p1, "Dragon Turtle")
        d.giveMana(p1, Color.BLUE, 3)
        d.castSpell(p1, turtle).error shouldBe null
        d.bothPass()

        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(p1, emptyList()).error shouldBe null
        d.bothPass()

        d.isTapped(turtle) shouldBe true

        d.passPriorityUntil(Step.UPKEEP, p1)
        d.isTapped(turtle) shouldBe true
    }

    test("if the chosen target becomes illegal the whole ability fizzles and Dragon Turtle stays untapped") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)

        val victim = d.putCreatureOnBattlefield(p2, "Centaur Courser")
        val turtle = d.putCardInHand(p1, "Dragon Turtle")
        d.giveMana(p1, Color.BLUE, 3)
        d.castSpell(p1, turtle).error shouldBe null
        d.bothPass()

        d.submitTargetSelection(p1, listOf(victim)).error shouldBe null
        d.moveToGraveyard(victim)
        d.bothPass()

        d.isTapped(turtle) shouldBe false
    }
})
