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
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ZoneType
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
            is EffectTarget.TargetCreature,
            is EffectTarget.TargetPermanent,
            is EffectTarget.AnyTarget -> context.targets.firstOrNull()?.toEntityId()
            else -> null
        }
    }

    /**
     * Resolve a player target from the effect target definition and context.
     */
    fun resolvePlayerTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? {
        return when (effectTarget) {
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.Opponent -> context.opponentId
            is EffectTarget.AnyPlayer -> context.targets.firstOrNull()?.toEntityId()
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
            is EffectTarget.EachPlayer -> state.turnOrder
            is EffectTarget.EachOpponent -> state.turnOrder.filter { it != context.controllerId }
            is EffectTarget.Controller -> listOf(context.controllerId)
            is EffectTarget.Opponent -> state.turnOrder.filter { it != context.controllerId }
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

        // TODO: When damage prevention is implemented, check for prevention effects here.
        // If cantBePrevented is true, skip the prevention check entirely.

        val events = mutableListOf<EngineGameEvent>()
        var newState = state

        // Check if target is a player or creature
        val lifeComponent = state.getEntity(targetId)?.get<LifeTotalComponent>()
        if (lifeComponent != null) {
            // It's a player - reduce life
            val newLife = lifeComponent.life - amount
            newState = newState.updateEntity(targetId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            events.add(LifeChangedEvent(targetId, lifeComponent.life, newLife, LifeChangeReason.DAMAGE))
        } else {
            // It's a creature - mark damage
            val currentDamage = state.getEntity(targetId)?.get<DamageComponent>()?.amount ?: 0
            newState = newState.updateEntity(targetId) { container ->
                container.with(DamageComponent(currentDamage + amount))
            }
        }

        events.add(DamageDealtEvent(sourceId, targetId, amount, false))

        return ExecutionResult.success(newState, events)
    }

    /**
     * Destroy a permanent (move to graveyard).
     *
     * @param state The current game state
     * @param entityId The entity to destroy
     * @return The execution result with updated state and events
     */
    fun destroyPermanent(state: GameState, entityId: EntityId): ExecutionResult {
        val container = state.getEntity(entityId)
            ?: return ExecutionResult.error(state, "Entity not found: $entityId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card: $entityId")

        // Check for indestructible - indestructible permanents can't be destroyed
        if (stateProjector.hasProjectedKeyword(state, entityId, Keyword.INDESTRUCTIBLE)) {
            // Indestructible - the destroy effect has no effect (not an error)
            return ExecutionResult.success(state)
        }

        // Find which player's battlefield it's on
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        val ownerId = cardComponent.ownerId ?: controllerId

        // Move to graveyard
        val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
        val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)

        var newState = state.removeFromZone(battlefieldZone, entityId)
        newState = newState.addToZone(graveyardZone, entityId)

        // Remove permanent-only components
        newState = newState.updateEntity(entityId) { c ->
            c.without<ControllerComponent>()
                .without<TappedComponent>()
                .without<SummoningSicknessComponent>()
                .without<DamageComponent>()
                .without<CountersComponent>()
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId,
                    cardComponent.name,
                    ZoneType.BATTLEFIELD,
                    ZoneType.GRAVEYARD,
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
     * @param targetZoneType The destination zone type
     * @return The execution result with updated state and events
     */
    fun moveCardToZone(state: GameState, entityId: EntityId, targetZoneType: ZoneType): ExecutionResult {
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
        val targetZone = ZoneKey(ownerId, targetZoneType)

        var newState = state.removeFromZone(currentZone, entityId)
        newState = newState.addToZone(targetZone, entityId)

        // Remove permanent-only components if moving from battlefield
        if (currentZone.zoneType == ZoneType.BATTLEFIELD) {
            newState = newState.updateEntity(entityId) { c ->
                c.without<ControllerComponent>()
                    .without<TappedComponent>()
                    .without<SummoningSicknessComponent>()
                    .without<DamageComponent>()
            }
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId,
                    cardComponent.name,
                    currentZone.zoneType,
                    targetZoneType,
                    ownerId
                )
            )
        )
    }

    /**
     * Move a permanent from battlefield to another zone, cleaning up permanent-only components.
     *
     * @param state The current game state
     * @param entityId The entity to move
     * @param targetZoneType The destination zone type
     * @return The execution result with updated state and events
     */
    fun movePermanentToZone(state: GameState, entityId: EntityId, targetZoneType: ZoneType): ExecutionResult {
        val container = state.getEntity(entityId)
            ?: return ExecutionResult.error(state, "Entity not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        val ownerId = cardComponent.ownerId ?: controllerId

        // Move to target zone
        val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
        val targetZone = ZoneKey(ownerId, targetZoneType)

        var newState = state.removeFromZone(battlefieldZone, entityId)
        newState = newState.addToZone(targetZone, entityId)

        // Remove permanent-only components
        newState = newState.updateEntity(entityId) { c ->
            c.without<ControllerComponent>()
                .without<TappedComponent>()
                .without<SummoningSicknessComponent>()
                .without<DamageComponent>()
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId,
                    cardComponent.name,
                    ZoneType.BATTLEFIELD,
                    targetZoneType,
                    ownerId
                )
            )
        )
    }
}
