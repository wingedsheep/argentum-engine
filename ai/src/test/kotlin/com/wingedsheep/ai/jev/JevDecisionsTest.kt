package com.wingedsheep.ai.jev

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.actions.decision.DecisionValidators
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class JevDecisionsTest : FunSpec({
    val player = EntityId("player")
    val a = EntityId("a")
    val b = EntityId("b")
    val context = DecisionContext()
    fun chooser(last: Boolean = false) = JevDecisions(JevChoices(JevChoiceClient { _, _, choices, _ ->
        if (last) choices.keys.last() else choices.keys.first()
    }, "state", 1000), label = { it.value })

    val decisions = listOf<PendingDecision>(
        YesNoDecision("d", player, "May", context),
        BatchYesNoDecision("d", player, "May", context, 3),
        ChooseOptionDecision("d", player, "Option", context, listOf("first", "second")),
        ChooseNumberDecision("d", player, "Number", context, 1, 5),
        ChooseColorDecision("d", player, "Color", context, setOf(Color.RED, Color.BLUE)),
        ChooseModeDecision("d", player, "Mode", context, listOf(ModeOption(0, "unavailable", false), ModeOption(1, "available"))),
        SelectCardsDecision("d", player, "Select", context, listOf(a, b), 1, 2),
        SearchLibraryDecision("d", player, "Search", context, listOf(a, b), 0, 1, emptyMap(), "any card"),
        ChooseTargetsDecision("d", player, "Targets", context,
            listOf(TargetRequirementInfo(0, "one", 1, 1), TargetRequirementInfo(1, "optional", 0, 1)), mapOf(0 to listOf(a, b), 1 to listOf(b))),
        OrderObjectsDecision("d", player, "Order", context, listOf(a, b)),
        ReorderLibraryDecision("d", player, "Order", context, listOf(a, b), emptyMap()),
        SplitPilesDecision("d", player, "Piles", context, listOf(a, b)),
        ChooseReplacementDecision("d", player, "Replace", context, listOf("red", "blue"), listOf("green", "white"), allowedToByFrom = listOf(listOf(1), listOf(0))),
        BudgetModalDecision("d", player, "Budget", context, 3, listOf(BudgetModeOption(1, "draw"), BudgetModeOption(2, "token"))),
        DistributeDecision("d", player, "Divide", context, 3, listOf(a, b), 1),
        AssignDamageDecision("d", player, "Damage", context, a, 3, listOf(b), null, mapOf(b to 2), mapOf(b to 3), false, false),
        SelectManaSourcesDecision("d", player, "Mana", context,
            listOf(ManaSourceOption(a, "Mountain", setOf(Color.RED), false)), "{R}", listOf(a)),
        CombatResolutionDecision("d", player, "Combat damage", context, false, emptyList(), emptyList(), emptyList(),
            listOf(DamageEdge("edge", a, b, DamageEdgeDirection.ATTACKER_TO_BLOCKER, 3, 3, 2, true, false, player))),
    )
    decisions.forEach { decision ->
        test("Jev assembles ${decision::class.simpleName} with valid routing and selection shape") {
            val response = chooser().answer(decision)
            response.decisionId shouldBe decision.id
            DecisionValidators.validate(decision, response) shouldBe null
        }
    }
    test("Jev can decline optional targets and fail to find without selecting the first card") {
        val decision = SearchLibraryDecision("d", player, "Search", context, listOf(a, b), 0, 1, emptyMap(), "any")
        (chooser(last = true).answer(decision) as CardsSelectedResponse).selectedCards shouldBe emptyList()
    }
    test("ordering is controlled by Jev and preserves every object exactly once") {
        val decision = OrderObjectsDecision("d", player, "Order", context, listOf(a, b))
        (chooser(last = true).answer(decision) as OrderedResponse).orderedObjects shouldBe listOf(b, a)
    }
})
