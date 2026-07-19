package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.isd.InnistradSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Fiend Hunter — {1}{W}{W} Creature — Human Cleric 1/3
 * When this creature enters, you may exile another target creature.
 * When this creature leaves the battlefield, return the exiled card under its owner's control.
 */
class FiendHunterScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + InnistradSet.cards)
        return d
    }

    test("exiles a creature on enter and returns it when Fiend Hunter dies") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = d.putCreatureOnBattlefield(p2, "Centaur Courser")

        val hunter = d.putCardInHand(p1, "Fiend Hunter")
        d.giveMana(p1, Color.WHITE, 3)
        d.castSpell(p1, hunter)
        d.bothPass() // resolve the creature; ETB trigger goes on the stack and asks for a target

        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(p1, listOf(victim))
        d.bothPass() // resolve the ETB exile trigger

        // Victim is exiled (under its owner p2's control zone), off the battlefield.
        d.getExile(p2) shouldContain victim
        d.state.getBattlefield(p2) shouldNotContain victim

        // Kill Fiend Hunter with a burn spell (3 damage to a 1/3).
        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, bolt, targets = listOf(hunter))
        d.bothPass() // resolve bolt -> Fiend Hunter dies -> LTB return trigger goes on stack
        d.bothPass() // resolve the LTB return trigger

        // Victim returns to the battlefield under its owner's control; exile is empty.
        d.state.getBattlefield(p2) shouldContain victim
        d.getExile(p2) shouldNotContain victim
    }

    test("may decline the exile when a legal target exists") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bystander = d.putCreatureOnBattlefield(p2, "Centaur Courser")

        val hunter = d.putCardInHand(p1, "Fiend Hunter")
        d.giveMana(p1, Color.WHITE, 3)
        d.castSpell(p1, hunter)
        d.bothPass()

        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        // Optional "you may" — choose no target.
        d.submitTargetSelection(p1, emptyList())
        d.bothPass()

        // Nothing was exiled; the bystander stays on the battlefield.
        d.getExile(p2) shouldNotContain bystander
        d.state.getBattlefield(p2) shouldContain bystander
    }
})
