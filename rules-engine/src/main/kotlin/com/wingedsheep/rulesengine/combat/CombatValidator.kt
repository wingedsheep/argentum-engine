package com.wingedsheep.rulesengine.combat

import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.player.PlayerId

/**
 * Validates combat actions.
 */
object CombatValidator {

    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val reason: String) : ValidationResult
    }

    /**
     * Checks if a creature can be declared as an attacker.
     */
    fun canDeclareAttacker(state: GameState, cardId: CardId, playerId: PlayerId): ValidationResult {
        // Must be in declare attackers step
        if (state.currentStep != Step.DECLARE_ATTACKERS) {
            return ValidationResult.Invalid("Can only declare attackers during declare attackers step")
        }

        // Must be active player
        @Suppress("DEPRECATION")
        if (!state.turnState.isActivePlayer(playerId)) {
            return ValidationResult.Invalid("Only the active player can declare attackers")
        }

        // Find the creature
        val creature = state.battlefield.getCard(cardId)
            ?: return ValidationResult.Invalid("Creature not found on battlefield")

        // Must be a creature
        if (!creature.isCreature) {
            return ValidationResult.Invalid("Only creatures can attack")
        }

        // Must control the creature
        if (creature.controllerId != playerId.value) {
            return ValidationResult.Invalid("You don't control this creature")
        }

        // Must be untapped
        if (creature.isTapped) {
            return ValidationResult.Invalid("Tapped creatures cannot attack")
        }

        // Must not have summoning sickness (unless has haste)
        if (creature.summoningSickness && !creature.hasKeyword(Keyword.HASTE)) {
            return ValidationResult.Invalid("Creature has summoning sickness")
        }

        // Must not have defender
        if (creature.hasKeyword(Keyword.DEFENDER)) {
            return ValidationResult.Invalid("Creatures with defender cannot attack")
        }

        // Must not already be attacking
        @Suppress("DEPRECATION")
        if (state.combat?.isAttacking(cardId) == true) {
            return ValidationResult.Invalid("Creature is already attacking")
        }

        return ValidationResult.Valid
    }

    /**
     * Checks if a creature can block another creature.
     */
    fun canDeclareBlocker(
        state: GameState,
        blockerId: CardId,
        attackerId: CardId,
        playerId: PlayerId
    ): ValidationResult {
        // Must be in declare blockers step
        if (state.currentStep != Step.DECLARE_BLOCKERS) {
            return ValidationResult.Invalid("Can only declare blockers during declare blockers step")
        }

        // Must be defending player
        @Suppress("DEPRECATION")
        if (state.combat?.isDefendingPlayer(playerId) != true) {
            return ValidationResult.Invalid("Only the defending player can declare blockers")
        }

        // Find the blocker
        val blocker = state.battlefield.getCard(blockerId)
            ?: return ValidationResult.Invalid("Blocking creature not found on battlefield")

        // Find the attacker
        val attacker = state.battlefield.getCard(attackerId)
            ?: return ValidationResult.Invalid("Attacking creature not found on battlefield")

        // Blocker must be a creature
        if (!blocker.isCreature) {
            return ValidationResult.Invalid("Only creatures can block")
        }

        // Must control the blocker
        if (blocker.controllerId != playerId.value) {
            return ValidationResult.Invalid("You don't control this creature")
        }

        // Blocker must be untapped
        if (blocker.isTapped) {
            return ValidationResult.Invalid("Tapped creatures cannot block")
        }

        // Attacker must be attacking
        @Suppress("DEPRECATION")
        if (state.combat?.isAttacking(attackerId) != true) {
            return ValidationResult.Invalid("Target creature is not attacking")
        }

        // Check flying restriction
        if (attacker.hasKeyword(Keyword.FLYING)) {
            if (!blocker.hasKeyword(Keyword.FLYING) && !blocker.hasKeyword(Keyword.REACH)) {
                return ValidationResult.Invalid("Cannot block a creature with flying unless blocker has flying or reach")
            }
        }

        // Must not already be blocking this attacker
        @Suppress("DEPRECATION")
        val currentBlockers = state.combat?.getBlockersFor(attackerId) ?: emptyList()
        val blockerEntityId = EntityId.of(blockerId.value)
        if (blockerEntityId in currentBlockers) {
            return ValidationResult.Invalid("Creature is already blocking this attacker")
        }

        return ValidationResult.Valid
    }

    /**
     * Calculates combat damage for an attacker.
     * Returns pair of (damage to defending player, damage to each blocker)
     */
    fun calculateCombatDamage(
        state: GameState,
        attackerId: CardId
    ): CombatDamageResult {
        val attacker = state.battlefield.getCard(attackerId)
            ?: return CombatDamageResult.Invalid("Attacker not found")

        val attackerPower = attacker.currentPower ?: 0
        if (attackerPower <= 0) {
            return CombatDamageResult.NoDamage
        }

        val combat = state.combat
            ?: return CombatDamageResult.Invalid("Not in combat")

        // Convert attackerId to EntityId for CombatState lookups
        val attackerEntityId = EntityId.of(attackerId.value)

        @Suppress("DEPRECATION")
        val blockerEntityIds = combat.getBlockersFor(attackerId)

        if (blockerEntityIds.isEmpty()) {
            // Unblocked - all damage to defending player
            return CombatDamageResult.UnblockedDamage(attackerPower)
        }

        // Blocked - assign damage to blockers (convert EntityId to CardId for Zone lookup)
        val blockers = blockerEntityIds.mapNotNull { entityId ->
            state.battlefield.getCard(CardId(entityId.value))
        }
        val hasTrample = attacker.hasKeyword(Keyword.TRAMPLE)
        val hasDeathtouch = attacker.hasKeyword(Keyword.DEATHTOUCH)

        // Get damage assignment order (or default to blocker order)
        val orderedBlockerEntityIds = combat.damageAssignmentOrder[attackerEntityId] ?: blockerEntityIds

        val damageAssignment = mutableMapOf<CardId, Int>()
        var remainingDamage = attackerPower

        for ((index, blockerEntityId) in orderedBlockerEntityIds.withIndex()) {
            val blockerCardId = CardId(blockerEntityId.value)
            val blocker = state.battlefield.getCard(blockerCardId) ?: continue
            val blockerToughness = blocker.currentToughness ?: 0
            val existingDamage = blocker.damageMarked
            val isLastBlocker = index == orderedBlockerEntityIds.size - 1

            // Calculate lethal damage (1 with deathtouch, otherwise remaining toughness)
            val lethalDamage = if (hasDeathtouch) 1 else (blockerToughness - existingDamage).coerceAtLeast(0)

            // For the last blocker without trample, assign all remaining damage
            // (there's nowhere else for it to go)
            val damageToAssign = if (isLastBlocker && !hasTrample) {
                remainingDamage
            } else {
                minOf(remainingDamage, lethalDamage)
            }

            damageAssignment[blockerCardId] = damageToAssign
            remainingDamage -= damageToAssign

            if (remainingDamage <= 0) break
        }

        // Trample damage goes through to defending player
        val trampleDamage = if (hasTrample && remainingDamage > 0) remainingDamage else 0

        return CombatDamageResult.BlockedDamage(damageAssignment, trampleDamage)
    }

    /**
     * Checks if a creature has lethal damage marked.
     */
    fun hasLethalDamage(creature: CardInstance): Boolean {
        val toughness = creature.currentToughness ?: return false
        return creature.damageMarked >= toughness
    }
}

/**
 * Result of combat damage calculation.
 */
sealed interface CombatDamageResult {
    data object NoDamage : CombatDamageResult
    data class UnblockedDamage(val damage: Int) : CombatDamageResult
    data class BlockedDamage(
        val damageToBlockers: Map<CardId, Int>,
        val trampleDamage: Int = 0
    ) : CombatDamageResult
    data class Invalid(val reason: String) : CombatDamageResult
}
