package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseModeDecision
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.DamageAssignmentResponse
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.ModesChosenResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse

/**
 * Decisions with exactly one legal answer.
 *
 * "Trivial" here means *forced*, not *easy*: a single legal target, a forced card selection, a mode
 * that is the only available one. Answering these needs no strategy and no simulation, so both the
 * strategic path ([GameSimulator], on the way to a quiet state) and the rollout path
 * ([com.wingedsheep.ai.engine.rollout.FastDecisionResponder], inside a playout) start here and only
 * fall through to their own policy when the choice is real.
 *
 * Extracted from `GameSimulator` in Phase 7 unchanged, rather than reimplemented: a playout that
 * answered a forced decision differently from the simulator would make rollout scores incomparable
 * with the static ones they replace, for no benefit.
 */
object TrivialDecisions {

    /** The forced response to [decision], or null when the choice is a real one. */
    fun responseFor(decision: PendingDecision): DecisionResponse? = when (decision) {
        // Single target, single requirement → auto-select
        is ChooseTargetsDecision -> {
            val allSingle = decision.targetRequirements.all { req ->
                val targets = decision.legalTargets[req.index] ?: emptyList()
                targets.size == 1 && req.minTargets == 1 && req.maxTargets == 1
            }
            if (allSingle) {
                TargetsResponse(
                    decisionId = decision.id,
                    selectedTargets = decision.targetRequirements.associate { req ->
                        req.index to decision.legalTargets[req.index]!!
                    }
                )
            } else null
        }

        // Forced card selection (min == max == options.size)
        is SelectCardsDecision -> {
            if (decision.minSelections == decision.options.size &&
                decision.maxSelections == decision.options.size
            ) {
                CardsSelectedResponse(decision.id, decision.options)
            } else null
        }

        // Damage assignment with defaults
        is AssignDamageDecision -> {
            if (decision.defaultAssignments.isNotEmpty()) {
                DamageAssignmentResponse(decision.id, decision.defaultAssignments)
            } else null
        }

        // Mana sources — auto-pay is trivial only when the solver actually found a
        // solution. When autoPaySuggestion is empty (e.g. the only available mana
        // requires sacrificing a Treasure), autoPay=true errors and the resumer
        // would re-prompt the same decision; fall through so the pluggable
        // resolver (DecisionResponder.respondManaSelection) handles it.
        is SelectManaSourcesDecision -> {
            if (decision.autoPaySuggestion.isNotEmpty()) {
                ManaSourcesSelectedResponse(decision.id, autoPay = true)
            } else null
        }

        // Single option
        is ChooseOptionDecision -> {
            if (decision.options.size == 1) {
                OptionChosenResponse(decision.id, 0)
            } else null
        }

        // Single color
        is ChooseColorDecision -> {
            if (decision.availableColors.size == 1) {
                ColorChosenResponse(decision.id, decision.availableColors.first())
            } else null
        }

        // Single mode, min==max==1
        is ChooseModeDecision -> {
            val available = decision.modes.filter { it.available }
            if (available.size == 1 && decision.minModes == 1) {
                ModesChosenResponse(decision.id, listOf(available.first().index))
            } else null
        }

        // Number with single valid value
        is ChooseNumberDecision -> {
            if (decision.minValue == decision.maxValue) {
                NumberChosenResponse(decision.id, decision.minValue)
            } else null
        }

        // Single object ordering
        is OrderObjectsDecision -> {
            if (decision.objects.size <= 1) {
                OrderedResponse(decision.id, decision.objects)
            } else null
        }

        // Library reordering with single card
        is ReorderLibraryDecision -> {
            if (decision.cards.size <= 1) {
                OrderedResponse(decision.id, decision.cards)
            } else null
        }

        else -> null
    }
}
