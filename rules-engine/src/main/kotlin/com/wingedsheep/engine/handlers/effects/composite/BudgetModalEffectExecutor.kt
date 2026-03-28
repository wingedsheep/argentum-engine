package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.BudgetModalEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for BudgetModalEffect.
 *
 * Handles "Choose up to N {P} worth of modes" spells (Bloomburrow Season cycle).
 * Enumerates all valid combinations of mode selections within the budget,
 * presents them as options, then executes the selected combination's effects
 * in printed order via CompositeEffect.
 *
 * Flow:
 * 1. Enumerate all valid combinations (mode indices that fit within budget)
 * 2. Present as ChooseOptionDecision (with human-readable descriptions)
 * 3. Push BudgetModalContinuation
 * 4. On response, build CompositeEffect from chosen modes and execute
 */
class BudgetModalEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<BudgetModalEffect> {

    override val effectType: KClass<BudgetModalEffect> = BudgetModalEffect::class

    override fun execute(
        state: GameState,
        effect: BudgetModalEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Enumerate all valid combinations
        val combinations = enumerateValidCombinations(effect)

        if (combinations.isEmpty()) {
            // No valid combinations (shouldn't happen with budget > 0)
            return ExecutionResult.success(state, emptyList())
        }

        // Build option descriptions
        val options = combinations.map { combo -> describeCombination(combo, effect) }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Choose modes for ${sourceName ?: "budget modal spell"}",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = options
        )

        val continuation = BudgetModalContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            modes = effect.modes,
            combinations = combinations,
            opponentId = context.opponentId
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    companion object {
        /**
         * Enumerate all valid combinations of mode indices that fit within the budget.
         * Each combination is a sorted list of mode indices (allowing repeats).
         * Modes execute in printed order, so indices are sorted ascending.
         *
         * Returns at least the empty combination (choose no modes).
         */
        fun enumerateValidCombinations(effect: BudgetModalEffect): List<List<Int>> {
            val results = mutableListOf<List<Int>>()

            fun recurse(startIndex: Int, remainingBudget: Int, current: List<Int>) {
                results.add(current)
                for (i in startIndex until effect.modes.size) {
                    val cost = effect.modes[i].cost
                    if (cost <= remainingBudget) {
                        recurse(i, remainingBudget - cost, current + i)
                    }
                }
            }

            recurse(0, effect.budget, emptyList())

            // Remove the empty combination — player should pick at least something
            // Actually per MTG rules, you CAN choose no modes, but it's a waste.
            // Keep all combinations including empty for correctness.
            return results
        }

        /**
         * Build a human-readable description of a combination.
         */
        fun describeCombination(combo: List<Int>, effect: BudgetModalEffect): String {
            if (combo.isEmpty()) return "No modes (0 pawprints)"

            val totalCost = combo.sumOf { effect.modes[it].cost }

            // Group by mode index and count occurrences
            val grouped = combo.groupBy { it }
            val parts = grouped.entries.sortedBy { it.key }.map { (modeIndex, occurrences) ->
                val mode = effect.modes[modeIndex]
                val costStr = "\u2022".repeat(mode.cost) // bullet dots for pawprint visual
                if (occurrences.size > 1) {
                    "$costStr ${mode.description} (x${occurrences.size})"
                } else {
                    "$costStr ${mode.description}"
                }
            }

            return parts.joinToString(" + ") + " ($totalCost pawprints)"
        }
    }
}
