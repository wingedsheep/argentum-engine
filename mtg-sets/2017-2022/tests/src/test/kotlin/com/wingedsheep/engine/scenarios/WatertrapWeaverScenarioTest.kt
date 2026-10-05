package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.IxalanSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Watertrap Weaver — {2}{U} Creature — Merfolk Wizard 2/2
 * When this creature enters, tap target creature an opponent controls. That creature doesn't
 * untap during its controller's next untap step.
 */
class WatertrapWeaverScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + IxalanSet.cards)
        return d
    }

    test("taps an opponent's creature and keeps it tapped through its controller's next untap step") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = d.putCreatureOnBattlefield(p2, "Centaur Courser")
        d.removeSummoningSickness(victim)

        val weaver = d.putCardInHand(p1, "Watertrap Weaver")
        d.giveMana(p1, Color.BLUE, 3)
        d.castSpell(p1, weaver).error shouldBe null
        d.bothPass() // resolve the creature; its ETB trigger asks for a target

        d.isTapped(weaver) shouldBe false

        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(p1, listOf(victim))
        d.bothPass() // resolve the ETB trigger

        d.isTapped(victim) shouldBe true

        // p2's untap step — the victim stays tapped.
        d.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        d.activePlayer shouldBe p2
        d.isTapped(victim) shouldBe true
    }
})
