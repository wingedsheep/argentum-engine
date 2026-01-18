package com.wingedsheep.rulesengine.targeting

import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.PlayerId

/**
 * Validates targets for spells and abilities, both when selecting and on resolution.
 */
object TargetValidator {

    /**
     * Result of target validation.
     */
    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val reason: String) : ValidationResult
        data object Fizzle : ValidationResult  // All targets are now illegal
    }

    /**
     * Check if a single target is valid for a requirement.
     */
    fun validateTarget(
        target: Target,
        requirement: TargetRequirement,
        state: GameState,
        sourceControllerId: PlayerId,
        sourceId: CardId?
    ): ValidationResult {
        return if (requirement.isValidTarget(target, state, sourceControllerId, sourceId)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Target is not valid for requirement: ${requirement.description}")
        }
    }

    /**
     * Validate all targets in a selection.
     */
    fun validateSelection(
        selection: TargetSelection,
        state: GameState,
        sourceControllerId: PlayerId,
        sourceId: CardId?
    ): ValidationResult {
        for ((index, requirement) in selection.requirements.withIndex()) {
            val targets = selection.selectedTargets[index] ?: emptyList()

            // Check if we have enough targets
            if (!requirement.optional && targets.size < requirement.count) {
                return ValidationResult.Invalid(
                    "Not enough targets selected for: ${requirement.description} " +
                    "(need ${requirement.count}, have ${targets.size})"
                )
            }

            // Check if we have too many targets
            if (targets.size > requirement.count) {
                return ValidationResult.Invalid(
                    "Too many targets selected for: ${requirement.description} " +
                    "(max ${requirement.count}, have ${targets.size})"
                )
            }

            // Validate each target
            for (target in targets) {
                val result = validateTarget(target, requirement, state, sourceControllerId, sourceId)
                if (result !is ValidationResult.Valid) {
                    return result
                }
            }
        }

        return ValidationResult.Valid
    }

    /**
     * Validate targets on resolution (for fizzle checking).
     * Returns Fizzle if ALL targets are now illegal.
     * Returns Valid if at least one target is still legal (spell resolves with remaining valid targets).
     */
    fun validateOnResolution(
        selection: TargetSelection,
        state: GameState,
        sourceControllerId: PlayerId,
        sourceId: CardId?
    ): ResolutionValidation {
        if (selection.requirements.isEmpty()) {
            // No targeting requirements = always resolves
            return ResolutionValidation(valid = true, validTargets = emptyMap(), shouldFizzle = false)
        }

        val validTargets = mutableMapOf<Int, List<Target>>()
        var hasAnyValidTarget = false

        for ((index, requirement) in selection.requirements.withIndex()) {
            val targets = selection.selectedTargets[index] ?: emptyList()
            val stillValid = targets.filter { target ->
                requirement.isValidTarget(target, state, sourceControllerId, sourceId)
            }

            validTargets[index] = stillValid
            if (stillValid.isNotEmpty()) {
                hasAnyValidTarget = true
            }
        }

        // Check if we should fizzle
        // A spell fizzles if ALL of its targets are illegal when it tries to resolve
        val shouldFizzle = selection.allTargets.isNotEmpty() && !hasAnyValidTarget

        return ResolutionValidation(
            valid = hasAnyValidTarget || selection.allTargets.isEmpty(),
            validTargets = validTargets,
            shouldFizzle = shouldFizzle
        )
    }

    /**
     * Get all legal targets for a requirement in the current game state.
     */
    fun getLegalTargets(
        requirement: TargetRequirement,
        state: GameState,
        sourceControllerId: PlayerId,
        sourceId: CardId?
    ): List<Target> {
        val legalTargets = mutableListOf<Target>()

        // Check players
        for ((playerId, _) in state.players) {
            val target = Target.PlayerTarget(EntityId.fromPlayerId(playerId))
            if (requirement.isValidTarget(target, state, sourceControllerId, sourceId)) {
                legalTargets.add(target)
            }
        }

        // Check battlefield
        for (card in state.battlefield.cards) {
            val target = Target.CardTarget(card.id)
            if (requirement.isValidTarget(target, state, sourceControllerId, sourceId)) {
                legalTargets.add(target)
            }
        }

        // Check stack
        for (card in state.stack.cards) {
            val target = Target.CardTarget(card.id)
            if (requirement.isValidTarget(target, state, sourceControllerId, sourceId)) {
                legalTargets.add(target)
            }
        }

        // Check graveyards
        for ((_, player) in state.players) {
            for (card in player.graveyard.cards) {
                val target = Target.CardTarget(card.id)
                if (requirement.isValidTarget(target, state, sourceControllerId, sourceId)) {
                    legalTargets.add(target)
                }
            }
        }

        return legalTargets
    }

    /**
     * Check if any legal targets exist for a requirement.
     */
    fun hasLegalTargets(
        requirement: TargetRequirement,
        state: GameState,
        sourceControllerId: PlayerId,
        sourceId: CardId?
    ): Boolean {
        // Check players first (usually faster)
        for ((playerId, _) in state.players) {
            val target = Target.PlayerTarget(EntityId.fromPlayerId(playerId))
            if (requirement.isValidTarget(target, state, sourceControllerId, sourceId)) {
                return true
            }
        }

        // Check battlefield
        for (card in state.battlefield.cards) {
            val target = Target.CardTarget(card.id)
            if (requirement.isValidTarget(target, state, sourceControllerId, sourceId)) {
                return true
            }
        }

        // Check stack
        for (card in state.stack.cards) {
            val target = Target.CardTarget(card.id)
            if (requirement.isValidTarget(target, state, sourceControllerId, sourceId)) {
                return true
            }
        }

        // Check graveyards
        for ((_, player) in state.players) {
            for (card in player.graveyard.cards) {
                val target = Target.CardTarget(card.id)
                if (requirement.isValidTarget(target, state, sourceControllerId, sourceId)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Check if a spell/ability can be cast/activated given its targeting requirements.
     * Returns false if any mandatory targeting requirement has no legal targets.
     */
    fun canTarget(
        requirements: List<TargetRequirement>,
        state: GameState,
        sourceControllerId: PlayerId,
        sourceId: CardId?
    ): Boolean {
        for (requirement in requirements) {
            if (!requirement.optional && !hasLegalTargets(requirement, state, sourceControllerId, sourceId)) {
                return false
            }
        }
        return true
    }
}

/**
 * Result of validating targets on resolution.
 */
data class ResolutionValidation(
    val valid: Boolean,
    val validTargets: Map<Int, List<Target>>,
    val shouldFizzle: Boolean
) {
    /**
     * Get all still-valid targets as a flat list.
     */
    val allValidTargets: List<Target>
        get() = validTargets.values.flatten()
}

/**
 * Extension function to check if a card can be targeted (not hexproof/shroud from opponent).
 * This is a basic implementation - full hexproof/shroud would need more context.
 */
fun CardInstance.canBeTargetedBy(sourceControllerId: PlayerId): Boolean {
    // Shroud: Can't be targeted by any spell or ability
    if (hasKeyword(com.wingedsheep.rulesengine.core.Keyword.SHROUD)) {
        return false
    }

    // Hexproof: Can't be targeted by opponents' spells or abilities
    if (hasKeyword(com.wingedsheep.rulesengine.core.Keyword.HEXPROOF)) {
        return controllerId == sourceControllerId.value
    }

    return true
}
