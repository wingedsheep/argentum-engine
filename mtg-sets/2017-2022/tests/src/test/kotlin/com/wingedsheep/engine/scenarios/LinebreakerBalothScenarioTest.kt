package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.LinebreakerBaloth
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class LinebreakerBalothScenarioTest : FunSpec({
    val small = card("Baloth Small Blocker") { typeLine = "Creature — Bear"; power = 2; toughness = 2 }
    val big = card("Baloth Big Blocker") { typeLine = "Creature — Bear"; power = 3; toughness = 3 }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(LinebreakerBaloth, DominariaUnitedForest274, small, big))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    test("creatures with power 2 or less can't block it; power 3 can") {
        val d = setup(); val p = d.activePlayer!!; val opp = d.getOpponent(p)
        val a = d.putCreatureOnBattlefield(p, LinebreakerBaloth.name); d.removeSummoningSickness(a)
        val s = d.putCreatureOnBattlefield(opp, small.name)
        val b = d.putCreatureOnBattlefield(opp, big.name)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p, listOf(a), opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, mapOf(s to listOf(a))).error.shouldNotBeNull()
        d.declareBlockers(opp, mapOf(b to listOf(a))).error shouldBe null
    }
    test("enlist adds the tapped creature's power") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, LinebreakerBaloth.name)
        val h = d.putCreatureOnBattlefield(p, big.name)
        d.removeSummoningSickness(a); d.removeSummoningSickness(h)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p, listOf(a), d.getOpponent(p)).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(p, listOf(h)).error shouldBe null
        d.bothPass()
        d.state.projectedState.getPower(a) shouldBe 7
    }
})
