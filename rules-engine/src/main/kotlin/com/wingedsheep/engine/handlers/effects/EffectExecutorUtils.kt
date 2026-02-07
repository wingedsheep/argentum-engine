package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.Player

/**
 * Utility functions shared across effect executors.
 */
object EffectExecutorUtils {

    private val stateProjector = StateProjector()

    /**
     * Resolve a target from the effect target definition and context.
     */
    fun resolveTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? {
        return when (effectTarget) {
            is EffectTarget.Self -> context.sourceId
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.ContextTarget -> context.targets.getOrNull(effectTarget.index)?.toEntityId()
            is EffectTarget.SpecificEntity -> effectTarget.entityId
            is EffectTarget.TriggeringEntity -> context.triggeringEntityId
            else -> null
        }
    }

    /**
     * Resolve a player target from the effect target definition and context.
     */
    fun resolvePlayerTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? {
        return when (effectTarget) {
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.ContextTarget -> context.targets.getOrNull(effectTarget.index)?.toEntityId()
            is EffectTarget.PlayerRef -> when (effectTarget.player) {
                Player.You -> context.controllerId
                Player.Opponent, Player.TargetOpponent -> context.opponentId
                Player.TargetPlayer, Player.Any -> context.targets.firstOrNull()?.toEntityId()
                else -> null
            }
            else -> null
        }
    }

    /**
     * Resolve a player target to a list of player IDs (for multi-player effects like "each player").
     */
    fun resolvePlayerTargets(effectTarget: EffectTarget, state: GameState, context: EffectContext): List<EntityId> {
        return when (effectTarget) {
            is EffectTarget.Controller -> listOf(context.controllerId)
            is EffectTarget.PlayerRef -> when (effectTarget.player) {
                Player.Each -> state.turnOrder
                Player.EachOpponent -> state.turnOrder.filter { it != context.controllerId }
                Player.You -> listOf(context.controllerId)
                Player.Opponent, Player.TargetOpponent -> state.turnOrder.filter { it != context.controllerId }
                Player.TargetPlayer, Player.Any -> {
                    context.targets.firstOrNull()?.toEntityId()?.let { listOf(it) } ?: emptyList()
                }
                else -> emptyList()
            }
            else -> resolvePlayerTarget(effectTarget, context)?.let { listOf(it) } ?: emptyList()
        }
    }

    /**
     * Convert a ChosenTarget to an EntityId.
     */
    fun ChosenTarget.toEntityId(): EntityId = when (this) {
        is ChosenTarget.Player -> playerId
        is ChosenTarget.Permanent -> entityId
        is ChosenTarget.Card -> cardId
        is ChosenTarget.Spell -> spellEntityId
    }

    /**
     * Deal damage to a target (player or creature).
     *
     * @param state The current game state
     * @param targetId The entity to deal damage to
     * @param amount The amount of damage
     * @param sourceId The source of the damage
     * @param cantBePrevented If true, this damage cannot be prevented by prevention effects
     * @return The execution result with updated state and events
     */
    fun dealDamageToTarget(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        sourceId: EntityId?,
        cantBePrevented: Boolean = false
    ): ExecutionResult {
        if (amount <= 0) return ExecutionResult.success(state)

        // Protection from color/subtype: damage from sources of the stated quality is prevented (Rule 702.16)
        if (!cantBePrevented && sourceId != null) {
            val projected = stateProjector.project(state)
            val sourceColors = projected.getColors(sourceId)
            for (colorName in sourceColors) {
                if (projected.hasKeyword(targetId, "PROTECTION_FROM_$colorName")) {
                    // Damage is prevented — return success with no state change
                    return ExecutionResult.success(state)
                }
            }
            val sourceSubtypes = projected.getSubtypes(sourceId)
            for (subtype in sourceSubtypes) {
                if (projected.hasKeyword(targetId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")) {
                    return ExecutionResult.success(state)
                }
            }
        }

        // Apply damage prevention shields (Prevent the next X damage)
        var effectiveAmount = amount
        var newState = state
        if (!cantBePrevented) {
            val (shieldState, reducedAmount) = applyDamagePreventionShields(newState, targetId, effectiveAmount)
            newState = shieldState
            effectiveAmount = reducedAmount
        }
        if (effectiveAmount <= 0) return ExecutionResult.success(newState)

        val events = mutableListOf<EngineGameEvent>()

        // Check if target is a player or creature
        val lifeComponent = newState.getEntity(targetId)?.get<LifeTotalComponent>()
        if (lifeComponent != null) {
            // It's a player - reduce life
            val newLife = lifeComponent.life - effectiveAmount
            newState = newState.updateEntity(targetId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            events.add(LifeChangedEvent(targetId, lifeComponent.life, newLife, LifeChangeReason.DAMAGE))
        } else {
            // It's a creature - mark damage
            val currentDamage = newState.getEntity(targetId)?.get<DamageComponent>()?.amount ?: 0
            newState = newState.updateEntity(targetId) { container ->
                container.with(DamageComponent(currentDamage + effectiveAmount))
            }
        }

        events.add(DamageDealtEvent(sourceId, targetId, effectiveAmount, false))

        return ExecutionResult.success(newState, events)
    }

    /**
     * Destroy a permanent (move to graveyard).
     *
     * @param state The current game state
     * @param entityId The entity to destroy
     * @param canRegenerate If false, regeneration shields are not checked (e.g. Wrath of God)
     * @return The execution result with updated state and events
     */
    fun destroyPermanent(state: GameState, entityId: EntityId, canRegenerate: Boolean = true): ExecutionResult {
        val container = state.getEntity(entityId)
            ?: return ExecutionResult.error(state, "Entity not found: $entityId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card: $entityId")

        // Check for indestructible - indestructible permanents can't be destroyed
        if (stateProjector.hasProjectedKeyword(state, entityId, Keyword.INDESTRUCTIBLE)) {
            // Indestructible - the destroy effect has no effect (not an error)
            return ExecutionResult.success(state)
        }

        // Check for regeneration shields
        if (canRegenerate) {
            val (shieldState, wasRegenerated) = applyRegenerationShields(state, entityId)
            if (wasRegenerated) {
                return applyRegenerationReplacement(shieldState, entityId)
            }
        }

        // Find which player's battlefield it's on
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        val ownerId = cardComponent.ownerId ?: controllerId

        // Move to graveyard
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

        var newState = state.removeFromZone(battlefieldZone, entityId)
        newState = newState.addToZone(graveyardZone, entityId)

        // Remove permanent-only components
        newState = newState.updateEntity(entityId) { c ->
            c.without<ControllerComponent>()
                .without<TappedComponent>()
                .without<SummoningSicknessComponent>()
                .without<DamageComponent>()
                .without<CountersComponent>()
                .without<FaceDownComponent>()
                .without<MorphDataComponent>()
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId,
                    cardComponent.name,
                    Zone.BATTLEFIELD,
                    Zone.GRAVEYARD,
                    ownerId
                )
            )
        )
    }

    /**
     * Move a card from any zone to another zone.
     * Finds the card's current zone and moves it to the destination.
     *
     * @param state The current game state
     * @param entityId The entity to move
     * @param targetZone The destination zone type
     * @return The execution result with updated state and events
     */
    fun moveCardToZone(state: GameState, entityId: EntityId, targetZone: Zone): ExecutionResult {
        val container = state.getEntity(entityId)
            ?: return ExecutionResult.error(state, "Entity not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        val ownerId = cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        // Find the current zone of the card
        val currentZone = state.zones.entries.find { (_, cards) -> entityId in cards }?.key
            ?: return ExecutionResult.error(state, "Card not in any zone")

        // Move to target zone
        val targetZoneKey = ZoneKey(ownerId, targetZone)

        var newState = state.removeFromZone(currentZone, entityId)
        newState = newState.addToZone(targetZoneKey, entityId)

        // Remove permanent-only components if moving from battlefield
        if (currentZone.zoneType == Zone.BATTLEFIELD) {
            newState = newState.updateEntity(entityId) { c ->
                c.without<ControllerComponent>()
                    .without<TappedComponent>()
                    .without<SummoningSicknessComponent>()
                    .without<DamageComponent>()
                    .without<FaceDownComponent>()
                    .without<MorphDataComponent>()
            }
        }

        // Add controller component when moving to battlefield
        if (targetZone == Zone.BATTLEFIELD) {
            newState = newState.updateEntity(entityId) { c ->
                c.with(ControllerComponent(ownerId))
                    .with(SummoningSicknessComponent)
            }
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId,
                    cardComponent.name,
                    currentZone.zoneType,
                    targetZone,
                    ownerId
                )
            )
        )
    }

    /**
     * Apply damage prevention shields to reduce incoming damage.
     *
     * Finds all PreventNextDamage floating effects targeting the entity,
     * consumes shield amounts, and returns the updated state and remaining damage.
     *
     * @param state The current game state
     * @param targetId The entity receiving damage
     * @param amount The incoming damage amount
     * @return Pair of (updated state with consumed shields, remaining damage after prevention)
     */
    fun applyDamagePreventionShields(state: GameState, targetId: EntityId, amount: Int): Pair<GameState, Int> {
        var remainingDamage = amount
        val updatedEffects = state.floatingEffects.toMutableList()
        val toRemove = mutableListOf<Int>()

        for (i in updatedEffects.indices) {
            if (remainingDamage <= 0) break
            val effect = updatedEffects[i]
            val mod = effect.effect.modification
            if (mod is SerializableModification.PreventNextDamage && targetId in effect.effect.affectedEntities) {
                val prevented = minOf(mod.remainingAmount, remainingDamage)
                remainingDamage -= prevented
                val newRemaining = mod.remainingAmount - prevented
                if (newRemaining <= 0) {
                    toRemove.add(i)
                } else {
                    updatedEffects[i] = effect.copy(
                        effect = effect.effect.copy(
                            modification = SerializableModification.PreventNextDamage(newRemaining)
                        )
                    )
                }
            }
        }

        // Remove fully consumed shields in reverse order to maintain indices
        for (idx in toRemove.asReversed()) {
            updatedEffects.removeAt(idx)
        }

        return state.copy(floatingEffects = updatedEffects) to remainingDamage
    }

    /**
     * Check for regeneration shields on an entity and consume one if found.
     *
     * Regeneration shields are floating effects with RegenerationShield modification.
     * If a CantBeRegenerated floating effect targets the entity, regeneration is blocked.
     *
     * @param state The current game state
     * @param entityId The entity being destroyed
     * @return Pair of (updated state with consumed shield, whether regeneration occurred)
     */
    fun applyRegenerationShields(state: GameState, entityId: EntityId): Pair<GameState, Boolean> {
        // Check if the entity has a CantBeRegenerated floating effect
        val cantRegenerate = state.floatingEffects.any { effect ->
            effect.effect.modification is SerializableModification.CantBeRegenerated &&
                entityId in effect.effect.affectedEntities
        }
        if (cantRegenerate) return state to false

        // Find the first regeneration shield targeting this entity
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.RegenerationShield &&
                entityId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return state to false

        // Consume the shield (remove it)
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)

        return state.copy(floatingEffects = updatedEffects) to true
    }

    /**
     * Apply regeneration replacement effect: tap the creature, remove all damage,
     * and remove it from combat. The creature stays on the battlefield.
     *
     * @param state The current game state (shield already consumed)
     * @param entityId The entity being regenerated
     * @return ExecutionResult with the regenerated creature still on the battlefield
     */
    fun applyRegenerationReplacement(state: GameState, entityId: EntityId): ExecutionResult {
        val newState = state.updateEntity(entityId) { c ->
            c.with(TappedComponent)
                .without<DamageComponent>()
                .without<AttackingComponent>()
                .without<BlockingComponent>()
                .without<BlockedComponent>()
                .without<DamageAssignmentComponent>()
                .without<DamageAssignmentOrderComponent>()
        }
        return ExecutionResult.success(newState)
    }

    /**
     * Move a permanent from battlefield to another zone, cleaning up permanent-only components.
     *
     * @param state The current game state
     * @param entityId The entity to move
     * @param targetZone The destination zone type
     * @return The execution result with updated state and events
     */
    fun movePermanentToZone(state: GameState, entityId: EntityId, targetZone: Zone): ExecutionResult {
        val container = state.getEntity(entityId)
            ?: return ExecutionResult.error(state, "Entity not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        val ownerId = cardComponent.ownerId ?: controllerId

        // Move to target zone
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        val targetZoneKey = ZoneKey(ownerId, targetZone)

        var newState = state.removeFromZone(battlefieldZone, entityId)
        newState = newState.addToZone(targetZoneKey, entityId)

        // Remove permanent-only components
        newState = newState.updateEntity(entityId) { c ->
            c.without<ControllerComponent>()
                .without<TappedComponent>()
                .without<SummoningSicknessComponent>()
                .without<DamageComponent>()
                .without<FaceDownComponent>()
                .without<MorphDataComponent>()
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId,
                    cardComponent.name,
                    Zone.BATTLEFIELD,
                    targetZone,
                    ownerId
                )
            )
        )
    }
}
