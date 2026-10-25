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
 * White Dragon — {4}{W}{W} Creature — Dragon 4/4
 * Flying
 * Cold Breath — When this creature enters, tap target creature an opponent controls. That creature
 * doesn't untap during its controller's next untap step.
 */
class WhiteDragonScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ForgottenRealmsSet.cards)
        return d
    }

    test("Cold Breath taps an opponent's creature and keeps it tapped through its controller's next untap step") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = d.putCreatureOnBattlefield(p2, "Centaur Courser")
        d.removeSummoningSickness(victim)

        val dragon = d.putCardInHand(p1, "White Dragon")
        d.giveMana(p1, Color.WHITE, 6)
        d.castSpell(p1, dragon).error shouldBe null
        d.bothPass() // resolve the creature; its ETB trigger asks for a target

        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(p1, listOf(victim))
        d.bothPass() // resolve the ETB trigger

        d.isTapped(victim) shouldBe true
        d.isTapped(dragon) shouldBe false

        // p2's untap step — the victim stays tapped.
        d.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        d.activePlayer shouldBe p2
        d.isTapped(victim) shouldBe true
    }
})
