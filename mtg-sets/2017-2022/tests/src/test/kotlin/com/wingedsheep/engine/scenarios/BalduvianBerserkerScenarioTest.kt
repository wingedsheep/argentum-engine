package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.BalduvianBerserker
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BalduvianBerserkerScenarioTest : FunSpec({
    val helper = card("Berserker Helper") { typeLine = "Creature — Bear"; power = 3; toughness = 3 }
    val doom = card("Berserker Doom") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Destroy(target(TargetFilter.Creature)) }
    }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(BalduvianBerserker, DominariaUnitedForest274, helper, doom))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.killAndAim(victim: com.wingedsheep.sdk.model.EntityId, aim: com.wingedsheep.sdk.model.EntityId) {
        val p = state.priorityPlayerId!!
        val spell = putCardInHand(p, doom.name)
        castSpell(p, spell, listOf(victim)).error shouldBe null
        bothPass().error shouldBe null
        val decision = pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        submitTargetSelection(decision.playerId, listOf(aim)).error shouldBe null
        bothPass().error shouldBe null
    }
    test("dying deals damage equal to its base power") {
        val d = setup(); val p = d.activePlayer!!; val opp = d.getOpponent(p)
        val a = d.putCreatureOnBattlefield(p, BalduvianBerserker.name)
        d.killAndAim(a, opp)
        d.getLifeTotal(opp) shouldBe 19
    }
    test("an enlisted power boost counts as last-known power when it dies") {
        val d = setup(); val p = d.activePlayer!!; val opp = d.getOpponent(p)
        val a = d.putCreatureOnBattlefield(p, BalduvianBerserker.name)
        val b = d.putCreatureOnBattlefield(p, helper.name)
        d.removeSummoningSickness(a); d.removeSummoningSickness(b)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p, listOf(a), opp).error shouldBe null
        d.submitCardSelection(p, listOf(b)).error shouldBe null
        d.bothPass().error shouldBe null
        d.state.projectedState.getPower(a) shouldBe 4
        d.killAndAim(a, opp)
        d.getLifeTotal(opp) shouldBe 16
    }
})
