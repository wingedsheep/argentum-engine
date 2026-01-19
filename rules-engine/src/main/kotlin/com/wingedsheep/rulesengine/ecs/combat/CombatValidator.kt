package com.wingedsheep.rulesengine.ecs.combat

import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.layers.GameObjectView
import com.wingedsheep.rulesengine.ecs.layers.ModifierProvider
import com.wingedsheep.rulesengine.ecs.layers.StateProjector
import com.wingedsheep.rulesengine.game.Step

/**
 * ECS-based combat validator that uses the layer system for accurate state checks.
 *
 * This validator ensures all combat rules are properly enforced including:
 * - Basic attack/block requirements (untapped, no summoning sickness, etc.)
 * - Evasion abilities (flying, shadow, menace, fear, intimidate, horsemanship)
 * - Combat restriction components (CantAttack, CantBlock, MustAttack, etc.)
 * - Taxing effects (Propaganda-style "pay to attack" costs)
 *
 * All keyword checks use the StateProjector to get the post-layer state,
 * ensuring effects like "loses all abilities" are properly respected.
 */
object CombatValidator {

    // ==========================================================================
    // Validation Results
    // ==========================================================================

    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val reason: String) : ValidationResult

        /**
         * Creature can attack but requires paying a cost.
         */
        data class RequiresCost(
            val costs: List<AttackCost>
        ) : ValidationResult
    }

    /**
     * Represents a cost that must be paid to attack.
     */
    data class AttackCost(
        val sourceId: EntityId,
        val manaCost: String
    )

    /**
     * Represents a cost that must be paid to block.
     */
    data class BlockCost(
        val sourceId: EntityId,
        val manaCost: String
    )

    sealed interface BlockValidationResult {
        data object Valid : BlockValidationResult
        data class Invalid(val reason: String) : BlockValidationResult
        data class RequiresCost(val costs: List<BlockCost>) : BlockValidationResult
    }

    // ==========================================================================
    // Attack Validation
    // ==========================================================================

    /**
     * Checks if a creature can be declared as an attacker.
     *
     * Uses the StateProjector to get the post-layer state of the creature,
     * ensuring continuous effects are properly applied.
     *
     * @param state The current game state
     * @param creatureId The creature attempting to attack
     * @param playerId The player declaring the attack
     * @param modifierProvider Optional provider for continuous effects
     */
    fun canDeclareAttacker(
        state: GameState,
        creatureId: EntityId,
        playerId: EntityId,
        modifierProvider: ModifierProvider? = null
    ): ValidationResult {
        // Must be in declare attackers step
        if (state.turnState.step != Step.DECLARE_ATTACKERS) {
            return ValidationResult.Invalid("Can only declare attackers during declare attackers step")
        }

        // Must be active player
        if (state.turnState.activePlayer != playerId) {
            return ValidationResult.Invalid("Only the active player can declare attackers")
        }

        // Get the creature's projected state
        val projector = StateProjector.forState(state, modifierProvider)
        val creature = projector.getView(creatureId)
            ?: return ValidationResult.Invalid("Creature not found on battlefield")

        // Must be a creature
        if (!creature.isCreature) {
            return ValidationResult.Invalid("Only creatures can attack")
        }

        // Must control the creature
        if (creature.controllerId != playerId) {
            return ValidationResult.Invalid("You don't control this creature")
        }

        // Check explicit CantAttackComponent (highest priority restriction)
        if (state.hasComponent<CantAttackComponent>(creatureId)) {
            return ValidationResult.Invalid("This creature cannot attack")
        }

        // Must be untapped
        if (creature.isTapped) {
            return ValidationResult.Invalid("Tapped creatures cannot attack")
        }

        // Must not have summoning sickness (unless has haste)
        if (creature.hasSummoningSickness && !creature.hasKeyword(Keyword.HASTE)) {
            return ValidationResult.Invalid("Creature has summoning sickness")
        }

        // Must not have defender
        if (creature.hasKeyword(Keyword.DEFENDER)) {
            return ValidationResult.Invalid("Creatures with defender cannot attack")
        }

        // Must not already be attacking
        if (state.hasComponent<AttackingComponent>(creatureId)) {
            return ValidationResult.Invalid("Creature is already attacking")
        }

        // Check for attack costs (Propaganda-style effects)
        val attackCosts = collectAttackCosts(state, creatureId, playerId)
        if (attackCosts.isNotEmpty()) {
            return ValidationResult.RequiresCost(attackCosts)
        }

        return ValidationResult.Valid
    }

    /**
     * Collect any costs required to attack with this creature.
     *
     * These come from AttackCostComponent on the creature itself
     * (typically added by effects like Propaganda on the defending player's side).
     */
    private fun collectAttackCosts(
        state: GameState,
        creatureId: EntityId,
        playerId: EntityId
    ): List<AttackCost> {
        val costs = mutableListOf<AttackCost>()

        // Check for AttackCostComponent on the creature
        state.getComponent<AttackCostComponent>(creatureId)?.let { component ->
            costs.add(AttackCost(component.sourceId, component.manaCost))
        }

        // Future: Could also check global effects on the defending player's permanents
        // that impose attack costs (like Propaganda, Ghostly Prison)

        return costs
    }

    // ==========================================================================
    // Block Validation
    // ==========================================================================

    /**
     * Checks if a creature can block another creature.
     *
     * This validates:
     * - Basic blocking requirements
     * - Evasion abilities on the attacker
     * - Blocking restrictions on the blocker
     * - Timing: creature must have been on battlefield when declare blockers step began
     */
    fun canDeclareBlocker(
        state: GameState,
        blockerId: EntityId,
        attackerId: EntityId,
        playerId: EntityId,
        modifierProvider: ModifierProvider? = null
    ): BlockValidationResult {
        // Must be in declare blockers step
        if (state.turnState.step != Step.DECLARE_BLOCKERS) {
            return BlockValidationResult.Invalid("Can only declare blockers during declare blockers step")
        }

        // Must be defending player
        val combat = state.combat
            ?: return BlockValidationResult.Invalid("Not in combat")

        if (combat.defendingPlayer != playerId) {
            return BlockValidationResult.Invalid("Only the defending player can declare blockers")
        }

        // Check if creature is eligible to block (was on battlefield when declare blockers began)
        // MTG Rule 509.1a: A creature that enters the battlefield after blockers are declared
        // cannot be declared as a blocker.
        if (!combat.isEligibleBlocker(blockerId)) {
            return BlockValidationResult.Invalid("Creature entered the battlefield after blockers were declared and cannot block")
        }

        // Get projected views for both creatures
        val projector = StateProjector.forState(state, modifierProvider)

        val blocker = projector.getView(blockerId)
            ?: return BlockValidationResult.Invalid("Blocking creature not found on battlefield")

        val attacker = projector.getView(attackerId)
            ?: return BlockValidationResult.Invalid("Attacking creature not found on battlefield")

        // Blocker must be a creature
        if (!blocker.isCreature) {
            return BlockValidationResult.Invalid("Only creatures can block")
        }

        // Attacker must be a creature
        if (!attacker.isCreature) {
            return BlockValidationResult.Invalid("Target is not a creature")
        }

        // Must control the blocker
        if (blocker.controllerId != playerId) {
            return BlockValidationResult.Invalid("You don't control this creature")
        }

        // Check explicit CantBlockComponent
        if (state.hasComponent<CantBlockComponent>(blockerId)) {
            return BlockValidationResult.Invalid("This creature cannot block")
        }

        // Check restriction from static abilities (e.g. Jungle Lion "This creature can't block")
        // This comes from Modifications applied by the StateProjector
        if (blocker.cantBlock) {
            return BlockValidationResult.Invalid("This creature cannot block")
        }

        // Blocker must be untapped
        if (blocker.isTapped) {
            return BlockValidationResult.Invalid("Tapped creatures cannot block")
        }

        // Attacker must be attacking
        if (!state.hasComponent<AttackingComponent>(attackerId)) {
            return BlockValidationResult.Invalid("Target creature is not attacking")
        }

        // Check if attacker can be blocked at all
        if (state.hasComponent<CantBeBlockedComponent>(attackerId)) {
            return BlockValidationResult.Invalid("This creature cannot be blocked")
        }

        // Check evasion abilities
        val evasionResult = checkEvasionAbilities(attacker, blocker)
        if (evasionResult != null) {
            return BlockValidationResult.Invalid(evasionResult)
        }

        // Check if this blocker is already blocking this attacker
        val blockingComponent = state.getComponent<BlockingComponent>(blockerId)
        if (blockingComponent?.attackerId == attackerId) {
            return BlockValidationResult.Invalid("Creature is already blocking this attacker")
        }

        // Check for block costs
        val blockCosts = collectBlockCosts(state, blockerId, attackerId)
        if (blockCosts.isNotEmpty()) {
            return BlockValidationResult.RequiresCost(blockCosts)
        }

        return BlockValidationResult.Valid
    }

    /**
     * Check evasion abilities that restrict what can block.
     *
     * Returns an error message if the blocker cannot block due to evasion,
     * or null if blocking is allowed.
     */
    private fun checkEvasionAbilities(
        attacker: GameObjectView,
        blocker: GameObjectView
    ): String? {
        // Flying: Can only be blocked by flying or reach
        if (attacker.hasKeyword(Keyword.FLYING)) {
            if (!blocker.hasKeyword(Keyword.FLYING) && !blocker.hasKeyword(Keyword.REACH)) {
                return "Cannot block a creature with flying unless blocker has flying or reach"
            }
        }

        // Shadow: Can only block/be blocked by creatures with shadow
        if (attacker.hasKeyword(Keyword.SHADOW)) {
            if (!blocker.hasKeyword(Keyword.SHADOW)) {
                return "Cannot block a creature with shadow unless blocker has shadow"
            }
        }

        // If blocker has shadow but attacker doesn't, blocker can't block
        if (blocker.hasKeyword(Keyword.SHADOW) && !attacker.hasKeyword(Keyword.SHADOW)) {
            return "Creatures with shadow can only block other creatures with shadow"
        }

        // Fear: Can only be blocked by artifact creatures or black creatures
        if (attacker.hasKeyword(Keyword.FEAR)) {
            val isArtifactCreature = blocker.isArtifact
            val isBlack = com.wingedsheep.rulesengine.core.Color.BLACK in blocker.colors
            if (!isArtifactCreature && !isBlack) {
                return "Cannot block a creature with fear unless blocker is an artifact creature or black"
            }
        }

        // Intimidate: Can only be blocked by artifact creatures or creatures sharing a color
        if (attacker.hasKeyword(Keyword.INTIMIDATE)) {
            val isArtifactCreature = blocker.isArtifact
            val sharesColor = attacker.colors.intersect(blocker.colors).isNotEmpty()
            if (!isArtifactCreature && !sharesColor) {
                return "Cannot block a creature with intimidate unless blocker is an artifact creature or shares a color"
            }
        }

        // Horsemanship: Can only be blocked by creatures with horsemanship
        if (attacker.hasKeyword(Keyword.HORSEMANSHIP)) {
            if (!blocker.hasKeyword(Keyword.HORSEMANSHIP)) {
                return "Cannot block a creature with horsemanship unless blocker has horsemanship"
            }
        }

        // Note: Skulk would go here when that keyword is added
        // Skulk: Cannot be blocked by creatures with greater power

        return null
    }

    /**
     * Collect any costs required to block with this creature.
     */
    private fun collectBlockCosts(
        state: GameState,
        blockerId: EntityId,
        attackerId: EntityId
    ): List<BlockCost> {
        val costs = mutableListOf<BlockCost>()

        // Check for BlockCostComponent on the blocker
        state.getComponent<BlockCostComponent>(blockerId)?.let { component ->
            costs.add(BlockCost(component.sourceId, component.manaCost))
        }

        return costs
    }

    // ==========================================================================
    // Menace Validation
    // ==========================================================================

    /**
     * Validates menace requirements for all attackers.
     *
     * Menace requires at least 2 creatures to block. This must be validated
     * after all blockers are declared.
     *
     * @return List of attackers with menace that don't have enough blockers
     */
    fun validateMenaceRequirements(
        state: GameState,
        modifierProvider: ModifierProvider? = null
    ): List<EntityId> {
        if (state.combat == null) return emptyList()
        val projector = StateProjector.forState(state, modifierProvider)
        val violations = mutableListOf<EntityId>()

        // Query all entities with AttackingComponent (ECS pattern)
        for (attackerId in state.entitiesWithComponent<AttackingComponent>()) {
            val attacker = projector.getView(attackerId) ?: continue

            if (attacker.hasKeyword(Keyword.MENACE)) {
                val blockedBy = state.getComponent<BlockedByComponent>(attackerId)
                val blockerCount = blockedBy?.blockerCount ?: 0

                // Menace: if blocked at all, must be blocked by 2+
                if (blockerCount in 1..1) {
                    violations.add(attackerId)
                }
            }
        }

        return violations
    }

    // ==========================================================================
    // Combat Requirement Validation
    // ==========================================================================

    /**
     * Get all creatures that must attack but haven't been declared.
     *
     * Used to validate that required attacks have been made.
     */
    fun getCreaturesThatMustAttack(
        state: GameState,
        playerId: EntityId,
        modifierProvider: ModifierProvider? = null
    ): List<EntityId> {
        val combat = state.combat ?: return emptyList()
        val projector = StateProjector.forState(state, modifierProvider)

        return state.getCreaturesControlledBy(playerId).filter { creatureId ->
            // Has MustAttackComponent
            val mustAttack = state.hasComponent<MustAttackComponent>(creatureId)
            if (!mustAttack) return@filter false

            // Is not already attacking
            if (state.hasComponent<AttackingComponent>(creatureId)) return@filter false

            // Can actually attack (not tapped, no summoning sickness, etc.)
            val creature = projector.getView(creatureId) ?: return@filter false
            !creature.isTapped &&
                    (!creature.hasSummoningSickness || creature.hasKeyword(Keyword.HASTE)) &&
                    !creature.hasKeyword(Keyword.DEFENDER)
        }
    }

    /**
     * Get all creatures that must block but haven't been declared.
     *
     * Used to validate that required blocks have been made.
     */
    fun getCreaturesThatMustBlock(
        state: GameState,
        playerId: EntityId,
        modifierProvider: ModifierProvider? = null
    ): List<EntityId> {
        val combat = state.combat ?: return emptyList()
        val projector = StateProjector.forState(state, modifierProvider)

        return state.getCreaturesControlledBy(playerId).filter { creatureId ->
            // Has MustBlockComponent
            val mustBlock = state.hasComponent<MustBlockComponent>(creatureId)
            if (!mustBlock) return@filter false

            // Is not already blocking
            if (state.hasComponent<BlockingComponent>(creatureId)) return@filter false

            // Can actually block (not tapped)
            val creature = projector.getView(creatureId) ?: return@filter false
            !creature.isTapped
        }
    }

    /**
     * Get all attackers with MustBeBlockedComponent that are not blocked.
     *
     * Used to validate that creatures that must be blocked have blockers.
     */
    fun getUnblockedMustBeBlockedCreatures(
        state: GameState,
        modifierProvider: ModifierProvider? = null
    ): List<EntityId> {
        if (state.combat == null) return emptyList()

        // Query all attackers via ECS component
        return state.entitiesWithComponent<AttackingComponent>().filter { attackerId ->
            // Has MustBeBlockedComponent
            val mustBeBlocked = state.hasComponent<MustBeBlockedComponent>(attackerId)
            if (!mustBeBlocked) return@filter false

            // Check if blocked
            val blockedBy = state.getComponent<BlockedByComponent>(attackerId)
            blockedBy == null || !blockedBy.isBlocked
        }
    }

    /**
     * Validates that a creature CAN legally block one of the "must be blocked" attackers.
     *
     * Returns true if the blocker can legally block any attacker that must be blocked.
     */
    fun canBlockAnyMustBeBlocked(
        state: GameState,
        blockerId: EntityId,
        playerId: EntityId,
        modifierProvider: ModifierProvider? = null
    ): Boolean {
        val mustBeBlocked = getUnblockedMustBeBlockedCreatures(state, modifierProvider)
        if (mustBeBlocked.isEmpty()) return true // No must-block requirement

        // Check if this blocker can legally block any of them
        return mustBeBlocked.any { attackerId ->
            canDeclareBlocker(state, blockerId, attackerId, playerId, modifierProvider) is BlockValidationResult.Valid
        }
    }

    // ==========================================================================
    // Full Combat Validation
    // ==========================================================================

    /**
     * Validates the entire combat declaration phase.
     *
     * Returns a list of all combat violations that need to be resolved.
     */
    fun validateCombatDeclarations(
        state: GameState,
        modifierProvider: ModifierProvider? = null
    ): CombatDeclarationValidation {
        val combat = state.combat ?: return CombatDeclarationValidation.empty()

        val menaceViolations = validateMenaceRequirements(state, modifierProvider)
        val mustAttackViolations = getCreaturesThatMustAttack(state, combat.attackingPlayer, modifierProvider)
        val mustBlockViolations = getCreaturesThatMustBlock(state, combat.defendingPlayer, modifierProvider)
        val mustBeBlockedViolations = getUnblockedMustBeBlockedCreatures(state, modifierProvider)

        return CombatDeclarationValidation(
            menaceViolations = menaceViolations,
            mustAttackViolations = mustAttackViolations,
            mustBlockViolations = mustBlockViolations,
            mustBeBlockedViolations = mustBeBlockedViolations
        )
    }

    // ==========================================================================
    // Helper Methods
    // ==========================================================================

    /**
     * Check if any creature can legally attack.
     */
    fun hasLegalAttackers(
        state: GameState,
        playerId: EntityId,
        modifierProvider: ModifierProvider? = null
    ): Boolean {
        return state.getCreaturesControlledBy(playerId).any { creatureId ->
            canDeclareAttacker(state, creatureId, playerId, modifierProvider).let { result ->
                result is ValidationResult.Valid || result is ValidationResult.RequiresCost
            }
        }
    }

    /**
     * Check if any creature can legally block.
     */
    fun hasLegalBlockers(
        state: GameState,
        playerId: EntityId,
        modifierProvider: ModifierProvider? = null
    ): Boolean {
        if (state.combat == null) return false

        // Query all attackers via ECS component
        val attackerIds = state.entitiesWithComponent<AttackingComponent>()
        if (attackerIds.isEmpty()) return false

        return state.getCreaturesControlledBy(playerId).any { blockerId ->
            attackerIds.any { attackerId ->
                canDeclareBlocker(state, blockerId, attackerId, playerId, modifierProvider).let { result ->
                    result is BlockValidationResult.Valid || result is BlockValidationResult.RequiresCost
                }
            }
        }
    }
}

/**
 * Results of validating the complete combat declaration phase.
 */
data class CombatDeclarationValidation(
    /** Attackers with menace that have only 1 blocker */
    val menaceViolations: List<EntityId>,
    /** Creatures with MustAttackComponent that aren't attacking */
    val mustAttackViolations: List<EntityId>,
    /** Creatures with MustBlockComponent that aren't blocking */
    val mustBlockViolations: List<EntityId>,
    /** Creatures with MustBeBlockedComponent that aren't blocked */
    val mustBeBlockedViolations: List<EntityId>
) {
    val isValid: Boolean
        get() = menaceViolations.isEmpty() &&
                mustAttackViolations.isEmpty() &&
                mustBlockViolations.isEmpty() &&
                mustBeBlockedViolations.isEmpty()

    val violations: List<String>
        get() = buildList {
            if (menaceViolations.isNotEmpty()) {
                add("${menaceViolations.size} creature(s) with menace blocked by only 1 creature")
            }
            if (mustAttackViolations.isNotEmpty()) {
                add("${mustAttackViolations.size} creature(s) must attack but aren't")
            }
            if (mustBlockViolations.isNotEmpty()) {
                add("${mustBlockViolations.size} creature(s) must block but aren't")
            }
            if (mustBeBlockedViolations.isNotEmpty()) {
                add("${mustBeBlockedViolations.size} creature(s) must be blocked but aren't")
            }
        }

    companion object {
        fun empty() = CombatDeclarationValidation(
            emptyList(), emptyList(), emptyList(), emptyList()
        )
    }
}
