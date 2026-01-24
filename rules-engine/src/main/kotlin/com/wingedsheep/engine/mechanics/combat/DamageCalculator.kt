package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId

/**
 * Calculates combat damage assignments according to MTG rules (CR 510).
 *
 * Key rules:
 * - CR 510.1c: Damage must be assigned in order; can't assign to creature N+1
 *   until creature N has been assigned lethal damage.
 * - CR 510.1b: Attacker's controller chooses how to divide damage.
 * - CR 702.2b: Deathtouch - any amount of damage is considered lethal.
 * - CR 702.19: Trample - excess damage can be assigned to defending player.
 */
class DamageCalculator {

    /**
     * Result of calculating lethal damage for a creature.
     */
    data class LethalDamageInfo(
        val creatureId: EntityId,
        val toughness: Int,
        val damageAlreadyMarked: Int,
        val lethalAmount: Int,
        val sourceHasDeathtouch: Boolean
    )

    /**
     * Result of auto-calculating damage distribution.
     */
    data class DamageDistribution(
        /** Map of target (creature or player) to damage amount */
        val assignments: Map<EntityId, Int>,
        /** Total damage assigned */
        val totalAssigned: Int,
        /** Damage that couldn't be assigned (shouldn't happen normally) */
        val unassignedDamage: Int
    )

    /**
     * Calculate the minimum damage needed to be considered "lethal" for a creature.
     *
     * Per CR 510.1c: Damage is lethal if it equals or exceeds toughness minus
     * damage already marked, OR if the source has deathtouch (any nonzero amount).
     *
     * @param state Current game state
     * @param creatureId The creature receiving damage
     * @param sourceId The source dealing damage (to check for deathtouch)
     * @return LethalDamageInfo with calculated values
     */
    fun calculateLethalDamage(
        state: GameState,
        creatureId: EntityId,
        sourceId: EntityId
    ): LethalDamageInfo {
        val creatureContainer = state.getEntity(creatureId)
        val creatureCard = creatureContainer?.get<CardComponent>()

        val toughness = creatureCard?.baseStats?.baseToughness ?: 0
        val damageMarked = creatureContainer?.get<DamageComponent>()?.amount ?: 0

        // Check if source has deathtouch
        val sourceContainer = state.getEntity(sourceId)
        val sourceCard = sourceContainer?.get<CardComponent>()
        val hasDeathtouch = sourceCard?.baseKeywords?.contains(Keyword.DEATHTOUCH) == true

        // With deathtouch, 1 damage is lethal. Otherwise, need to reach toughness.
        val lethalAmount = if (hasDeathtouch) {
            1
        } else {
            (toughness - damageMarked).coerceAtLeast(1)
        }

        return LethalDamageInfo(
            creatureId = creatureId,
            toughness = toughness,
            damageAlreadyMarked = damageMarked,
            lethalAmount = lethalAmount,
            sourceHasDeathtouch = hasDeathtouch
        )
    }

    /**
     * Auto-calculate optimal damage distribution for an attacker.
     *
     * This implements the default "assign lethal to each blocker in order" behavior.
     * Players can override this with manual assignment.
     *
     * @param state Current game state
     * @param attackerId The attacking creature
     * @return DamageDistribution with assignments to blockers (and player if trample)
     */
    fun calculateAutoDamageDistribution(
        state: GameState,
        attackerId: EntityId
    ): DamageDistribution {
        val attackerContainer = state.getEntity(attackerId)
            ?: return DamageDistribution(emptyMap(), 0, 0)

        val attackerCard = attackerContainer.get<CardComponent>()
            ?: return DamageDistribution(emptyMap(), 0, 0)

        val attackerPower = attackerCard.baseStats?.basePower ?: 0
        if (attackerPower <= 0) {
            return DamageDistribution(emptyMap(), 0, 0)
        }

        val attackerKeywords = attackerCard.baseKeywords
        val hasTrample = attackerKeywords.contains(Keyword.TRAMPLE)

        // Get blockers in damage assignment order
        val blockedComponent = attackerContainer.get<BlockedComponent>()
        val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
            ?: blockedComponent?.blockerIds
            ?: emptyList()

        if (orderedBlockers.isEmpty()) {
            // Unblocked - this shouldn't be called for unblocked attackers
            return DamageDistribution(emptyMap(), 0, attackerPower)
        }

        val assignments = mutableMapOf<EntityId, Int>()
        var remainingPower = attackerPower

        // Assign lethal damage to each blocker in order
        for (blockerId in orderedBlockers) {
            if (remainingPower <= 0) break

            val lethalInfo = calculateLethalDamage(state, blockerId, attackerId)
            val damageToAssign = minOf(remainingPower, lethalInfo.lethalAmount)

            assignments[blockerId] = damageToAssign
            remainingPower -= damageToAssign
        }

        // Handle trample - excess damage goes to defending player
        if (hasTrample && remainingPower > 0) {
            // Get the defending player from AttackingComponent
            val attackingComponent = attackerContainer.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
            if (attackingComponent != null) {
                assignments[attackingComponent.defenderId] = remainingPower
                remainingPower = 0
            }
        }

        return DamageDistribution(
            assignments = assignments,
            totalAssigned = attackerPower - remainingPower,
            unassignedDamage = remainingPower
        )
    }

    /**
     * Validate a manual damage assignment.
     *
     * Rules for valid assignment:
     * 1. Total damage cannot exceed attacker's power
     * 2. Damage must be assigned in blocker order (can't skip)
     * 3. Each blocker (except the last) must be assigned lethal damage
     *    before assigning any damage to the next blocker
     * 4. Trample allows assigning to defending player only after all
     *    blockers have lethal damage assigned
     *
     * @param state Current game state
     * @param attackerId The attacking creature
     * @param assignment Proposed damage assignments (target -> amount)
     * @return Error message if invalid, null if valid
     */
    fun validateDamageAssignment(
        state: GameState,
        attackerId: EntityId,
        assignment: Map<EntityId, Int>
    ): String? {
        val attackerContainer = state.getEntity(attackerId)
            ?: return "Attacker not found"

        val attackerCard = attackerContainer.get<CardComponent>()
            ?: return "Attacker is not a card"

        val attackerPower = attackerCard.baseStats?.basePower ?: 0
        val attackerKeywords = attackerCard.baseKeywords
        val hasTrample = attackerKeywords.contains(Keyword.TRAMPLE)

        // Get blockers in damage assignment order
        val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
            ?: attackerContainer.get<BlockedComponent>()?.blockerIds
            ?: emptyList()

        // Get defending player for trample check
        val attackingComponent = attackerContainer.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
        val defenderId = attackingComponent?.defenderId

        // Check total damage doesn't exceed power
        val totalDamage = assignment.values.sum()
        if (totalDamage > attackerPower) {
            return "Total damage ($totalDamage) exceeds attacker's power ($attackerPower)"
        }

        // Verify damage is assigned in order with lethal requirements
        var allBlockersHaveLethal = true
        for ((index, blockerId) in orderedBlockers.withIndex()) {
            val damageToThisBlocker = assignment[blockerId] ?: 0
            val lethalInfo = calculateLethalDamage(state, blockerId, attackerId)

            // Check if this blocker has lethal
            val hasLethal = damageToThisBlocker >= lethalInfo.lethalAmount

            if (!hasLethal) {
                allBlockersHaveLethal = false

                // If this blocker doesn't have lethal, no damage can be assigned
                // to any subsequent blocker
                for (subsequentIndex in (index + 1) until orderedBlockers.size) {
                    val subsequentBlocker = orderedBlockers[subsequentIndex]
                    val subsequentDamage = assignment[subsequentBlocker] ?: 0
                    if (subsequentDamage > 0) {
                        return "Cannot assign damage to ${getCreatureName(state, subsequentBlocker)} " +
                               "until ${getCreatureName(state, blockerId)} has been assigned lethal damage"
                    }
                }
            }
        }

        // Check trample assignment to player
        val damageToPlayer = assignment[defenderId] ?: 0
        if (damageToPlayer > 0) {
            if (!hasTrample) {
                return "Cannot assign damage to defending player without trample"
            }
            if (!allBlockersHaveLethal) {
                return "Cannot assign trample damage until all blockers have lethal damage"
            }
        }

        return null
    }

    /**
     * Check if an attacker requires manual damage assignment.
     *
     * Manual assignment is needed when:
     * - Attacker has trample and is blocked (player can choose split)
     * - Attacker has enough power to kill multiple blockers with options
     * - User preference is set to always manually assign
     */
    fun requiresManualAssignment(
        state: GameState,
        attackerId: EntityId,
        userPreference: Boolean = false
    ): Boolean {
        if (userPreference) return true

        val attackerContainer = state.getEntity(attackerId) ?: return false
        val attackerCard = attackerContainer.get<CardComponent>() ?: return false

        val blockedComponent = attackerContainer.get<BlockedComponent>()
        val blockerIds = blockedComponent?.blockerIds ?: return false

        // Single blocker without trample = no choice needed
        if (blockerIds.size <= 1 && !attackerCard.baseKeywords.contains(Keyword.TRAMPLE)) {
            return false
        }

        // Has trample = always has a choice for excess damage
        if (attackerCard.baseKeywords.contains(Keyword.TRAMPLE)) {
            return true
        }

        // Multiple blockers - check if there's excess damage to distribute
        val attackerPower = attackerCard.baseStats?.basePower ?: 0
        var totalLethalNeeded = 0

        for (blockerId in blockerIds) {
            val lethalInfo = calculateLethalDamage(state, blockerId, attackerId)
            totalLethalNeeded += lethalInfo.lethalAmount
        }

        // If power exceeds total lethal needed, there are choices to make
        return attackerPower > totalLethalNeeded
    }

    /**
     * Get the minimum damage requirements for each target.
     * Used for UI to show what the minimum valid assignment is.
     */
    fun getMinimumAssignments(
        state: GameState,
        attackerId: EntityId
    ): Map<EntityId, Int> {
        val attackerContainer = state.getEntity(attackerId) ?: return emptyMap()

        val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
            ?: attackerContainer.get<BlockedComponent>()?.blockerIds
            ?: return emptyMap()

        val minimums = mutableMapOf<EntityId, Int>()

        for (blockerId in orderedBlockers) {
            val lethalInfo = calculateLethalDamage(state, blockerId, attackerId)
            minimums[blockerId] = lethalInfo.lethalAmount
        }

        return minimums
    }

    private fun getCreatureName(state: GameState, creatureId: EntityId): String {
        return state.getEntity(creatureId)?.get<CardComponent>()?.name ?: "creature"
    }
}
