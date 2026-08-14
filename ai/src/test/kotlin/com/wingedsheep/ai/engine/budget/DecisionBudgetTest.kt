package com.wingedsheep.ai.engine.budget

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe

/**
 * The budget arithmetic — the part that has to be right for `ArenaBudgetScalingTest` to mean
 * anything, and the part that has to be *unchanged* for `AiProfile.LEGACY_V0` to stay the frozen
 * reference opponent.
 */
class DecisionBudgetTest : FunSpec({

    test("the NORMAL tier reproduces today's search counts exactly") {
        // If this ever fails, the permanent reference opponent has moved and every published
        // arena number is measured against a different baseline than it claims.
        //
        // The one field that deliberately differs is the combat wall clock: v0 pins it at 1000 ms
        // regardless, while a tiered budget hands combat the whole tier (and combat is always
        // CRITICAL, so it is 5000 ms in practice — more than v0, never less).
        SearchAllowances.forMillis(BudgetTier.NORMAL.millis)
            .copy(combatSearchMillis = SearchAllowances.LEGACY.combatSearchMillis) shouldBe
            SearchAllowances.LEGACY
    }

    test("allowances are monotone in the budget") {
        val ladder = listOf(100L, 500L, 1_000L, 2_000L, 3_000L, 5_000L).map { SearchAllowances.forMillis(it) }
        ladder.zipWithNext().forEach { (smaller, bigger) ->
            bigger.targetCandidates shouldBeGreaterThanOrEqualTo smaller.targetCandidates
            bigger.blockSimulations shouldBeGreaterThanOrEqualTo smaller.blockSimulations
            bigger.attackSearchIterations shouldBeGreaterThanOrEqualTo smaller.attackSearchIterations
            bigger.combatSearchMillis shouldBeGreaterThanOrEqualTo smaller.combatSearchMillis
        }
    }

    test("blocking search never drops below its pre-budget floor") {
        // The risk register's "combat's 1 s cap fights the global budget" line: MAX_BLOCK_SIMULATIONS
        // is a floor now, so even the smallest tier searches blocking as hard as v0 did.
        listOf(1L, 50L, 100L, 200L, 2_000L, 10_000L).forEach { millis ->
            SearchAllowances.forMillis(millis).blockSimulations shouldBeGreaterThanOrEqualTo 10
        }
    }

    test("below NORMAL the simulation-refined target pick is dropped, at and above it is kept") {
        SearchAllowances.forMillis(BudgetTier.ROUTINE.millis).refineTargetsBySimulation shouldBe false
        SearchAllowances.forMillis(BudgetTier.NORMAL.millis).refineTargetsBySimulation shouldBe true
        SearchAllowances.forMillis(BudgetTier.CRITICAL.millis).refineTargetsBySimulation shouldBe true
    }

    test("a legacy budget never expires, but still caps the combat searches at 1000 ms") {
        val budget = DecisionBudget.legacy()
        budget.expired().shouldBeFalse()
        budget.deadlineNanos shouldBe Long.MAX_VALUE
        // The combat deadline is a real deadline even when the global one isn't — that is exactly
        // the pre-Phase-4 arrangement, where only `improveAttackViaLocalSearch` had a stopwatch.
        (budget.combatDeadlineNanos < Long.MAX_VALUE) shouldBe true
    }

    test("an already-elapsed budget reports expired rather than negative time") {
        val budget = DecisionBudget(
            BudgetTier.ROUTINE, SearchAllowances.forMillis(1), millis = 1,
            startNanos = System.nanoTime() - 1_000_000_000L,
        )
        budget.expired() shouldBe true
        budget.remainingMs() shouldBe 0L
    }

    test("a tiered policy scales every tier off its NORMAL size") {
        val policy = TieredBudgetPolicy(normalMillis = 1_000)
        policy.millisFor(BudgetTier.TRIVIAL) shouldBe 0L
        policy.millisFor(BudgetTier.ROUTINE) shouldBe 100L
        policy.millisFor(BudgetTier.NORMAL) shouldBe 1_000L
        policy.millisFor(BudgetTier.CRITICAL) shouldBe 2_500L
        // The default reproduces the plan's published table.
        val default = TieredBudgetPolicy()
        BudgetTier.entries.forEach { default.millisFor(it) shouldBe it.millis }
    }

    test("a policy names itself, because an arena report identifies an agent by it") {
        LegacyBudgetPolicy.toString() shouldBe "legacy"
        TieredBudgetPolicy(3_000).toString() shouldBe "tiered(3000ms)"
    }
})
