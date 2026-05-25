package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.combat.AttackerOrderComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter

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
class DamageCalculator(
    private val cardRegistry: CardRegistry? = null,
) {

    private val predicateEvaluator = PredicateEvaluator()

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
        val damageMarked = creatureContainer?.get<DamageComponent>()?.amount ?: 0

        // Use projected values for toughness (includes floating effects like +4/+4)
        val projected = state.projectedState
        val toughness = projected.getToughness(creatureId) ?: 0

        // Check if source has deathtouch (using projected keywords)
        val hasDeathtouch = projected.hasKeyword(sourceId, Keyword.DEATHTOUCH)

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

        // Use projected values for power and keywords (includes floating effects like +4/+4)
        val projected = state.projectedState
        val attackerPower = CombatDamageUtils.getAssignedCombatDamage(state, projected, attackerId, cardRegistry)
        if (attackerPower <= 0) {
            return DamageDistribution(emptyMap(), 0, 0)
        }

        val hasTrample = projected.hasKeyword(attackerId, Keyword.TRAMPLE)

        // Get blockers in damage assignment order, filtering out dead blockers
        val blockedComponent = attackerContainer.get<BlockedComponent>()
        val orderedBlockers = (attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
            ?: blockedComponent?.blockerIds
            ?: emptyList()).filter { it in state.getBattlefield() }

        if (orderedBlockers.isEmpty()) {
            // Unblocked - this shouldn't be called for unblocked attackers
            return DamageDistribution(emptyMap(), 0, attackerPower)
        }

        val assignments = mutableMapOf<EntityId, Int>()
        var remainingPower = attackerPower

        // Assign lethal damage to each blocker in order, accounting for damage prevention.
        // The default assignment includes extra damage to overcome prevention effects
        // (e.g., Daunting Defender preventing 1 damage to Clerics) so that the default
        // actually kills the blocker, matching what a player would do in a physical game.
        // Per CR 510.1d, without trample ALL damage must be assigned to blockers.
        // The last blocker receives all remaining damage (not just lethal).
        for ((index, blockerId) in orderedBlockers.withIndex()) {
            if (remainingPower <= 0) break

            val isLastBlocker = index == orderedBlockers.size - 1
            val lethalInfo = calculateLethalDamage(state, blockerId, attackerId)
            val preventionAmount = estimateDamagePrevention(state, projected, blockerId)
            val effectiveLethal = lethalInfo.lethalAmount + preventionAmount
            val damageToAssign = if (isLastBlocker && !hasTrample) {
                // Last blocker without trample gets all remaining damage
                remainingPower
            } else {
                minOf(remainingPower, effectiveLethal)
            }

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
        attackerContainer.get<CardComponent>() ?: return false

        val blockedComponent = attackerContainer.get<BlockedComponent>()
        val blockerIds = blockedComponent?.blockerIds?.filter { it in state.getBattlefield() } ?: return false
        if (blockerIds.isEmpty()) return false

        // Use projected values for keywords and power (includes floating effects like +4/+4)
        val projected = state.projectedState

        // Single blocker without trample = no choice (all damage goes to that one blocker).
        if (blockerIds.size <= 1 && !projected.hasKeyword(attackerId, Keyword.TRAMPLE)) {
            return false
        }

        // Trample (choose how much spills over) or 2+ blockers always present a choice: the
        // attacking player picks the damage-assignment order, which decides *which* blockers die
        // when power is short and where any overkill goes (CR 510.1c) — not just whether there's
        // excess to spread. So surface the board whenever there is more than one way to assign.
        return true
    }

    /**
     * Auto-calculate damage distribution for a blocker blocking multiple attackers.
     *
     * Similar to [calculateAutoDamageDistribution] but for the reverse case: a single
     * blocker dividing its damage among multiple attackers it's blocking.
     *
     * Uses [AttackerOrderComponent] for the damage assignment order. Assigns lethal
     * damage to each attacker in order, with the last attacker receiving all remaining damage.
     *
     * Per CR 510.1c, "in checking for assigned lethal damage, take into account damage
     * already marked on the creature and damage from other creatures that's being assigned
     * during the same combat damage step." The [pendingDamage] parameter tracks damage
     * being assigned by other blockers in this same step.
     *
     * @param state Current game state
     * @param blockerId The blocking creature
     * @param pendingDamage Damage already being assigned to each creature by other sources
     *        in this same combat damage step (per CR 510.1c)
     * @return DamageDistribution with assignments to attackers
     */
    fun calculateBlockerDamageDistribution(
        state: GameState,
        blockerId: EntityId,
        pendingDamage: Map<EntityId, Int> = emptyMap()
    ): DamageDistribution {
        val blockerContainer = state.getEntity(blockerId)
            ?: return DamageDistribution(emptyMap(), 0, 0)

        blockerContainer.get<CardComponent>()
            ?: return DamageDistribution(emptyMap(), 0, 0)

        val projected = state.projectedState
        val blockerPower = CombatDamageUtils.getAssignedCombatDamage(state, projected, blockerId, cardRegistry)
        if (blockerPower <= 0) {
            return DamageDistribution(emptyMap(), 0, 0)
        }

        val blockingComponent = blockerContainer.get<BlockingComponent>()
            ?: return DamageDistribution(emptyMap(), 0, 0)

        // Get attackers in damage assignment order, filtering out dead attackers
        val orderedAttackers = (blockerContainer.get<AttackerOrderComponent>()?.orderedAttackers
            ?: blockingComponent.blockedAttackerIds).filter { it in state.getBattlefield() }

        if (orderedAttackers.isEmpty()) {
            return DamageDistribution(emptyMap(), 0, blockerPower)
        }

        val assignments = mutableMapOf<EntityId, Int>()
        var remainingPower = blockerPower

        for ((index, attackerId) in orderedAttackers.withIndex()) {
            if (remainingPower <= 0) break

            val isLastAttacker = index == orderedAttackers.size - 1
            val lethalInfo = calculateLethalDamage(state, attackerId, blockerId)
            val preventionAmount = estimateDamagePrevention(state, projected, attackerId)
            // Account for damage already being assigned by other sources this step (CR 510.1c)
            val alreadyPending = pendingDamage[attackerId] ?: 0
            val effectiveLethal = (lethalInfo.lethalAmount + preventionAmount - alreadyPending).coerceAtLeast(0)
            val damageToAssign = if (isLastAttacker) {
                // Last attacker gets all remaining damage
                remainingPower
            } else {
                minOf(remainingPower, effectiveLethal)
            }

            assignments[attackerId] = damageToAssign
            remainingPower -= damageToAssign
        }

        return DamageDistribution(
            assignments = assignments,
            totalAssigned = blockerPower - remainingPower,
            unassignedDamage = remainingPower
        )
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

        val orderedBlockers = (attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
            ?: attackerContainer.get<BlockedComponent>()?.blockerIds
            ?: return emptyMap()).filter { it in state.getBattlefield() }

        val minimums = mutableMapOf<EntityId, Int>()

        for (blockerId in orderedBlockers) {
            val lethalInfo = calculateLethalDamage(state, blockerId, attackerId)
            minimums[blockerId] = lethalInfo.lethalAmount
        }

        return minimums
    }

    /**
     * Estimate how much damage prevention would apply to a creature.
     *
     * Scans the battlefield for ReplacementEffectSourceComponent containing
     * PreventDamage effects that match the target creature.
     * This is used to adjust the default damage assignment so that "lethal"
     * accounts for prevention (e.g., Daunting Defender prevents 1 to Clerics).
     */
    private fun estimateDamagePrevention(
        state: GameState,
        projected: ProjectedState,
        targetId: EntityId
    ): Int {
        var totalPrevention = 0

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = container.get<ControllerComponent>()?.playerId ?: continue

            for (effect in replacementComponent.replacementEffects) {
                if (effect !is PreventDamage) continue

                val damageEvent = effect.appliesTo
                if (damageEvent !is com.wingedsheep.sdk.scripting.GameEvent.DamageEvent) continue

                // This is called during combat, so combat damage type always matches
                val damageTypeMatches = when (damageEvent.damageType) {
                    is DamageType.Any -> true
                    is DamageType.Combat -> true  // We're estimating for combat
                    is DamageType.NonCombat -> false
                }
                if (!damageTypeMatches) continue

                val recipientMatches = when (val recipient = damageEvent.recipient) {
                    is RecipientFilter.Self -> targetId == entityId
                    is RecipientFilter.EnchantedCreature, is RecipientFilter.EquippedCreature -> {
                        val attachedTo = container.get<AttachedToComponent>()?.targetId
                        targetId == attachedTo
                    }
                    is RecipientFilter.Matching -> {
                        val context = PredicateContext(controllerId = sourceControllerId)
                        predicateEvaluator.matches(state, projected, targetId, recipient.filter, context)
                    }
                    is RecipientFilter.CreatureYouControl -> {
                        val isCreature = projected.isCreature(targetId)
                        val isControlled = projected.getController(targetId) == sourceControllerId
                        isCreature && isControlled
                    }
                    is RecipientFilter.Any -> true
                    else -> false
                }

                if (recipientMatches) {
                    totalPrevention += effect.amount ?: 0
                }
            }
        }

        return totalPrevention
    }

    private fun getCreatureName(state: GameState, creatureId: EntityId): String {
        return state.getEntity(creatureId)?.get<CardComponent>()?.name ?: "creature"
    }
}
