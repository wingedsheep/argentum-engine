package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A cast made while an effect resolves — here the madness trigger's (CR 702.35) — announces what a
 * player's own cast would, in CR 601.2's order: X (601.2b), then targets (601.2c), then the
 * division of damage (601.2d). Each test pins one step: the division is asked while casting and
 * rides the spell onto the stack; a target count that reads X is offered with the announced X;
 * an X that leaves nothing to target casts the spell untargeted.
 */
class SynthesizedCastDivisionTest : FunSpec({

    // Fixed madness cost; 3 damage divided among one to three targets.
    val madnessVolley = card("Madness Volley") {
        manaCost = "{3}{R}"
        typeLine = "Sorcery"
        spell {
            target = AnyTarget(count = 3, minCount = 1)
            effect = Effects.DividedDamage(total = 3, minTargets = 1, maxTargets = 3)
        }
        madness("{R}")
    }

    // Avacyn's Judgment's shape: the madness total, X, also caps the target count.
    val madnessJudgment = card("Madness Judgment") {
        manaCost = "{1}{R}"
        typeLine = "Sorcery"
        val damage = DynamicAmounts.conditional(Conditions.MadnessCostWasPaid, DynamicAmounts.xValue(), DynamicAmounts.fixed(2))
        spell {
            target = AnyTarget(count = 2, minCount = 0, optional = true, dynamicMaxCount = damage)
            effect = Effects.DividedDamage(minTargets = 0, maxTargets = 2, dynamicTotal = damage)
        }
        madness("{X}{R}")
    }

    val selfDiscard = card("Self Discard") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.Discard(1) }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(madnessVolley, madnessJudgment, selfDiscard))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.discardIntoMadness(player: EntityId, cardId: EntityId) {
        val spell = putCardInHand(player, "Self Discard")
        giveColorlessMana(player, 1)
        submitSuccess(CastSpell(player, spell, paymentStrategy = PaymentStrategy.FromPool))
        bothPass()
        submitCardSelection(player, listOf(cardId))
        var guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 20) bothPass()
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 20) bothPass()
    }

    test("the division is announced while casting and rides the spell onto the stack (CR 601.2d)") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val lions = d.putCreatureOnBattlefield(opp, "Savannah Lions")
        val volley = d.putCardInHand(p, "Madness Volley")
        d.discardIntoMadness(p, volley)
        d.giveMana(p, Color.RED, 1)
        d.submitYesNo(p, true)

        val targets = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitDecision(p, TargetsResponse(targets.id, mapOf(0 to listOf(opp, lions)))).error shouldBe null

        val divide = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        divide.context.phase shouldBe DecisionPhase.CASTING
        divide.totalAmount shouldBe 3
        divide.minPerTarget shouldBe 1
        d.submitDecision(p, DistributionResponse(divide.id, mapOf(opp to 2, lions to 1))).error shouldBe null

        d.state.getEntity(volley)?.get<SpellOnStackComponent>()?.damageDistribution shouldBe mapOf(opp to 2, lions to 1)
        d.resolveStack()
        d.getLifeTotal(opp) shouldBe 18
        d.findPermanent(opp, "Savannah Lions") shouldBe null
    }

    test("X is announced before targets, and a target count that reads X is offered with it") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        d.putCreatureOnBattlefield(opp, "Savannah Lions")
        val judgment = d.putCardInHand(p, "Madness Judgment")
        d.discardIntoMadness(p, judgment)
        d.giveMana(p, Color.RED, 2) // {X}{R} with X up to 1
        d.submitYesNo(p, true)

        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        d.submitDecision(p, NumberChosenResponse(ask.id, 1)).error shouldBe null

        val targets = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targets.targetRequirements.single().maxTargets shouldBe 1
        d.submitDecision(p, TargetsResponse(targets.id, mapOf(0 to listOf(opp)))).error shouldBe null

        // One target takes the whole X; there is nothing to divide.
        d.resolveStack()
        d.getLifeTotal(opp) shouldBe 19
    }

    test("an X of 0 leaves nothing to target, and the spell is cast untargeted") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val judgment = d.putCardInHand(p, "Madness Judgment")
        d.discardIntoMadness(p, judgment)
        d.giveMana(p, Color.RED, 1)
        d.submitYesNo(p, true)

        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        ask.maxValue shouldBe 0
        d.submitDecision(p, NumberChosenResponse(ask.id, 0)).error shouldBe null

        d.resolveStack()
        d.getLifeTotal(opp) shouldBe 20
        d.getGraveyard(p).contains(judgment) shouldBe true
    }
})
