package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.NimbleObstructionist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Nimble Obstructionist — "When you cycle this card, counter target activated or triggered ability
 * you don't control." The self-cycled trigger targets a stack ability filtered by
 * `Not(ControlledByYou)`: an opponent's trigger is a legal target, the cycler's own trigger is not,
 * and the cycling draw happens regardless (ruling 2022-12-08).
 */
class NimbleObstructionistScenarioTest : FunSpec({

    val LifeGainCreature = card("Obstructionist Test Lifegainer") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(3)
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(NimbleObstructionist, LifeGainCreature))
        d.initMirrorMatch(deck = Deck.of("Island" to 20, "Forest" to 20), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    // The engine's cycling is "discard, then draw" at once (no cycling ability on the stack), so the
    // only stack object the self-cycle trigger could see is whatever was already there.
    test("cycling it counters an opponent's triggered ability, and the cycler still draws") {
        val d = driver()
        val opponent = d.activePlayer!!
        val you = d.getOpponent(opponent)

        val creature = d.putCardInHand(opponent, "Obstructionist Test Lifegainer")
        d.giveMana(opponent, Color.GREEN, 2)
        d.castSpell(opponent, creature)
        d.bothPass()
        d.stackSize shouldBe 1
        val lifeTrigger = d.getTopOfStack()!!

        val obstructionist = d.putCardInHand(you, "Nimble Obstructionist")
        d.giveMana(you, Color.BLUE, 3)
        d.passPriority(opponent)
        val handBefore = d.getHandSize(you)

        d.submit(CycleCard(you, obstructionist)).error shouldBe null
        d.getHandSize(you) shouldBe handBefore

        val decision = d.pendingDecision
        if (decision is ChooseTargetsDecision) {
            decision.legalTargets[0].orEmpty() shouldContain lifeTrigger
            d.submitTargetSelection(you, listOf(lifeTrigger)).error shouldBe null
        }
        d.stackSize shouldBe 2

        // Resolve the counter trigger: the opponent's life trigger leaves the stack unresolved.
        d.bothPass()

        d.stackSize shouldBe 0
        d.getLifeTotal(opponent) shouldBe 20
    }

    test("your own triggered ability is not a legal target, so the trigger is removed and yours resolves") {
        val d = driver()
        val you = d.activePlayer!!

        val creature = d.putCardInHand(you, "Obstructionist Test Lifegainer")
        d.giveMana(you, Color.GREEN, 2)
        d.castSpell(you, creature)
        d.bothPass()
        d.stackSize shouldBe 1

        val obstructionist = d.putCardInHand(you, "Nimble Obstructionist")
        d.giveMana(you, Color.BLUE, 3)
        val handBefore = d.getHandSize(you)

        d.submit(CycleCard(you, obstructionist)).error shouldBe null
        (d.pendingDecision is ChooseTargetsDecision) shouldBe false
        d.stackSize shouldBe 1
        d.getHandSize(you) shouldBe handBefore

        d.bothPass()
        d.stackSize shouldBe 0
        d.getLifeTotal(you) shouldBe 23
    }
})
