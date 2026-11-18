package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m15.cards.RotfeasterMaggot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Rotfeaster Maggot (M15 #112) — {4}{B} 3/5 Insect.
 *
 *   When this creature enters, exile target creature card from a graveyard. You gain life equal
 *   to that card's toughness.
 *
 * The life is read off the exiled card itself: its toughness, not its power, and not the
 * toughness it had on the battlefield before it died.
 */
class RotfeasterMaggotScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + RotfeasterMaggot)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("exiles a creature card from an opponent's graveyard and gains life equal to its toughness") {
        val d = driver()
        val you = d.player1
        val opp = d.player2
        // A 3/5: the gain must be 5 (toughness), not 3 (power).
        val deadMaggot = d.putCardInGraveyard(opp, "Rotfeaster Maggot")
        val maggot = d.putCardInHand(you, "Rotfeaster Maggot")
        val lifeBefore = d.getLifeTotal(you)

        d.giveMana(you, Color.BLACK, 5)
        d.castSpell(you, maggot).error shouldBe null
        d.bothPass() // the creature spell resolves; its enters trigger asks for a target

        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(you, listOf(deadMaggot)).error shouldBe null
        d.bothPass() // the trigger resolves

        d.getExile(opp) shouldContain deadMaggot
        d.getGraveyard(opp) shouldNotContain deadMaggot
        d.getLifeTotal(you) shouldBe lifeBefore + 5
    }

    test("a creature that died with +1/+1 counters yields its printed toughness") {
        val d = driver()
        val you = d.player1
        val opp = d.player2
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.addComponent(bears, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        d.state.projectedState.getToughness(bears) shouldBe 3

        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, bolt, listOf(bears)).error shouldBe null
        d.bothPass() // Lightning Bolt resolves; the 3/3 Bears die
        d.getGraveyard(opp) shouldContain bears

        val maggot = d.putCardInHand(you, "Rotfeaster Maggot")
        val lifeBefore = d.getLifeTotal(you)
        d.giveMana(you, Color.BLACK, 5)
        d.castSpell(you, maggot).error shouldBe null
        d.bothPass()
        d.submitTargetSelection(you, listOf(bears)).error shouldBe null
        d.bothPass()

        d.getExile(opp) shouldContain bears
        d.getLifeTotal(you) shouldBe lifeBefore + 2
    }
})
