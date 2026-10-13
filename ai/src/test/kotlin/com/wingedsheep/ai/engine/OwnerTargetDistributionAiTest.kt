package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.rollout.FastDecisionResponder
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.actions.decision.DecisionValidators
import com.wingedsheep.engine.legalactions.TargetInfo
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OwnerTargetDistributionAiTest : FunSpec({
    test("all target policies spread required graveyard picks even when one owner has the top-ranked cards") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all)
            initMirrorMatch(Deck.of("Mountain" to 40))
        }
        val a = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val b = d.putCardInGraveyard(d.player1, "Lightning Bolt")
        val c = d.putCardInGraveyard(d.player2, "Lightning Bolt")
        val groups = listOf(listOf(a, b), listOf(c))
        val info = TargetInfo(0, "one per owner", 2, 2, listOf(a, b, c), exclusiveGroups = groups)
        TargetSelection.pick(d.state, info, info.validTargets) { if (it == c) 0.0 else 100.0 }
            .toSet() shouldBe setOf(a, c)
        val decision = ChooseTargetsDecision("owners", d.player1, "One per graveyard", DecisionContext(),
            listOf(TargetRequirementInfo(0, "one per owner", 2, 2, exclusiveGroups = groups)),
            mapOf(0 to listOf(a, b, c)))
        val registry = CardRegistry().apply { register(TestCards.all) }
        listOf(
            TargetsResponse(decision.id, decision.minimalSelection()),
            FastDecisionResponder().respond(d.state, decision, d.player1),
            DecisionResponder(GameSimulator(registry), AIPlayer.defaultEvaluator()).respond(d.state, decision, d.player1),
        ).forEach { answer ->
            DecisionValidators.validate(decision, answer, d.state) shouldBe null
            val selected = (answer as TargetsResponse).selectedTargets.getValue(0)
            selected.size shouldBe 2
            selected.contains(c) shouldBe true
        }
    }
    test("every automatic policy satisfies owner spread and numeric equality together") {
        val one = com.wingedsheep.sdk.dsl.card("Owner AI Value One") { manaCost = "{1}"; typeLine = "Instant" }
        val two = com.wingedsheep.sdk.dsl.card("Owner AI Value Two") { manaCost = "{2}"; typeLine = "Instant" }
        val cards = TestCards.all + listOf(one, two)
        val d = GameTestDriver().apply { registerCards(cards); initMirrorMatch(Deck.of("Mountain" to 40)) }
        val a1 = d.putCardInGraveyard(d.player1, one.name)
        val a2 = d.putCardInGraveyard(d.player1, two.name)
        val b2 = d.putCardInGraveyard(d.player2, two.name)
        val b1 = d.putCardInGraveyard(d.player2, one.name)
        val owners = listOf(listOf(a1, a2), listOf(b2, b1))
        val numeric = listOf(listOf(a1, b1), listOf(a2, b2))
        val pool = listOf(a1, a2, b2, b1)
        val decision = ChooseTargetsDecision("combined", d.player1, "Equal value, one per owner", DecisionContext(),
            listOf(TargetRequirementInfo(0, "combined", 2, 2, selectionGroups = numeric, exclusiveGroups = owners,
                sameNumericProperty = com.wingedsheep.sdk.scripting.values.CardNumericProperty.MANA_VALUE)), mapOf(0 to pool))
        val info = TargetInfo(0, "combined", 2, 2, pool, selectionGroups = numeric, exclusiveGroups = owners)
        val picks = TargetSelection.pick(d.state, info, pool) { if (it == a1 || it == b2) 100.0 else 1.0 }
        val registry = CardRegistry().apply { register(cards) }
        listOf(TargetsResponse(decision.id, mapOf(0 to picks)), TargetsResponse(decision.id, decision.minimalSelection()),
            FastDecisionResponder().respond(d.state, decision, d.player1),
            DecisionResponder(GameSimulator(registry), AIPlayer.defaultEvaluator()).respond(d.state, decision, d.player1)).forEach {
            DecisionValidators.validate(decision, it, d.state) shouldBe null
        }
    }

})
