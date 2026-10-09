package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.BenalishFaithbonder
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BenalishFaithbonderScenarioTest : FunSpec({
    val helper = card("Faithbonder Helper") { typeLine = "Creature — Bear"; power = 3; toughness = 3 }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(BenalishFaithbonder, DominariaUnitedForest274, helper))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    test("enlist taps the helper while vigilance keeps Faithbonder untapped") {
        val d = setup(); val active = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(active, BenalishFaithbonder.name)
        val b = d.putCreatureOnBattlefield(active, helper.name)
        d.removeSummoningSickness(a); d.removeSummoningSickness(b)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(a), d.getOpponent(active)).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe listOf(b)
        d.submitCardSelection(active, listOf(b)).error shouldBe null
        d.isTapped(a) shouldBe false; d.isTapped(b) shouldBe true
        d.state.projectedState.getPower(a) shouldBe 1
        d.bothPass()
        d.state.projectedState.getPower(a) shouldBe 4
        d.state.projectedState.getToughness(a) shouldBe 3
    }
    test("another attacking Faithbonder cannot be enlisted despite remaining untapped") {
        val d = setup(); val active = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(active, BenalishFaithbonder.name)
        val b = d.putCreatureOnBattlefield(active, BenalishFaithbonder.name)
        d.removeSummoningSickness(a); d.removeSummoningSickness(b)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(a,b), d.getOpponent(active)).error shouldBe null
        d.pendingDecision shouldBe null
        d.state.stack.size shouldBe 0
        d.isTapped(a) shouldBe false; d.isTapped(b) shouldBe false
    }
})
