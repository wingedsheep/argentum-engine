package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Avacyn's Judgment (SOI #145) — {1}{R} Sorcery, madness {X}{R}. "Avacyn's Judgment deals 2 damage
 * divided as you choose among any number of targets. If this spell's madness cost was paid, it
 * deals X damage divided as you choose among those permanents and/or players instead."
 */
class AvacynsJudgmentScenarioTest : FunSpec({
    val selfDiscard = card("Self Discard") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.Discard(1) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(selfDiscard))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 20) bothPass()
    }

    fun GameTestDriver.castFromHand(p: EntityId, judgment: EntityId, split: Map<EntityId, Int>) = submit(
        CastSpell(
            p, judgment,
            targets = split.keys.map { if (it == p || it == getOpponent(p)) ChosenTarget.Player(it) else ChosenTarget.Permanent(it) },
            damageDistribution = split,
            paymentStrategy = PaymentStrategy.FromPool,
        )
    )

    test("cast from hand, it divides 2 damage among up to two targets") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val lions = d.putCreatureOnBattlefield(opp, "Savannah Lions")
        val judgment = d.putCardInHand(p, "Avacyn's Judgment")
        d.giveMana(p, Color.RED, 2)

        d.castFromHand(p, judgment, mapOf(opp to 1, lions to 1)).error shouldBe null
        d.resolveStack()

        d.getLifeTotal(opp) shouldBe 19
        d.findPermanent(opp, "Savannah Lions") shouldBe null
    }

    test("cast from hand, it can't be divided among three targets") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val lions = d.putCreatureOnBattlefield(opp, "Savannah Lions")
        val courser = d.putCreatureOnBattlefield(opp, "Centaur Courser")
        val judgment = d.putCardInHand(p, "Avacyn's Judgment")
        d.giveMana(p, Color.RED, 2)

        d.castFromHand(p, judgment, mapOf(opp to 1, lions to 1, courser to 1)).error shouldNotBe null
    }

    test("cast for madness, X is announced, then up to X targets, then X damage divided among them") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val lions = d.putCreatureOnBattlefield(opp, "Savannah Lions")
        val courser = d.putCreatureOnBattlefield(opp, "Centaur Courser")
        val judgment = d.putCardInHand(p, "Avacyn's Judgment")

        val discard = d.putCardInHand(p, "Self Discard")
        d.giveColorlessMana(p, 1)
        d.submitSuccess(CastSpell(p, discard, paymentStrategy = PaymentStrategy.FromPool))
        d.bothPass()
        d.submitCardSelection(p, listOf(judgment))
        d.resolveStack()

        d.giveMana(p, Color.RED, 4) // {X}{R} with X up to 3
        d.submitYesNo(p, true)

        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        ask.maxValue shouldBe 3
        d.submitDecision(p, NumberChosenResponse(ask.id, 3)).error shouldBe null

        val targets = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        // Four legal targets (both players, two creatures); X = 3 caps the count.
        targets.targetRequirements.single().maxTargets shouldBe 3
        d.submitDecision(p, TargetsResponse(targets.id, mapOf(0 to listOf(opp, lions, courser)))).error shouldBe null

        val divide = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        divide.totalAmount shouldBe 3
        d.submitDecision(p, DistributionResponse(divide.id, mapOf(opp to 1, lions to 1, courser to 1))).error shouldBe null
        d.resolveStack()

        d.getLifeTotal(opp) shouldBe 19
        d.findPermanent(opp, "Savannah Lions") shouldBe null
        d.findPermanent(opp, "Centaur Courser") shouldNotBe null
        d.getGraveyard(p).contains(judgment) shouldBe true
    }
})
