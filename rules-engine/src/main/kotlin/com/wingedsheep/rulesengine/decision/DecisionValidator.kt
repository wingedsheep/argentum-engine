package com.wingedsheep.rulesengine.decision

import com.wingedsheep.rulesengine.ecs.EcsGameState

/**
 * Validates that player responses are legal for the given decision.
 */
object DecisionValidator {

    /**
     * Validate that a response is legal for the given decision.
     *
     * @param state The current game state
     * @param decision The decision that was presented
     * @param response The player's response
     * @return ValidationResult indicating whether the response is valid
     */
    fun validate(state: EcsGameState, decision: PlayerDecision, response: DecisionResponse): ValidationResult {
        return when (decision) {
            is ChooseTargets -> validateTargetsChoice(state, decision, response)
            is ChooseAttackers -> validateAttackersChoice(decision, response)
            is ChooseBlockers -> validateBlockersChoice(decision, response)
            is ChooseDamageAssignmentOrder -> validateDamageAssignmentOrder(decision, response)
            is ChooseManaPayment -> validateManaPayment(decision, response)
            is YesNoDecision -> validateYesNoChoice(response)
            is ChooseCards -> validateCardsChoice(decision, response)
            is ChooseOrder<*> -> validateOrderChoice(decision, response)
            is ChooseMode -> validateModeChoice(decision, response)
            is ChooseNumber -> validateNumberChoice(decision, response)
            is PriorityDecision -> validatePriorityChoice(state, decision, response)
            is MulliganDecision -> validateMulliganChoice(response)
            is ChooseMulliganBottomCards -> validateMulliganBottomCards(decision, response)
        }
    }

    private fun validateTargetsChoice(
        state: EcsGameState,
        decision: ChooseTargets,
        response: DecisionResponse
    ): ValidationResult {
        if (response !is TargetsChoice) {
            return ValidationResult.Invalid("Expected TargetsChoice response")
        }

        // Validate each requirement has the right number of valid targets
        for ((index, requirement) in decision.requirements.withIndex()) {
            val selectedTargets = response.selectedTargets[index] ?: emptyList()
            val legalTargets = decision.legalTargets[index] ?: emptyList()

            // Check count
            if (requirement.optional) {
                if (selectedTargets.size > requirement.count) {
                    return ValidationResult.Invalid(
                        "Too many targets selected for requirement $index: ${selectedTargets.size} > ${requirement.count}"
                    )
                }
            } else {
                if (selectedTargets.size != requirement.count) {
                    return ValidationResult.Invalid(
                        "Wrong number of targets for requirement $index: expected ${requirement.count}, got ${selectedTargets.size}"
                    )
                }
            }

            // Check each target is legal
            for (target in selectedTargets) {
                if (target !in legalTargets) {
                    return ValidationResult.Invalid("Invalid target selected: $target")
                }
            }
        }

        return ValidationResult.Valid
    }

    private fun validateAttackersChoice(
        decision: ChooseAttackers,
        response: DecisionResponse
    ): ValidationResult {
        if (response !is AttackersChoice) {
            return ValidationResult.Invalid("Expected AttackersChoice response")
        }

        for (attackerId in response.attackerIds) {
            if (attackerId !in decision.legalAttackers) {
                return ValidationResult.Invalid("Creature cannot attack: $attackerId")
            }
        }

        // Check for duplicates
        if (response.attackerIds.size != response.attackerIds.distinct().size) {
            return ValidationResult.Invalid("Duplicate attackers selected")
        }

        return ValidationResult.Valid
    }

    private fun validateBlockersChoice(
        decision: ChooseBlockers,
        response: DecisionResponse
    ): ValidationResult {
        if (response !is BlockersChoice) {
            return ValidationResult.Invalid("Expected BlockersChoice response")
        }

        for ((blockerId, attackerId) in response.blocks) {
            // Check blocker is legal
            if (blockerId !in decision.legalBlockers) {
                return ValidationResult.Invalid("Creature cannot block: $blockerId")
            }

            // Check attacker exists
            if (attackerId !in decision.attackers) {
                return ValidationResult.Invalid("Invalid attacker: $attackerId")
            }

            // Check this blocker can legally block this attacker
            val legalAttackers = decision.legalBlocks[blockerId] ?: emptyList()
            if (attackerId !in legalAttackers) {
                return ValidationResult.Invalid("$blockerId cannot legally block $attackerId")
            }
        }

        return ValidationResult.Valid
    }

    private fun validateDamageAssignmentOrder(
        decision: ChooseDamageAssignmentOrder,
        response: DecisionResponse
    ): ValidationResult {
        if (response !is DamageAssignmentOrderChoice) {
            return ValidationResult.Invalid("Expected DamageAssignmentOrderChoice response")
        }

        // Must include all blockers exactly once
        if (response.orderedBlockerIds.size != decision.blockerIds.size) {
            return ValidationResult.Invalid(
                "Wrong number of blockers: expected ${decision.blockerIds.size}, got ${response.orderedBlockerIds.size}"
            )
        }

        if (response.orderedBlockerIds.toSet() != decision.blockerIds.toSet()) {
            return ValidationResult.Invalid("Blockers don't match")
        }

        return ValidationResult.Valid
    }

    private fun validateManaPayment(
        decision: ChooseManaPayment,
        response: DecisionResponse
    ): ValidationResult {
        if (response !is ManaPaymentChoice) {
            return ValidationResult.Invalid("Expected ManaPaymentChoice response")
        }

        // Check that total for generic equals required generic
        if (response.totalForGeneric != decision.requiredGeneric) {
            return ValidationResult.Invalid(
                "Mana for generic doesn't match: need ${decision.requiredGeneric}, got ${response.totalForGeneric}"
            )
        }

        // Check that player has enough mana of each color
        val availableWhite = decision.availableMana[com.wingedsheep.rulesengine.core.Color.WHITE] ?: 0
        val availableBlue = decision.availableMana[com.wingedsheep.rulesengine.core.Color.BLUE] ?: 0
        val availableBlack = decision.availableMana[com.wingedsheep.rulesengine.core.Color.BLACK] ?: 0
        val availableRed = decision.availableMana[com.wingedsheep.rulesengine.core.Color.RED] ?: 0
        val availableGreen = decision.availableMana[com.wingedsheep.rulesengine.core.Color.GREEN] ?: 0

        val totalWhiteNeeded = decision.requiredWhite + response.whiteForGeneric
        val totalBlueNeeded = decision.requiredBlue + response.blueForGeneric
        val totalBlackNeeded = decision.requiredBlack + response.blackForGeneric
        val totalRedNeeded = decision.requiredRed + response.redForGeneric
        val totalGreenNeeded = decision.requiredGreen + response.greenForGeneric
        val totalColorlessNeeded = decision.requiredColorless + response.colorlessForGeneric

        if (totalWhiteNeeded > availableWhite) {
            return ValidationResult.Invalid("Not enough white mana")
        }
        if (totalBlueNeeded > availableBlue) {
            return ValidationResult.Invalid("Not enough blue mana")
        }
        if (totalBlackNeeded > availableBlack) {
            return ValidationResult.Invalid("Not enough black mana")
        }
        if (totalRedNeeded > availableRed) {
            return ValidationResult.Invalid("Not enough red mana")
        }
        if (totalGreenNeeded > availableGreen) {
            return ValidationResult.Invalid("Not enough green mana")
        }
        if (totalColorlessNeeded > decision.availableColorless) {
            return ValidationResult.Invalid("Not enough colorless mana")
        }

        return ValidationResult.Valid
    }

    private fun validateYesNoChoice(response: DecisionResponse): ValidationResult {
        if (response !is YesNoChoice) {
            return ValidationResult.Invalid("Expected YesNoChoice response")
        }
        return ValidationResult.Valid
    }

    private fun validateCardsChoice(
        decision: ChooseCards,
        response: DecisionResponse
    ): ValidationResult {
        if (response !is CardsChoice) {
            return ValidationResult.Invalid("Expected CardsChoice response")
        }

        // Check count
        if (response.selectedCardIds.size < decision.minCount) {
            return ValidationResult.Invalid(
                "Too few cards selected: ${response.selectedCardIds.size} < ${decision.minCount}"
            )
        }
        if (response.selectedCardIds.size > decision.maxCount) {
            return ValidationResult.Invalid(
                "Too many cards selected: ${response.selectedCardIds.size} > ${decision.maxCount}"
            )
        }

        // Check all selected cards are valid options
        for (cardId in response.selectedCardIds) {
            if (cardId !in decision.cards) {
                return ValidationResult.Invalid("Invalid card selected: $cardId")
            }
        }

        // Check for duplicates
        if (response.selectedCardIds.size != response.selectedCardIds.distinct().size) {
            return ValidationResult.Invalid("Duplicate cards selected")
        }

        return ValidationResult.Valid
    }

    private fun validateOrderChoice(
        decision: ChooseOrder<*>,
        response: DecisionResponse
    ): ValidationResult {
        if (response !is OrderChoice) {
            return ValidationResult.Invalid("Expected OrderChoice response")
        }

        // Must include all indices exactly once
        if (response.orderedIndices.size != decision.items.size) {
            return ValidationResult.Invalid(
                "Wrong number of items: expected ${decision.items.size}, got ${response.orderedIndices.size}"
            )
        }

        val expectedIndices = decision.items.indices.toSet()
        if (response.orderedIndices.toSet() != expectedIndices) {
            return ValidationResult.Invalid("Indices don't match expected range")
        }

        return ValidationResult.Valid
    }

    private fun validateModeChoice(
        decision: ChooseMode,
        response: DecisionResponse
    ): ValidationResult {
        if (response !is ModeChoice) {
            return ValidationResult.Invalid("Expected ModeChoice response")
        }

        // Check count
        if (response.selectedModeIndices.size < decision.minModes) {
            return ValidationResult.Invalid(
                "Too few modes selected: ${response.selectedModeIndices.size} < ${decision.minModes}"
            )
        }
        if (response.selectedModeIndices.size > decision.maxModes) {
            return ValidationResult.Invalid(
                "Too many modes selected: ${response.selectedModeIndices.size} > ${decision.maxModes}"
            )
        }

        // Check all selected modes are valid and available
        for (modeIndex in response.selectedModeIndices) {
            val mode = decision.modes.getOrNull(modeIndex)
            if (mode == null) {
                return ValidationResult.Invalid("Invalid mode index: $modeIndex")
            }
            if (!mode.isAvailable) {
                return ValidationResult.Invalid("Mode is not available: ${mode.description}")
            }
        }

        // Check for duplicates (unless repeating is allowed)
        if (!decision.canRepeatModes) {
            if (response.selectedModeIndices.size != response.selectedModeIndices.distinct().size) {
                return ValidationResult.Invalid("Cannot repeat modes")
            }
        }

        return ValidationResult.Valid
    }

    private fun validateNumberChoice(
        decision: ChooseNumber,
        response: DecisionResponse
    ): ValidationResult {
        if (response !is NumberChoice) {
            return ValidationResult.Invalid("Expected NumberChoice response")
        }

        if (response.number < decision.minimum) {
            return ValidationResult.Invalid(
                "Number too low: ${response.number} < ${decision.minimum}"
            )
        }
        if (response.number > decision.maximum) {
            return ValidationResult.Invalid(
                "Number too high: ${response.number} > ${decision.maximum}"
            )
        }

        return ValidationResult.Valid
    }

    private fun validatePriorityChoice(
        state: EcsGameState,
        decision: PriorityDecision,
        response: DecisionResponse
    ): ValidationResult {
        if (response !is PriorityChoice) {
            return ValidationResult.Invalid("Expected PriorityChoice response")
        }

        return when (response) {
            is PriorityChoice.Pass -> ValidationResult.Valid
            is PriorityChoice.CastSpell -> {
                if (!decision.canCastSpells) {
                    ValidationResult.Invalid("Cannot cast spells right now")
                } else {
                    // Additional validation would check if the card is in hand, castable, etc.
                    ValidationResult.Valid
                }
            }
            is PriorityChoice.ActivateAbility -> {
                if (!decision.canActivateAbilities) {
                    ValidationResult.Invalid("Cannot activate abilities right now")
                } else {
                    ValidationResult.Valid
                }
            }
            is PriorityChoice.PlayLand -> {
                if (!decision.canPlayLand) {
                    ValidationResult.Invalid("Cannot play a land right now")
                } else {
                    ValidationResult.Valid
                }
            }
        }
    }

    private fun validateMulliganChoice(response: DecisionResponse): ValidationResult {
        if (response !is MulliganChoice) {
            return ValidationResult.Invalid("Expected MulliganChoice response")
        }
        return ValidationResult.Valid
    }

    private fun validateMulliganBottomCards(
        decision: ChooseMulliganBottomCards,
        response: DecisionResponse
    ): ValidationResult {
        if (response !is CardsChoice) {
            return ValidationResult.Invalid("Expected CardsChoice response")
        }

        // Must choose exactly the required number of cards
        if (response.selectedCardIds.size != decision.cardsToPutOnBottom) {
            return ValidationResult.Invalid(
                "Must choose exactly ${decision.cardsToPutOnBottom} cards, got ${response.selectedCardIds.size}"
            )
        }

        // All selected cards must be in hand
        for (cardId in response.selectedCardIds) {
            if (cardId !in decision.hand) {
                return ValidationResult.Invalid("Card not in hand: $cardId")
            }
        }

        // No duplicates
        if (response.selectedCardIds.size != response.selectedCardIds.distinct().size) {
            return ValidationResult.Invalid("Duplicate cards selected")
        }

        return ValidationResult.Valid
    }

    /**
     * Result of validating a decision response.
     */
    sealed interface ValidationResult {
        data object Valid : ValidationResult

        data class Invalid(val reason: String) : ValidationResult

        val isValid: Boolean get() = this is Valid
    }
}
