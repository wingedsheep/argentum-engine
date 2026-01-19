package com.wingedsheep.rulesengine.ecs.combat

import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.layers.GameObjectView
import com.wingedsheep.rulesengine.ecs.layers.ModifierProvider
import com.wingedsheep.rulesengine.ecs.layers.StateProjector

/**
 * Calculates combat damage for the ECS combat system.
 *
 * Implements MTG's combat damage rules including:
 * - Two-step damage (first strike step, then regular damage step)
 * - Damage assignment to blockers in order
 * - Lethal damage requirement before moving to next blocker
 * - Trample damage calculation
 * - Deathtouch interaction (1 damage is lethal)
 *
 * The calculator produces PendingDamageEvents that can be modified by
 * prevention effects before being applied.
 */
object CombatDamageCalculator {

    // ==========================================================================
    // Combat Damage Steps
    // ==========================================================================

    /**
     * Identifies which combat damage step we're in.
     */
    enum class DamageStep {
        /** First strike and double strike creatures deal damage */
        FIRST_STRIKE,
        /** All other creatures (and double strike creatures again) deal damage */
        REGULAR
    }

    // ==========================================================================
    // Pending Damage Events
    // ==========================================================================

    /**
     * Represents damage that will be dealt, before prevention effects.
     *
     * These events can be modified by damage prevention effects (Fog, etc.)
     * before being resolved into actual damage.
     */
    sealed interface PendingDamageEvent {
        val sourceId: EntityId
        val amount: Int
        val isCombatDamage: Boolean get() = true

        /**
         * Damage to a player (unblocked attacker or trample).
         */
        data class ToPlayer(
            override val sourceId: EntityId,
            val targetPlayerId: EntityId,
            override val amount: Int,
            val isTrampleDamage: Boolean = false
        ) : PendingDamageEvent

        /**
         * Damage to a planeswalker.
         */
        data class ToPlaneswalker(
            override val sourceId: EntityId,
            val targetPlaneswalker: EntityId,
            override val amount: Int
        ) : PendingDamageEvent

        /**
         * Damage to a creature.
         */
        data class ToCreature(
            override val sourceId: EntityId,
            val targetCreatureId: EntityId,
            override val amount: Int
        ) : PendingDamageEvent
    }

    /**
     * Result of calculating combat damage for a step.
     */
    data class DamageCalculationResult(
        val step: DamageStep,
        val damageEvents: List<PendingDamageEvent>,
        val creaturesDealtDamage: Set<EntityId>
    ) {
        val hasFirstStrikeDamage: Boolean
            get() = step == DamageStep.FIRST_STRIKE && damageEvents.isNotEmpty()

        val hasDamage: Boolean
            get() = damageEvents.isNotEmpty()
    }

    // ==========================================================================
    // Damage Calculation
    // ==========================================================================

    /**
     * Calculate damage for the first strike damage step.
     *
     * Only creatures with first strike or double strike deal damage in this step.
     */
    fun calculateFirstStrikeDamage(
        state: GameState,
        modifierProvider: ModifierProvider? = null
    ): DamageCalculationResult {
        if (state.combat == null) {
            return DamageCalculationResult(DamageStep.FIRST_STRIKE, emptyList(), emptySet())
        }

        val projector = StateProjector.forState(state, modifierProvider)
        val damageEvents = mutableListOf<PendingDamageEvent>()
        val creaturesDealtDamage = mutableSetOf<EntityId>()

        // Calculate damage from attackers with first strike or double strike (ECS query)
        for (attackerId in state.entitiesWithComponent<AttackingComponent>()) {
            val attacking = state.getComponent<AttackingComponent>(attackerId) ?: continue

            // Skip if doesn't deal first strike damage
            if (!attacking.dealsFirstStrikeDamage) continue

            val attacker = projector.getView(attackerId) ?: continue
            val attackerDamage = calculateAttackerDamage(state, attackerId, attacker, projector)
            damageEvents.addAll(attackerDamage)
            creaturesDealtDamage.add(attackerId)
        }

        // Calculate damage from blockers with first strike or double strike
        for (entityId in state.getBattlefield()) {
            val blocking = state.getComponent<BlockingComponent>(entityId) ?: continue
            val blocker = projector.getView(entityId) ?: continue

            if (!blocker.hasKeyword(Keyword.FIRST_STRIKE) && !blocker.hasKeyword(Keyword.DOUBLE_STRIKE)) {
                continue
            }

            val blockerDamage = calculateBlockerDamage(blocker, blocking.attackerId)
            if (blockerDamage != null) {
                damageEvents.add(blockerDamage)
                creaturesDealtDamage.add(entityId)
            }
        }

        return DamageCalculationResult(DamageStep.FIRST_STRIKE, damageEvents, creaturesDealtDamage)
    }

    /**
     * Calculate damage for the regular damage step.
     *
     * Creatures deal damage here if they:
     * - Have double strike (deal damage again)
     * - Don't have first strike
     * - Have first strike but haven't dealt first strike damage yet
     */
    fun calculateRegularDamage(
        state: GameState,
        modifierProvider: ModifierProvider? = null
    ): DamageCalculationResult {
        if (state.combat == null) {
            return DamageCalculationResult(DamageStep.REGULAR, emptyList(), emptySet())
        }

        val projector = StateProjector.forState(state, modifierProvider)
        val damageEvents = mutableListOf<PendingDamageEvent>()
        val creaturesDealtDamage = mutableSetOf<EntityId>()

        // Calculate damage from attackers (ECS query)
        for (attackerId in state.entitiesWithComponent<AttackingComponent>()) {
            val attacking = state.getComponent<AttackingComponent>(attackerId) ?: continue

            // Skip if doesn't deal regular damage
            if (!attacking.dealsRegularDamage) continue

            val attacker = projector.getView(attackerId) ?: continue
            val attackerDamage = calculateAttackerDamage(state, attackerId, attacker, projector)
            damageEvents.addAll(attackerDamage)
            creaturesDealtDamage.add(attackerId)
        }

        // Calculate damage from blockers
        for (entityId in state.getBattlefield()) {
            val blocking = state.getComponent<BlockingComponent>(entityId) ?: continue
            val blocker = projector.getView(entityId) ?: continue

            // Skip if this blocker has first strike only (dealt damage in FS step)
            // Double strike creatures deal damage again
            if (blocker.hasKeyword(Keyword.FIRST_STRIKE) && !blocker.hasKeyword(Keyword.DOUBLE_STRIKE)) {
                continue
            }

            val blockerDamage = calculateBlockerDamage(blocker, blocking.attackerId)
            if (blockerDamage != null) {
                damageEvents.add(blockerDamage)
                creaturesDealtDamage.add(entityId)
            }
        }

        return DamageCalculationResult(DamageStep.REGULAR, damageEvents, creaturesDealtDamage)
    }

    /**
     * Check if there should be a first strike damage step.
     *
     * Returns true if any attacker or blocker has first strike or double strike.
     */
    fun hasFirstStrikeStep(
        state: GameState,
        modifierProvider: ModifierProvider? = null
    ): Boolean {
        if (state.combat == null) return false
        val projector = StateProjector.forState(state, modifierProvider)

        // Check attackers (ECS query)
        for (attackerId in state.entitiesWithComponent<AttackingComponent>()) {
            val attacker = projector.getView(attackerId) ?: continue
            if (attacker.hasKeyword(Keyword.FIRST_STRIKE) || attacker.hasKeyword(Keyword.DOUBLE_STRIKE)) {
                return true
            }
        }

        // Check blockers
        for (entityId in state.getBattlefield()) {
            if (!state.hasComponent<BlockingComponent>(entityId)) continue
            val blocker = projector.getView(entityId) ?: continue
            if (blocker.hasKeyword(Keyword.FIRST_STRIKE) || blocker.hasKeyword(Keyword.DOUBLE_STRIKE)) {
                return true
            }
        }

        return false
    }

    // ==========================================================================
    // Attacker Damage Calculation
    // ==========================================================================

    /**
     * Calculate all damage events from a single attacker.
     */
    private fun calculateAttackerDamage(
        state: GameState,
        attackerId: EntityId,
        attacker: GameObjectView,
        projector: StateProjector
    ): List<PendingDamageEvent> {
        val attackerPower = attacker.power ?: 0
        if (attackerPower <= 0) return emptyList()

        val attacking = state.getComponent<AttackingComponent>(attackerId) ?: return emptyList()
        val blockedBy = state.getComponent<BlockedByComponent>(attackerId)

        // If not blocked, damage goes to the attack target
        if (blockedBy == null || !blockedBy.isBlocked) {
            return listOf(damageToTarget(attackerId, attacking.target, attackerPower))
        }

        // Blocked - assign damage to blockers
        return calculateDamageToBlockers(
            state = state,
            attackerId = attackerId,
            attacker = attacker,
            blockedBy = blockedBy,
            projector = projector
        )
    }

    /**
     * Create a damage event to the combat target.
     */
    private fun damageToTarget(
        sourceId: EntityId,
        target: CombatTarget,
        amount: Int
    ): PendingDamageEvent {
        return when (target) {
            is CombatTarget.Player -> PendingDamageEvent.ToPlayer(
                sourceId = sourceId,
                targetPlayerId = target.playerId,
                amount = amount
            )
            is CombatTarget.Planeswalker -> PendingDamageEvent.ToPlaneswalker(
                sourceId = sourceId,
                targetPlaneswalker = target.planeswalkerEntityId,
                amount = amount
            )
            is CombatTarget.Battle -> {
                // Battles receive damage like planeswalkers
                // For now treat as planeswalker damage
                PendingDamageEvent.ToPlaneswalker(
                    sourceId = sourceId,
                    targetPlaneswalker = target.battleEntityId,
                    amount = amount
                )
            }
        }
    }

    /**
     * Calculate damage to blockers in damage assignment order.
     *
     * Implements MTG's damage assignment rules:
     * - Damage is assigned in order
     * - Lethal damage must be assigned to a blocker before moving to the next
     * - Deathtouch makes 1 damage lethal
     * - Trample damage goes to the player after lethal to all blockers
     */
    private fun calculateDamageToBlockers(
        state: GameState,
        attackerId: EntityId,
        attacker: GameObjectView,
        blockedBy: BlockedByComponent,
        projector: StateProjector
    ): List<PendingDamageEvent> {
        val attackerPower = attacker.power ?: 0
        if (attackerPower <= 0) return emptyList()

        val hasDeathtouch = attacker.hasKeyword(Keyword.DEATHTOUCH)
        val hasTrample = attacker.hasKeyword(Keyword.TRAMPLE)

        val events = mutableListOf<PendingDamageEvent>()
        var remainingDamage = attackerPower

        // Assign damage in blocker order
        for ((index, blockerId) in blockedBy.blockerIds.withIndex()) {
            if (remainingDamage <= 0) break

            val blocker = projector.getView(blockerId) ?: continue
            val blockerToughness = blocker.toughness ?: 0
            val existingDamage = blocker.damage
            val remainingToughness = (blockerToughness - existingDamage).coerceAtLeast(0)

            // Calculate lethal damage amount
            val lethalDamage = if (hasDeathtouch) {
                // With deathtouch, 1 damage is lethal
                if (remainingToughness > 0) 1 else 0
            } else {
                remainingToughness
            }

            val isLastBlocker = index == blockedBy.blockerIds.size - 1

            // Calculate damage to this blocker
            val damageToAssign = if (isLastBlocker && !hasTrample) {
                // Last blocker without trample: all remaining damage
                remainingDamage
            } else {
                // Otherwise: at least lethal, but could be more
                minOf(remainingDamage, lethalDamage.coerceAtLeast(1))
            }

            if (damageToAssign > 0) {
                events.add(
                    PendingDamageEvent.ToCreature(
                        sourceId = attackerId,
                        targetCreatureId = blockerId,
                        amount = damageToAssign
                    )
                )
                remainingDamage -= damageToAssign
            }
        }

        // Trample damage goes to the original target
        if (hasTrample && remainingDamage > 0) {
            val attacking = state.getComponent<AttackingComponent>(attackerId)
            if (attacking != null) {
                when (val target = attacking.target) {
                    is CombatTarget.Player -> events.add(
                        PendingDamageEvent.ToPlayer(
                            sourceId = attackerId,
                            targetPlayerId = target.playerId,
                            amount = remainingDamage,
                            isTrampleDamage = true
                        )
                    )
                    is CombatTarget.Planeswalker -> events.add(
                        PendingDamageEvent.ToPlaneswalker(
                            sourceId = attackerId,
                            targetPlaneswalker = target.planeswalkerEntityId,
                            amount = remainingDamage
                        )
                    )
                    is CombatTarget.Battle -> events.add(
                        PendingDamageEvent.ToPlaneswalker(
                            sourceId = attackerId,
                            targetPlaneswalker = target.battleEntityId,
                            amount = remainingDamage
                        )
                    )
                }
            }
        }

        return events
    }

    // ==========================================================================
    // Blocker Damage Calculation
    // ==========================================================================

    /**
     * Calculate damage from a single blocker to the attacker.
     */
    private fun calculateBlockerDamage(
        blocker: GameObjectView,
        attackerId: EntityId
    ): PendingDamageEvent? {
        val blockerPower = blocker.power ?: 0
        if (blockerPower <= 0) return null

        return PendingDamageEvent.ToCreature(
            sourceId = blocker.entityId,
            targetCreatureId = attackerId,
            amount = blockerPower
        )
    }

    // ==========================================================================
    // State Updates
    // ==========================================================================

    /**
     * Mark that an attacker has dealt first strike damage.
     *
     * This is used to track which attackers should deal damage in the
     * regular damage step (only double strike creatures deal twice).
     */
    fun markFirstStrikeDamageDealt(
        state: GameState,
        creaturesDealtDamage: Set<EntityId>
    ): GameState {
        var result = state

        for (creatureId in creaturesDealtDamage) {
            val attacking = state.getComponent<AttackingComponent>(creatureId)
            if (attacking != null) {
                result = result.updateEntity(creatureId) { container ->
                    container.with(attacking.withFirstStrikeDamageDealt())
                }
            }
        }

        return result
    }

    // ==========================================================================
    // Utility Methods
    // ==========================================================================

    /**
     * Get all creatures that will deal damage in the current step.
     */
    fun getCreaturesDealingDamage(
        state: GameState,
        step: DamageStep,
        modifierProvider: ModifierProvider? = null
    ): Set<EntityId> {
        return when (step) {
            DamageStep.FIRST_STRIKE -> calculateFirstStrikeDamage(state, modifierProvider).creaturesDealtDamage
            DamageStep.REGULAR -> calculateRegularDamage(state, modifierProvider).creaturesDealtDamage
        }
    }

    /**
     * Check if a specific creature will deal damage in the given step.
     */
    fun willDealDamage(
        state: GameState,
        creatureId: EntityId,
        step: DamageStep,
        modifierProvider: ModifierProvider? = null
    ): Boolean {
        val projector = StateProjector.forState(state, modifierProvider)
        val creature = projector.getView(creatureId) ?: return false

        if (!creature.isCreature) return false
        if ((creature.power ?: 0) <= 0) return false

        val attacking = state.getComponent<AttackingComponent>(creatureId)
        val blocking = state.getComponent<BlockingComponent>(creatureId)

        if (attacking == null && blocking == null) return false

        return when (step) {
            DamageStep.FIRST_STRIKE -> {
                if (attacking != null) {
                    attacking.dealsFirstStrikeDamage
                } else {
                    creature.hasKeyword(Keyword.FIRST_STRIKE) || creature.hasKeyword(Keyword.DOUBLE_STRIKE)
                }
            }
            DamageStep.REGULAR -> {
                if (attacking != null) {
                    attacking.dealsRegularDamage
                } else {
                    // Blockers with only first strike don't deal regular damage
                    !creature.hasKeyword(Keyword.FIRST_STRIKE) || creature.hasKeyword(Keyword.DOUBLE_STRIKE)
                }
            }
        }
    }
}
