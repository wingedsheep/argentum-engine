package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.CoalitionSkyknight
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.core.Keyword
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CoalitionSkyknightScenarioTest : FunSpec({
    val helper = card("CoalitionSkyknight Helper") { typeLine = "Creature — Bear"; power = 3; toughness = 3 }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(CoalitionSkyknight, DominariaUnitedForest274, helper))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    test("enlisting a 3-power creature adds its power until end of turn") {
        val d = setup(); val active = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(active, CoalitionSkyknight.name)
        val b = d.putCreatureOnBattlefield(active, helper.name)
        d.removeSummoningSickness(a); d.removeSummoningSickness(b)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(a), d.getOpponent(active)).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe listOf(b)
        d.submitCardSelection(active, listOf(b)).error shouldBe null
        d.isTapped(a) shouldBe true; d.isTapped(b) shouldBe true
        d.bothPass()
        d.state.projectedState.getPower(a) shouldBe 5
        d.state.projectedState.hasKeyword(a, Keyword.FLYING) shouldBe true
    }
    test("declining to enlist leaves the helper untapped and power unchanged") {
        val d = setup(); val active = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(active, CoalitionSkyknight.name)
        val b = d.putCreatureOnBattlefield(active, helper.name)
        d.removeSummoningSickness(a); d.removeSummoningSickness(b)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(a), d.getOpponent(active)).error shouldBe null
        d.submitCardSelection(active, emptyList()).error shouldBe null
        d.isTapped(b) shouldBe false
        d.state.stack.size shouldBe 0
        d.state.projectedState.getPower(a) shouldBe 2
    }
})
