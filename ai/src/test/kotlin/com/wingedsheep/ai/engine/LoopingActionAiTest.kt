package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.budget.BudgetPolicy
import com.wingedsheep.ai.engine.budget.BudgetTier
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.budget.LegacyBudgetPolicy
import com.wingedsheep.ai.engine.budget.SearchAllowances
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Regression: the AI must never spend a priority window going in circles.
 *
 * Reported from a real game — the AI activated Aphetto Alchemist ({T}: Untap target artifact or
 * creature) eleven times in a row and would have kept going, because aiming it at itself pays its
 * own cost back and leaves the board exactly as it was.
 *
 * The evaluator is stubbed here, and deliberately: the scoring bias that made the AI *want* the
 * no-op is that a candidate's leaf is scored where the action leaves the game while passing's leaf
 * is scored after the game has moved on. `stepPreferring` is the smallest honest model of it —
 * "passing lets the next step happen, and the next step is bad" — and it makes the loop reproduce
 * on demand instead of only on the board that happened to be in front of the player. What is under
 * test is that [StateProgress] refuses the line whatever the score says.
 */
class LoopingActionAiTest : FunSpec({

    /** Prefers standing still: any leaf still in [step] beats one where the game has moved on. */
    fun stepPreferring(step: Step) = BoardEvaluator { state, _, _ -> if (state.step == step) 0.0 else -100.0 }

    fun registry(): CardRegistry = CardRegistry().apply { register(TestCards.all) }

    /** A game where [ai] holds priority in the opponent's end-of-combat step, with nothing to do. */
    fun openWindow(driver: GameTestDriver): Pair<EntityId, EntityId> {
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        return driver.player2 to driver.player1
    }

    fun handPriorityToNonActivePlayer(driver: GameTestDriver, activePlayer: EntityId) {
        driver.passPriorityUntil(Step.END_COMBAT)
        driver.submitSuccess(PassPriority(activePlayer))
    }

    fun strategistFor(
        registry: CardRegistry,
        step: Step,
        budgetPolicy: BudgetPolicy = LegacyBudgetPolicy,
    ): Strategist {
        val simulator = GameSimulator(registry)
        return Strategist(simulator, stepPreferring(step), budgetPolicy = budgetPolicy)
    }

    fun chooseFor(strategist: Strategist, registry: CardRegistry, state: GameState, playerId: EntityId) =
        strategist.chooseAction(state, GameSimulator(registry).getLegalActions(state, playerId), playerId)

    test("untapping itself leaves the position untouched, untapping another creature does not") {
        val registry = registry()
        val driver = GameTestDriver()
        val (ai, human) = openWindow(driver)
        val alchemist = driver.putCreatureOnBattlefield(ai, "Aphetto Alchemist")
        driver.removeSummoningSickness(alchemist)
        val partner = driver.putCreatureOnBattlefield(ai, "Grizzly Bears")
        driver.removeSummoningSickness(partner)
        driver.tapPermanent(partner)
        handPriorityToNonActivePlayer(driver, human)

        val simulator = GameSimulator(registry)
        val abilityId = simulator.getLegalActions(driver.state, ai)
            .mapNotNull { it.action as? ActivateAbility }
            .first { it.sourceId == alchemist }
            .abilityId

        fun untap(target: EntityId) = ActivateAbility(
            playerId = ai, sourceId = alchemist, abilityId = abilityId,
            targets = listOf(ChosenTarget.Permanent(target)),
        )

        val here = StateProgress.digest(driver.state)

        val self = simulator.simulate(driver.state, untap(alchemist))
        withClue("untapping itself pays its own cost back") {
            StateProgress.digest(self.state) shouldBe here
        }

        val other = simulator.simulate(driver.state, untap(partner))
        withClue("untapping the tapped partner is a real change") {
            StateProgress.digest(other.state) shouldNotBe here
        }
    }

    test("an ability with one inert target and one productive one is aimed, not dropped") {
        // Every tier, because the guard's whole failure mode is tier-dependent: below NORMAL the
        // committed targets are `TargetSelection.rank`'s heuristic pick, which ranks board value
        // and cannot see that untapping an untapped creature does nothing.
        for (tier in BudgetTier.entries) {
            val registry = registry()
            val driver = GameTestDriver()
            val (ai, human) = openWindow(driver)
            val alchemist = driver.putCreatureOnBattlefield(ai, "Aphetto Alchemist")
            driver.removeSummoningSickness(alchemist)
            val partner = driver.putCreatureOnBattlefield(ai, "Grizzly Bears")
            driver.removeSummoningSickness(partner)
            driver.tapPermanent(partner)
            handPriorityToNonActivePlayer(driver, human)

            // Untapping itself does nothing; untapping the tapped Bears does. Dropping the whole
            // ability because the *heuristic* target pick happened to be the inert one would trade
            // the loop for a missed play — and below NORMAL that pick is all there is unless
            // `materialize` buys the simulated one back.
            val strategist = strategistFor(registry, Step.END_COMBAT, FixedTierBudgetPolicy(tier))
            val chosen = chooseFor(strategist, registry, driver.state, ai)

            withClue("tier $tier") {
                val activation = chosen.action.shouldBeInstanceOf<ActivateAbility>()
                activation.sourceId shouldBe alchemist
                activation.targets shouldBe listOf(ChosenTarget.Permanent(partner))
            }
        }
    }

    test("the AI passes rather than activate an ability that changes nothing") {
        val registry = registry()
        val driver = GameTestDriver()
        val (ai, human) = openWindow(driver)
        val alchemist = driver.putCreatureOnBattlefield(ai, "Aphetto Alchemist")
        driver.removeSummoningSickness(alchemist)
        handPriorityToNonActivePlayer(driver, human)

        val strategist = strategistFor(registry, Step.END_COMBAT)
        val chosen = chooseFor(strategist, registry, driver.state, ai)

        chosen.actionType shouldBe "PassPriority"
    }

    test("the AI unwinds a two-untapper cycle instead of riding it forever") {
        val registry = registry()
        val driver = GameTestDriver()
        val (ai, human) = openWindow(driver)
        val first = driver.putCreatureOnBattlefield(ai, "Aphetto Alchemist")
        val second = driver.putCreatureOnBattlefield(ai, "Aphetto Alchemist")
        driver.removeSummoningSickness(first)
        driver.removeSummoningSickness(second)
        driver.tapPermanent(second)
        handPriorityToNonActivePlayer(driver, human)

        val strategist = strategistFor(registry, Step.END_COMBAT)
        val simulator = GameSimulator(registry)

        // Untapping the tapped one is a real change, so the AI is allowed to want it.
        val opening = chooseFor(strategist, registry, driver.state, ai)
        opening.actionType shouldNotBe "PassPriority"
        val afterOpening = simulator.simulate(driver.state, opening.action).state

        // Untapping it straight back would return the game to the position we just acted from —
        // the whole cycle. The only other option, untapping itself, changes nothing at all.
        val reply = chooseFor(strategist, registry, afterOpening, ai)
        reply.actionType shouldBe "PassPriority"
    }
})

/**
 * Pins every decision to one [BudgetTier], so a test can say which allowances it is exercising
 * instead of hoping `TieredBudgetPolicy` picks the tier it had in mind from the board.
 */
private class FixedTierBudgetPolicy(private val tier: BudgetTier) : BudgetPolicy {
    private fun budget() = DecisionBudget(tier, SearchAllowances.forMillis(tier.millis), tier.millis)

    override fun budgetFor(
        state: GameState,
        playerId: EntityId,
        meaningfulActions: List<LegalAction>,
    ): DecisionBudget = budget()

    override fun budgetForDecision(state: GameState, playerId: EntityId): DecisionBudget = budget()

    override fun toString(): String = "fixed-$tier"
}
