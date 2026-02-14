package com.wingedsheep.engine.handlers.actions.decision

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
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.ModesChosenResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.PilesSplitResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SplitPilesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse

/**
 * Validators for different types of decision responses.
 *
 * Each decision type requires specific validation to ensure
 * the player's response is legal and complete.
 */
object DecisionValidators {

    /**
     * Validate a decision response against its pending decision.
     *
     * @param decision The pending decision requiring a response
     * @param response The player's response
     * @return An error message if invalid, null if valid
     */
    fun validate(decision: PendingDecision, response: DecisionResponse): String? {
        return when (decision) {
            is ChooseTargetsDecision -> validateTargets(decision, response)
            is SelectCardsDecision -> validateSelectCards(decision, response)
            is YesNoDecision -> validateYesNo(response)
            is ChooseModeDecision -> validateModes(decision, response)
            is ChooseColorDecision -> validateColor(decision, response)
            is ChooseNumberDecision -> validateNumber(decision, response)
            is DistributeDecision -> validateDistribute(decision, response)
            is OrderObjectsDecision -> validateOrder(decision, response)
            is SplitPilesDecision -> validateSplitPiles(decision, response)
            is ChooseOptionDecision -> validateOption(decision, response)
            is AssignDamageDecision -> validateDamageAssignment(decision, response)
            is SearchLibraryDecision -> validateLibrarySearch(decision, response)
            is ReorderLibraryDecision -> validateLibraryReorder(decision, response)
            is SelectManaSourcesDecision -> validateManaSourcesSelection(response)
        }
    }

    private fun validateTargets(decision: ChooseTargetsDecision, response: DecisionResponse): String? {
        if (response !is TargetsResponse) {
            return "Expected target selection response"
        }

        for ((reqIndex, selectedIds) in response.selectedTargets) {
            val legalForReq = decision.legalTargets[reqIndex] ?: emptyList()
            for (id in selectedIds) {
                if (id !in legalForReq) {
                    return "Invalid target: $id is not a legal choice for requirement $reqIndex"
                }
            }

            val req = decision.targetRequirements.find { it.index == reqIndex }
            if (req != null) {
                if (selectedIds.size < req.minTargets) {
                    return "Not enough targets for requirement $reqIndex: need at least ${req.minTargets}"
                }
                if (selectedIds.size > req.maxTargets) {
                    return "Too many targets for requirement $reqIndex: maximum is ${req.maxTargets}"
                }
            }
        }
        return null
    }

    private fun validateSelectCards(decision: SelectCardsDecision, response: DecisionResponse): String? {
        if (response !is CardsSelectedResponse) {
            return "Expected card selection response"
        }

        for (cardId in response.selectedCards) {
            if (cardId !in decision.options) {
                return "Invalid selection: $cardId is not a valid option"
            }
        }
        if (response.selectedCards.size < decision.minSelections) {
            return "Not enough cards selected: need at least ${decision.minSelections}"
        }
        if (response.selectedCards.size > decision.maxSelections) {
            return "Too many cards selected: maximum is ${decision.maxSelections}"
        }
        return null
    }

    private fun validateYesNo(response: DecisionResponse): String? {
        if (response !is YesNoResponse) {
            return "Expected yes/no response"
        }
        return null
    }

    private fun validateModes(decision: ChooseModeDecision, response: DecisionResponse): String? {
        if (response !is ModesChosenResponse) {
            return "Expected mode selection response"
        }

        for (modeIndex in response.selectedModes) {
            val mode = decision.modes.find { it.index == modeIndex }
            if (mode == null) {
                return "Invalid mode index: $modeIndex"
            }
            if (!mode.available) {
                return "Mode $modeIndex is not available"
            }
        }
        if (response.selectedModes.size < decision.minModes) {
            return "Not enough modes selected: need at least ${decision.minModes}"
        }
        if (response.selectedModes.size > decision.maxModes) {
            return "Too many modes selected: maximum is ${decision.maxModes}"
        }
        return null
    }

    private fun validateColor(decision: ChooseColorDecision, response: DecisionResponse): String? {
        if (response !is ColorChosenResponse) {
            return "Expected color choice response"
        }
        if (response.color !in decision.availableColors) {
            return "Invalid color: ${response.color} is not available"
        }
        return null
    }

    private fun validateNumber(decision: ChooseNumberDecision, response: DecisionResponse): String? {
        if (response !is NumberChosenResponse) {
            return "Expected number choice response"
        }
        if (response.number < decision.minValue || response.number > decision.maxValue) {
            return "Invalid number: must be between ${decision.minValue} and ${decision.maxValue}"
        }
        return null
    }

    private fun validateDistribute(decision: DistributeDecision, response: DecisionResponse): String? {
        if (response !is DistributionResponse) {
            return "Expected distribution response"
        }

        val total = response.distribution.values.sum()
        if (total != decision.totalAmount) {
            return "Distribution must total ${decision.totalAmount}, got $total"
        }

        for ((targetId, amount) in response.distribution) {
            if (targetId !in decision.targets) {
                return "Invalid target for distribution: $targetId"
            }
            if (amount < decision.minPerTarget) {
                return "Each target must receive at least ${decision.minPerTarget}"
            }
            val maxForTarget = decision.maxPerTarget[targetId]
            if (maxForTarget != null && amount > maxForTarget) {
                return "Target $targetId cannot receive more than $maxForTarget"
            }
        }
        return null
    }

    private fun validateOrder(decision: OrderObjectsDecision, response: DecisionResponse): String? {
        if (response !is OrderedResponse) {
            return "Expected ordering response"
        }
        if (response.orderedObjects.toSet() != decision.objects.toSet()) {
            return "Ordered objects must contain exactly the same objects as the decision"
        }
        return null
    }

    private fun validateSplitPiles(decision: SplitPilesDecision, response: DecisionResponse): String? {
        if (response !is PilesSplitResponse) {
            return "Expected pile split response"
        }

        val allCards = response.piles.flatten().toSet()
        if (allCards != decision.cards.toSet()) {
            return "Piles must contain exactly the same cards as the decision"
        }
        if (response.piles.size != decision.numberOfPiles) {
            return "Must split into exactly ${decision.numberOfPiles} piles"
        }
        return null
    }

    private fun validateOption(decision: ChooseOptionDecision, response: DecisionResponse): String? {
        if (response !is OptionChosenResponse) {
            return "Expected option choice response"
        }
        if (response.optionIndex < 0 || response.optionIndex >= decision.options.size) {
            return "Invalid option index: ${response.optionIndex}"
        }
        return null
    }

    private fun validateDamageAssignment(decision: AssignDamageDecision, response: DecisionResponse): String? {
        if (response !is DamageAssignmentResponse) {
            return "Expected damage assignment response"
        }

        val totalDamage = response.assignments.values.sum()
        if (totalDamage > decision.availablePower) {
            return "Total damage ($totalDamage) exceeds available power (${decision.availablePower})"
        }

        val validTargets = decision.orderedTargets.toSet() + listOfNotNull(decision.defenderId)
        for (targetId in response.assignments.keys) {
            if (targetId !in validTargets) {
                return "Invalid damage target: $targetId"
            }
        }

        // Validate damage assignment order
        var allPreviousHaveLethal = true
        for (blockerId in decision.orderedTargets) {
            val assignedDamage = response.assignments[blockerId] ?: 0
            val lethalRequired = decision.minimumAssignments[blockerId] ?: 0
            val hasLethal = assignedDamage >= lethalRequired

            if (!hasLethal && !allPreviousHaveLethal) {
                // Fine - can assign 0 to later blockers
            } else if (!hasLethal) {
                allPreviousHaveLethal = false
            }

            // Check no subsequent blocker has damage if this one doesn't have lethal
            if (!hasLethal) {
                val laterBlockers = decision.orderedTargets.dropWhile { it != blockerId }.drop(1)
                for (laterBlocker in laterBlockers) {
                    if ((response.assignments[laterBlocker] ?: 0) > 0) {
                        return "Cannot assign damage to later blocker until earlier blockers have lethal damage"
                    }
                }
            }
        }

        // Validate trample damage
        val damageToDefender = response.assignments[decision.defenderId] ?: 0
        if (damageToDefender > 0) {
            if (!decision.hasTrample) {
                return "Cannot assign damage to defending player without trample"
            }
            if (!allPreviousHaveLethal) {
                return "Cannot assign trample damage until all blockers have lethal damage"
            }
        }
        return null
    }

    private fun validateLibrarySearch(decision: SearchLibraryDecision, response: DecisionResponse): String? {
        if (response !is CardsSelectedResponse) {
            return "Expected card selection response for library search"
        }

        for (cardId in response.selectedCards) {
            if (cardId !in decision.options) {
                return "Invalid selection: $cardId is not a valid option"
            }
        }
        if (response.selectedCards.size > decision.maxSelections) {
            return "Too many cards selected: maximum is ${decision.maxSelections}"
        }
        return null
    }

    private fun validateLibraryReorder(decision: ReorderLibraryDecision, response: DecisionResponse): String? {
        if (response !is OrderedResponse) {
            return "Expected ordered response for library reorder"
        }

        val expectedSet = decision.cards.toSet()
        val responseSet = response.orderedObjects.toSet()
        if (expectedSet != responseSet) {
            return "Invalid reorder: response must contain the same cards"
        }
        if (response.orderedObjects.size != decision.cards.size) {
            return "Invalid reorder: response must contain exactly ${decision.cards.size} cards"
        }
        return null
    }

    private fun validateManaSourcesSelection(response: DecisionResponse): String? {
        if (response !is ManaSourcesSelectedResponse) {
            return "Expected mana sources selected response"
        }
        return null
    }
}
