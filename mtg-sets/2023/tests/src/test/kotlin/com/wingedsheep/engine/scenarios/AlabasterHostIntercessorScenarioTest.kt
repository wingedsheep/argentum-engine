package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.MarchOfTheMachineSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Alabaster Host Intercessor — {5}{W} Creature — Phyrexian Samurai 3/4
 * When this creature enters, exile target creature an opponent controls until this creature
 * leaves the battlefield.
 * Plainscycling {2}
 */
class AlabasterHostIntercessorScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + MarchOfTheMachineSet.cards)
        return d
    }

    test("exiles an opponent's creature until Intercessor leaves the battlefield") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = d.putCreatureOnBattlefield(p2, "Centaur Courser")

        val intercessor = d.putCardInHand(p1, "Alabaster Host Intercessor")
        d.giveMana(p1, Color.WHITE, 6)
        d.castSpell(p1, intercessor)
        d.bothPass() // resolve the creature; ETB trigger asks for a target

        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(p1, listOf(victim))
        d.bothPass() // resolve the exile trigger

        d.getExile(p2) shouldContain victim
        d.state.getBattlefield(p2) shouldNotContain victim

        val blade = d.putCardInHand(p1, "Doom Blade")
        d.giveMana(p1, Color.BLACK, 2)
        d.castSpell(p1, blade, targets = listOf(intercessor))
        d.bothPass() // Intercessor dies -> LTB return trigger
        d.bothPass() // resolve the return

        d.state.getBattlefield(p2) shouldContain victim
        d.getExile(p2) shouldNotContain victim
    }
})
