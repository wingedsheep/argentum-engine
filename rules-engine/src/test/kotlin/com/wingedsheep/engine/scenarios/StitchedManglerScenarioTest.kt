package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.ShadowsOverInnistradSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Stitched Mangler — {2}{U} Creature — Zombie Horror 2/3
 * This creature enters tapped.
 * When this creature enters, tap target creature an opponent controls. That creature doesn't
 * untap during its controller's next untap step.
 */
class StitchedManglerScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ShadowsOverInnistradSet.cards)
        return d
    }

    test("enters tapped, taps an opponent's creature, and keeps it tapped through its next untap") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = d.putCreatureOnBattlefield(p2, "Centaur Courser")
        d.removeSummoningSickness(victim)

        val mangler = d.putCardInHand(p1, "Stitched Mangler")
        d.giveMana(p1, Color.BLUE, 3)
        d.castSpell(p1, mangler)
        d.bothPass() // resolve the creature; it enters tapped and its ETB trigger asks for a target

        // Stitched Mangler entered tapped.
        d.isTapped(mangler) shouldBe true

        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(p1, listOf(victim))
        d.bothPass() // resolve the ETB tap trigger

        d.isTapped(victim) shouldBe true

        // Advance to p2's untap step — the victim must stay tapped (doesn't untap).
        d.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        d.activePlayer shouldBe p2
        d.isTapped(victim) shouldBe true
    }
})
