package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Welcome to the Fold (SOI #96) — {2}{U}{U} Sorcery. "Gain control of target creature if its
 * toughness is 2 or less. If this spell's madness cost was paid, instead gain control of that
 * creature if its toughness is X or less." Madness {X}{U}{U}.
 *
 * Any creature can be targeted; the toughness check is made on resolution.
 */
class WelcomeToTheFoldScenarioTest : FunSpec({
    val selfDiscard = card("Self Discard") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.Discard(1) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(selfDiscard))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 20) bothPass()
    }

    test("cast from hand, it steals a creature with toughness 2") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val spell = d.putCardInHand(p, "Welcome to the Fold")
        d.giveMana(p, Color.BLUE, 4)
        d.castSpell(p, spell, listOf(bears)).error shouldBe null
        d.resolveStack()
        d.state.projectedState.getController(bears) shouldBe p
    }

    test("cast from hand, a creature with toughness 3 stays with its controller") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val giant = d.putCreatureOnBattlefield(opp, "Hill Giant")
        val spell = d.putCardInHand(p, "Welcome to the Fold")
        d.giveMana(p, Color.BLUE, 4)
        d.castSpell(p, spell, listOf(giant)).error shouldBe null
        d.resolveStack()
        d.state.projectedState.getController(giant) shouldBe opp
        d.getGraveyard(p).contains(spell) shouldBe true
    }

    test("cast for its madness cost with X = 3, it steals a toughness-3 creature") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val giant = d.putCreatureOnBattlefield(opp, "Hill Giant")
        val fold = d.putCardInHand(p, "Welcome to the Fold")

        val discard = d.putCardInHand(p, "Self Discard")
        d.giveColorlessMana(p, 1)
        d.submitSuccess(CastSpell(p, discard, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()
        d.submitCardSelection(p, listOf(fold))
        d.resolveStack()

        d.giveMana(p, Color.BLUE, 5) // {X}{U}{U} with X = 3
        d.submitYesNo(p, true)
        // The madness cast picks its target, then announces X.
        if (d.pendingDecision !is ChooseNumberDecision) d.submitTargetSelection(p, listOf(giant))
        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        d.submitDecision(p, NumberChosenResponse(ask.id, 3)).error shouldBe null
        d.resolveStack()

        d.state.projectedState.getController(giant) shouldBe p
    }

    test("cast for its madness cost with X = 1, even a toughness-2 creature stays put") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val fold = d.putCardInHand(p, "Welcome to the Fold")

        val discard = d.putCardInHand(p, "Self Discard")
        d.giveColorlessMana(p, 1)
        d.submitSuccess(CastSpell(p, discard, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()
        d.submitCardSelection(p, listOf(fold))
        d.resolveStack()

        d.giveMana(p, Color.BLUE, 3) // {X}{U}{U} with X = 1
        d.submitYesNo(p, true)
        if (d.pendingDecision !is ChooseNumberDecision) d.submitTargetSelection(p, listOf(bears))
        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        d.submitDecision(p, NumberChosenResponse(ask.id, 1)).error shouldBe null
        d.resolveStack()

        d.state.projectedState.getController(bears) shouldBe opp
    }
})
