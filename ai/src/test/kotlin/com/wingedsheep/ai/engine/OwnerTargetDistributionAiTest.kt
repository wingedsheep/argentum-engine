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
})
