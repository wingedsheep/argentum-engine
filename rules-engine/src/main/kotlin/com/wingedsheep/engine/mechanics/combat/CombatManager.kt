package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId

/**
 * Manages combat flow: attackers, blockers, damage.
 *
 * Combat proceeds through these steps:
 * 1. Beginning of combat step
 * 2. Declare attackers step
 * 3. Declare blockers step
 * 4. Combat damage step (first strike, then regular)
 * 5. End of combat step
 */
class CombatManager(
    private val damageCalculator: DamageCalculator = DamageCalculator()
) {

    // =========================================================================
    // Declare Attackers
    // =========================================================================

    /**
     * Validate and declare attackers.
     *
     * @param attackers Map of attacker entity ID to defender (player or planeswalker)
     */
    fun declareAttackers(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): ExecutionResult {
        // Validate each attacker
        for ((attackerId, defenderId) in attackers) {
            val validation = validateAttacker(state, attackingPlayer, attackerId)
            if (validation != null) {
                return ExecutionResult.error(state, validation)
            }
        }

        // Apply attacker components and tap attacking creatures
        var newState = state
        for ((attackerId, defenderId) in attackers) {
            newState = newState.updateEntity(attackerId) { container ->
                var updated = container.with(AttackingComponent(defenderId))
                // Tap attacking creatures (unless they have vigilance)
                val hasVigilance = container.get<CardComponent>()?.baseKeywords?.contains(Keyword.VIGILANCE) == true
                if (!hasVigilance) {
                    updated = updated.with(TappedComponent)
                }
                updated
            }
        }

        // Mark that attackers have been declared this combat (even if empty)
        newState = newState.updateEntity(attackingPlayer) { container ->
            container.with(AttackersDeclaredThisCombatComponent)
        }

        return ExecutionResult.success(
            newState,
            listOf(AttackersDeclaredEvent(attackers.keys.toList()))
        )
    }

    /**
     * Validate that a creature can attack.
     */
    private fun validateAttacker(
        state: GameState,
        attackingPlayer: EntityId,
        attackerId: EntityId
    ): String? {
        val container = state.getEntity(attackerId)
            ?: return "Attacker not found: $attackerId"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: $attackerId"

        // Must be a creature
        if (!cardComponent.typeLine.isCreature) {
            return "Only creatures can attack: ${cardComponent.name}"
        }

        // Must be controlled by attacking player
        val controller = container.get<ControllerComponent>()?.playerId
        if (controller != attackingPlayer) {
            return "You don't control ${cardComponent.name}"
        }

        // Must be untapped
        if (container.has<TappedComponent>()) {
            return "${cardComponent.name} is tapped and cannot attack"
        }

        // Cannot have summoning sickness (unless it has haste)
        val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
        if (!hasHaste && container.has<SummoningSicknessComponent>()) {
            return "${cardComponent.name} has summoning sickness"
        }

        // Cannot have defender
        if (cardComponent.baseKeywords.contains(Keyword.DEFENDER)) {
            return "${cardComponent.name} has defender and cannot attack"
        }

        // Cannot be already attacking
        if (container.has<AttackingComponent>()) {
            return "${cardComponent.name} is already attacking"
        }

        return null
    }

    // =========================================================================
    // Declare Blockers
    // =========================================================================

    /**
     * Validate and declare blockers.
     *
     * @param blockers Map of blocker entity ID to list of attackers being blocked
     */
    fun declareBlockers(
        state: GameState,
        blockingPlayer: EntityId,
        blockers: Map<EntityId, List<EntityId>>
    ): ExecutionResult {
        // Validate each blocker
        for ((blockerId, attackerIds) in blockers) {
            val validation = validateBlocker(state, blockingPlayer, blockerId, attackerIds)
            if (validation != null) {
                return ExecutionResult.error(state, validation)
            }
        }

        // Check menace requirements
        val menaceValidation = validateMenaceRequirements(state, blockers)
        if (menaceValidation != null) {
            return ExecutionResult.error(state, menaceValidation)
        }

        // Apply blocker components
        var newState = state
        for ((blockerId, attackerIds) in blockers) {
            newState = newState.updateEntity(blockerId) { container ->
                container.with(BlockingComponent(attackerIds))
            }

            // Mark attackers as blocked
            for (attackerId in attackerIds) {
                newState = newState.updateEntity(attackerId) { container ->
                    val existing = container.get<BlockedComponent>()?.blockerIds ?: emptyList()
                    container.with(BlockedComponent(existing + blockerId))
                }
            }
        }

        // Mark that blockers have been declared this combat (even if empty)
        newState = newState.updateEntity(blockingPlayer) { container ->
            container.with(BlockersDeclaredThisCombatComponent)
        }

        return ExecutionResult.success(
            newState,
            listOf(BlockersDeclaredEvent(blockers))
        )
    }

    /**
     * Validate that a creature can block.
     */
    private fun validateBlocker(
        state: GameState,
        blockingPlayer: EntityId,
        blockerId: EntityId,
        attackerIds: List<EntityId>
    ): String? {
        val container = state.getEntity(blockerId)
            ?: return "Blocker not found: $blockerId"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: $blockerId"

        // Must be a creature
        if (!cardComponent.typeLine.isCreature) {
            return "Only creatures can block: ${cardComponent.name}"
        }

        // Must be controlled by blocking player
        val controller = container.get<ControllerComponent>()?.playerId
        if (controller != blockingPlayer) {
            return "You don't control ${cardComponent.name}"
        }

        // Must be untapped
        if (container.has<TappedComponent>()) {
            return "${cardComponent.name} is tapped and cannot block"
        }

        // Cannot be already blocking
        if (container.has<BlockingComponent>()) {
            return "${cardComponent.name} is already blocking"
        }

        // Check evasion abilities of each attacker
        for (attackerId in attackerIds) {
            val evasionValidation = validateCanBlock(state, blockerId, attackerId)
            if (evasionValidation != null) {
                return evasionValidation
            }
        }

        return null
    }

    /**
     * Validate that a blocker can block a specific attacker (evasion abilities).
     */
    private fun validateCanBlock(
        state: GameState,
        blockerId: EntityId,
        attackerId: EntityId
    ): String? {
        val blockerContainer = state.getEntity(blockerId)!!
        val attackerContainer = state.getEntity(attackerId)
            ?: return "Attacker not found: $attackerId"

        val blockerCard = blockerContainer.get<CardComponent>()!!
        val attackerCard = attackerContainer.get<CardComponent>()
            ?: return "Not a card: $attackerId"

        val blockerKeywords = blockerCard.baseKeywords
        val attackerKeywords = attackerCard.baseKeywords

        // Flying: Can only be blocked by creatures with flying or reach
        if (attackerKeywords.contains(Keyword.FLYING)) {
            val canBlockFlying = blockerKeywords.contains(Keyword.FLYING) ||
                blockerKeywords.contains(Keyword.REACH)
            if (!canBlockFlying) {
                return "${blockerCard.name} cannot block ${attackerCard.name} (flying)"
            }
        }

        // Horsemanship: Can only be blocked by creatures with horsemanship
        if (attackerKeywords.contains(Keyword.HORSEMANSHIP)) {
            if (!blockerKeywords.contains(Keyword.HORSEMANSHIP)) {
                return "${blockerCard.name} cannot block ${attackerCard.name} (horsemanship)"
            }
        }

        // Shadow: Can only be blocked by creatures with shadow
        if (attackerKeywords.contains(Keyword.SHADOW)) {
            if (!blockerKeywords.contains(Keyword.SHADOW)) {
                return "${blockerCard.name} cannot block ${attackerCard.name} (shadow)"
            }
        }

        // Skulk: Cannot be blocked by creatures with greater power
        // TODO: Implement skulk

        // Intimidate/Fear: Can only be blocked by artifact creatures or creatures sharing a color
        // TODO: Implement intimidate/fear

        return null
    }

    /**
     * Validate menace requirements (must be blocked by 2+ creatures).
     */
    private fun validateMenaceRequirements(
        state: GameState,
        blockers: Map<EntityId, List<EntityId>>
    ): String? {
        // Build a map of attackers to their blockers
        val attackerToBlockers = mutableMapOf<EntityId, MutableList<EntityId>>()
        for ((blockerId, attackerIds) in blockers) {
            for (attackerId in attackerIds) {
                attackerToBlockers.getOrPut(attackerId) { mutableListOf() }.add(blockerId)
            }
        }

        // Check each attacker with menace
        for ((attackerId, blockerList) in attackerToBlockers) {
            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue

            if (attackerCard.baseKeywords.contains(Keyword.MENACE)) {
                if (blockerList.size < 2) {
                    return "${attackerCard.name} has menace and must be blocked by 2 or more creatures"
                }
            }
        }

        return null
    }

    // =========================================================================
    // Combat Damage
    // =========================================================================

    /**
     * Calculate and apply combat damage.
     *
     * @param firstStrike If true, only creatures with first strike/double strike deal damage
     */
    fun applyCombatDamage(state: GameState, firstStrike: Boolean = false): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        // Find all attackers
        val attackers = state.findEntitiesWith<AttackingComponent>()

        for ((attackerId, attackingComponent) in attackers) {
            val attackerContainer = state.getEntity(attackerId) ?: continue
            val attackerCard = attackerContainer.get<CardComponent>() ?: continue
            val attackerKeywords = attackerCard.baseKeywords

            // Check if this attacker deals damage in this step
            val hasFirstStrike = attackerKeywords.contains(Keyword.FIRST_STRIKE)
            val hasDoubleStrike = attackerKeywords.contains(Keyword.DOUBLE_STRIKE)

            val dealsDamageThisStep = if (firstStrike) {
                hasFirstStrike || hasDoubleStrike
            } else {
                !hasFirstStrike || hasDoubleStrike
            }

            if (!dealsDamageThisStep) continue

            // Get attacker's power
            val power = attackerCard.baseStats?.basePower ?: 0
            if (power <= 0) continue

            // Check if blocked
            val blockedBy = attackerContainer.get<BlockedComponent>()

            if (blockedBy == null || blockedBy.blockerIds.isEmpty()) {
                // Unblocked - deal damage to defending player/planeswalker
                val defenderId = attackingComponent.defenderId
                val damageResult = dealDamageToPlayer(newState, defenderId, power, attackerId)
                newState = damageResult.newState
                events.addAll(damageResult.events)
            } else {
                // Blocked - deal damage to blockers and receive damage from them
                val (attackerDamageState, attackerEvents) = dealCombatDamageBetweenCreatures(
                    newState, attackerId, blockedBy.blockerIds, firstStrike
                )
                newState = attackerDamageState
                events.addAll(attackerEvents)
            }
        }

        // Check for lethal damage (state-based actions)
        val (postDamageState, deathEvents) = checkLethalDamage(newState)
        newState = postDamageState
        events.addAll(deathEvents)

        return ExecutionResult.success(newState, events)
    }

    /**
     * Deal damage to a player.
     */
    private fun dealDamageToPlayer(
        state: GameState,
        playerId: EntityId,
        amount: Int,
        sourceId: EntityId
    ): ExecutionResult {
        val playerContainer = state.getEntity(playerId)
            ?: return ExecutionResult.error(state, "Player not found: $playerId")

        val currentLife = playerContainer.get<LifeTotalComponent>()?.life
            ?: return ExecutionResult.error(state, "Player has no life total")

        val newLife = currentLife - amount
        val newState = state.updateEntity(playerId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        return ExecutionResult.success(
            newState,
            listOf(
                DamageDealtEvent(sourceId, playerId, amount, true),
                LifeChangedEvent(playerId, currentLife, newLife, LifeChangeReason.DAMAGE)
            )
        )
    }

    /**
     * Deal combat damage between an attacker and blockers.
     *
     * This uses the damage assignment order (if set) and applies damage according to CR 510:
     * - Damage must be assigned in order
     * - Each blocker must receive lethal before moving to the next
     * - Excess damage with trample goes to defending player
     */
    private fun dealCombatDamageBetweenCreatures(
        state: GameState,
        attackerId: EntityId,
        blockerIds: List<EntityId>,
        firstStrike: Boolean
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()

        val attackerContainer = newState.getEntity(attackerId) ?: return newState to events
        val attackerCard = attackerContainer.get<CardComponent>() ?: return newState to events
        val attackerPower = attackerCard.baseStats?.basePower ?: 0
        val attackerKeywords = attackerCard.baseKeywords
        val hasTrample = attackerKeywords.contains(Keyword.TRAMPLE)

        // Get blockers in damage assignment order (or default order)
        val orderedBlockers = attackerContainer.get<DamageAssignmentOrderComponent>()?.orderedBlockers
            ?: blockerIds

        // Check for manual damage assignment component
        val manualAssignment = attackerContainer.get<DamageAssignmentComponent>()

        // Calculate damage distribution
        val damageDistribution = if (manualAssignment != null) {
            // Use player-specified assignment
            manualAssignment.assignments
        } else {
            // Auto-calculate optimal distribution
            damageCalculator.calculateAutoDamageDistribution(newState, attackerId).assignments
        }

        // Apply attacker's damage to blockers (and potentially defending player with trample)
        for ((targetId, damage) in damageDistribution) {
            if (damage <= 0) continue

            // Check if target is a player or creature
            val targetContainer = newState.getEntity(targetId)
            val isPlayer = targetContainer?.get<LifeTotalComponent>() != null &&
                           targetContainer.get<CardComponent>() == null

            if (isPlayer) {
                // Deal damage to defending player (trample)
                val currentLife = targetContainer?.get<LifeTotalComponent>()?.life ?: 0
                val newLife = currentLife - damage
                newState = newState.updateEntity(targetId) { container ->
                    container.with(LifeTotalComponent(newLife))
                }
                events.add(DamageDealtEvent(attackerId, targetId, damage, true))
                events.add(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.DAMAGE))
            } else {
                // Deal damage to blocker
                val currentDamage = targetContainer?.get<DamageComponent>()?.amount ?: 0
                newState = newState.updateEntity(targetId) { container ->
                    container.with(DamageComponent(currentDamage + damage))
                }
                events.add(DamageDealtEvent(attackerId, targetId, damage, true))
            }
        }

        // Each blocker deals damage to attacker
        for (blockerId in orderedBlockers) {
            val blockerContainer = newState.getEntity(blockerId) ?: continue
            val blockerCard = blockerContainer.get<CardComponent>() ?: continue
            val blockerKeywords = blockerCard.baseKeywords

            // Check if blocker deals damage in this step
            val hasFirstStrike = blockerKeywords.contains(Keyword.FIRST_STRIKE)
            val hasDoubleStrike = blockerKeywords.contains(Keyword.DOUBLE_STRIKE)

            val dealsDamageThisStep = if (firstStrike) {
                hasFirstStrike || hasDoubleStrike
            } else {
                !hasFirstStrike || hasDoubleStrike
            }

            if (!dealsDamageThisStep) continue

            val blockerPower = blockerCard.baseStats?.basePower ?: 0
            if (blockerPower > 0) {
                val currentDamage = newState.getEntity(attackerId)?.get<DamageComponent>()?.amount ?: 0
                newState = newState.updateEntity(attackerId) { container ->
                    container.with(DamageComponent(currentDamage + blockerPower))
                }
                events.add(DamageDealtEvent(blockerId, attackerId, blockerPower, true))
            }
        }

        return newState to events
    }

    /**
     * Check for creatures with lethal damage and destroy them.
     */
    private fun checkLethalDamage(state: GameState): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for ((entityId, container) in state.entities) {
            val cardComponent = container.get<CardComponent>() ?: continue
            if (!cardComponent.typeLine.isCreature) continue

            val damage = container.get<DamageComponent>()?.amount ?: 0
            val toughness = cardComponent.baseStats?.baseToughness ?: 0

            if (damage >= toughness) {
                // Creature has lethal damage - would be destroyed by state-based actions
                // For now, just emit an event (actual destruction handled elsewhere)
                events.add(CreatureDestroyedEvent(entityId, cardComponent.name, "lethal damage"))
            }
        }

        return newState to events
    }

    // =========================================================================
    // End Combat
    // =========================================================================

    /**
     * Clear combat state at end of combat.
     */
    fun endCombat(state: GameState): ExecutionResult {
        var newState = state

        // Remove all combat-related components from all entities (creatures and players)
        for ((entityId, _) in state.entities) {
            newState = newState.updateEntity(entityId) { container ->
                container
                    .without<AttackingComponent>()
                    .without<BlockingComponent>()
                    .without<BlockedComponent>()
                    .without<DamageAssignmentComponent>()
                    .without<DamageAssignmentOrderComponent>()
                    .without<DealtFirstStrikeDamageComponent>()
                    .without<RequiresManualDamageAssignmentComponent>()
                    .without<AttackersDeclaredThisCombatComponent>()
                    .without<BlockersDeclaredThisCombatComponent>()
            }
        }

        return ExecutionResult.success(newState)
    }

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get all attacking creatures.
     */
    fun getAttackers(state: GameState): List<EntityId> {
        return state.findEntitiesWith<AttackingComponent>().map { it.first }
    }

    /**
     * Get all blocking creatures.
     */
    fun getBlockers(state: GameState): List<EntityId> {
        return state.findEntitiesWith<BlockingComponent>().map { it.first }
    }

    /**
     * Check if any creatures are attacking.
     */
    fun hasAttackers(state: GameState): Boolean {
        return state.findEntitiesWith<AttackingComponent>().isNotEmpty()
    }
}
