package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.budget.BudgetPolicy
import com.wingedsheep.ai.engine.budget.BudgetTier
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.budget.SearchAllowances
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class HearthcageGiantAiTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
    }
    fun registry() = CardRegistry().apply { register(TestCards.all) }

    test("heuristic pump targets our Giant and the paid ability buffs only that Giant") {
        val driver = driver()
        val registry = registry()
        val ours = driver.putCreatureOnBattlefield(driver.player1, "Hearthcage Giant")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Hearthcage Giant")
        val fodder = driver.putCreatureOnBattlefield(driver.player1, "Water Elemental")
        val simulator = GameSimulator(registry)
        val legal = simulator.getLegalActions(driver.state, driver.player1)
            .first { (it.action as? ActivateAbility)?.sourceId == ours }
        val filled = TargetSelection.fillHeuristically(
            driver.state, legal, driver.player1, true, IntentCatalog.of(registry)
        ).shouldBeInstanceOf<ActivateAbility>()
        filled.targets shouldBe listOf(ChosenTarget.Permanent(ours))
        val result = simulator.simulate(
            driver.state, filled.copy(costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)))
        )
        result.state.projectedState.getPower(ours) shouldBe 8
        result.state.projectedState.getToughness(ours) shouldBe 6
        result.state.projectedState.getPower(theirs) shouldBe 5
        result.state.projectedState.getToughness(theirs) shouldBe 5
    }

    for (tier in listOf(BudgetTier.ROUTINE, BudgetTier.NORMAL)) {
        test("$tier search keeps a friendly pump target with more opponents than the target cap") {
            val driver = driver()
            val registry = registry()
            val ours = driver.putCreatureOnBattlefield(driver.player1, "Hearthcage Giant")
            driver.putCreatureOnBattlefield(driver.player1, "Water Elemental")
            repeat(10) { driver.putCreatureOnBattlefield(driver.player2, "Hearthcage Giant") }
            val simulator = GameSimulator(registry)
            val actions = simulator.getLegalActions(driver.state, driver.player1)
            val legal = actions.first { (it.action as? ActivateAbility)?.sourceId == ours }
            val policy = object : BudgetPolicy {
                fun budget() = DecisionBudget(tier, SearchAllowances.forMillis(tier.millis), tier.millis)
                override fun budgetFor(state: GameState, playerId: EntityId, meaningfulActions: List<LegalAction>) = budget()
                override fun budgetForDecision(state: GameState, playerId: EntityId) = budget()
            }
            val strategist = Strategist(
                simulator, BoardEvaluator { _, projected, _ ->
                    if (projected.getPower(ours) == 8) 100.0 else 0.0
                }, budgetPolicy = policy,
                intents = IntentCatalog.of(registry)
            )
            val pass = actions.first { it.actionType == "PassPriority" }
            val chosen = strategist.chooseAction(driver.state, listOf(legal, pass), driver.player1)
                .action.shouldBeInstanceOf<ActivateAbility>()
            chosen.targets shouldBe listOf(ChosenTarget.Permanent(ours))
        }
    }

    test("the frozen legacy heuristic remains unchanged when card knowledge is off") {
        val driver = driver()
        val registry = registry()
        val ours = driver.putCreatureOnBattlefield(driver.player1, "Hearthcage Giant")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Hearthcage Giant")
        driver.putCreatureOnBattlefield(driver.player1, "Water Elemental")
        val legal = GameSimulator(registry).getLegalActions(driver.state, driver.player1)
            .first { (it.action as? ActivateAbility)?.sourceId == ours }
        val filled = TargetSelection.fillHeuristically(driver.state, legal, driver.player1, true)
            .shouldBeInstanceOf<ActivateAbility>()
        filled.targets shouldBe listOf(ChosenTarget.Permanent(theirs))
    }
})
