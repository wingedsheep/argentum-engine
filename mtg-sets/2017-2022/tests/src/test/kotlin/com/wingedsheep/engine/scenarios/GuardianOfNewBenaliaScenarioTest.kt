package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.GuardianOfNewBenalia
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class GuardianOfNewBenaliaScenarioTest : FunSpec({
    val helper = card("Guardian Helper") { typeLine = "Creature — Bear"; power = 3; toughness = 3 }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(GuardianOfNewBenalia, DominariaUnitedForest274, helper))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.attackWith(a: com.wingedsheep.sdk.model.EntityId, enlist: List<com.wingedsheep.sdk.model.EntityId>) {
        val p = activePlayer!!
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(p, listOf(a), getOpponent(p)).error shouldBe null
        submitCardSelection(p, enlist).error shouldBe null
    }
    test("enlisting a creature triggers scry 2 alongside the power boost") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, GuardianOfNewBenalia.name)
        val b = d.putCreatureOnBattlefield(p, helper.name)
        d.removeSummoningSickness(a); d.removeSummoningSickness(b)
        d.attackWith(a, listOf(b))
        (d.pendingDecision as? OrderObjectsDecision)?.let { d.submitOrderedResponse(it.playerId, it.objects) }
        d.state.stack.size shouldBe 2
        var scryPrompts = 0
        repeat(6) {
            when (val q = d.pendingDecision) {
                null -> if (d.state.stack.isNotEmpty()) d.bothPass()
                is SelectCardsDecision -> { scryPrompts++; q.options.size shouldBe 2; d.autoResolveDecision() }
                else -> d.autoResolveDecision()
            }
        }
        scryPrompts shouldBe 1
        d.state.stack.size shouldBe 0
        d.state.projectedState.getPower(a) shouldBe 5
    }
    test("declining to enlist does not scry") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, GuardianOfNewBenalia.name)
        val b = d.putCreatureOnBattlefield(p, helper.name)
        d.removeSummoningSickness(a); d.removeSummoningSickness(b)
        d.attackWith(a, emptyList())
        d.state.stack.size shouldBe 0
        d.pendingDecision shouldBe null
    }
    test("discard a card: gains indestructible until end of turn and becomes tapped") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, GuardianOfNewBenalia.name)
        val fodder = d.putCardInHand(p, "Forest")
        val action = d.legalActions(p).map { it.action }.filterIsInstance<ActivateAbility>()
            .single { it.sourceId == a }
        d.submit(action.copy(costPayment = AdditionalCostPayment(discardedCards = listOf(fodder)))).error shouldBe null
        d.getGraveyard(p) shouldContain fodder
        d.bothPass().error shouldBe null
        d.state.projectedState.hasKeyword(a, Keyword.INDESTRUCTIBLE) shouldBe true
        d.isTapped(a) shouldBe true
    }
})
