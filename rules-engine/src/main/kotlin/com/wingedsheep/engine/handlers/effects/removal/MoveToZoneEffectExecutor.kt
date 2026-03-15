package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.cleanupCombatReferences
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.cleanupReverseAttachmentLink
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.destroyPermanent
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.moveCardToZone
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.stripBattlefieldComponents
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import kotlin.reflect.KClass

/**
 * Executor for MoveToZoneEffect.
 * Unified zone-moving effect that consolidates destroy, exile, bounce,
 * shuffle-into-library, put-on-top, etc.
 */
class MoveToZoneEffectExecutor(
    private val cardRegistry: CardRegistry? = null
) : EffectExecutor<MoveToZoneEffect> {

    override val effectType: KClass<MoveToZoneEffect> = MoveToZoneEffect::class

    override fun execute(
        state: GameState,
        effect: MoveToZoneEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context, state)
            ?: return ExecutionResult.error(state, "No valid target for move to zone")

        // byDestruction delegates to destroyPermanent (handles indestructible)
        if (effect.byDestruction) {
            return destroyPermanent(state, targetId)
        }

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target entity not found: $targetId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card: $targetId")

        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine card owner")

        val currentZone = findEntityZone(state, targetId)
            ?: return ExecutionResult.error(state, "Card not found in any zone: $targetId")

        // If fromZone is specified, skip the move if the target is not in the expected zone
        if (effect.fromZone != null && currentZone.zoneType != effect.fromZone) {
            return ExecutionResult.success(state, emptyList())
        }

        // Resolve controller override for "under your control" effects
        val controllerOverride = effect.controllerOverride
        val controllerId = if (controllerOverride != null && effect.destination == Zone.BATTLEFIELD) {
            resolveTarget(controllerOverride, context, state) ?: ownerId
        } else {
            ownerId
        }

        // Face-down battlefield entry
        if (effect.faceDown && effect.destination == Zone.BATTLEFIELD) {
            return moveToBattlefieldFaceDown(state, targetId, cardComponent, ownerId, controllerId, currentZone)
        }

        // positionFromTop takes precedence for library destination
        val posFromTop = effect.positionFromTop
        if (posFromTop != null && effect.destination == Zone.LIBRARY) {
            return moveToLibraryNthFromTop(state, targetId, cardComponent, ownerId, currentZone, posFromTop)
        }

        val result = when (effect.placement) {
            ZonePlacement.Top -> moveToLibraryTop(state, targetId, cardComponent, ownerId, currentZone)
            ZonePlacement.Bottom -> moveToLibraryBottom(state, targetId, cardComponent, ownerId, currentZone)
            ZonePlacement.Shuffled -> moveToLibraryShuffled(state, targetId, cardComponent, ownerId, currentZone)
            ZonePlacement.Tapped -> moveToBattlefieldTapped(state, targetId, cardComponent, controllerId, currentZone)
            ZonePlacement.TappedAndAttacking -> moveToBattlefieldTappedAndAttacking(state, targetId, cardComponent, controllerId, currentZone)
            ZonePlacement.Default -> {
                if (controllerId != ownerId && effect.destination == Zone.BATTLEFIELD) {
                    moveToBattlefieldUnderControl(state, targetId, cardComponent, ownerId, controllerId, currentZone)
                } else {
                    moveCardToZone(state, targetId, effect.destination)
                }
            }
        }

        // Link exiled card to source permanent via LinkedExileComponent
        if (effect.linkToSource && effect.destination == Zone.EXILE && result.error == null) {
            val sourceId = context.sourceId ?: return result
            val sourceContainer = result.state.getEntity(sourceId) ?: return result
            val existingLinked = sourceContainer.get<LinkedExileComponent>()
            val allExiled = (existingLinked?.exiledIds ?: emptyList()) + listOf(targetId)
            val linkedState = result.state.updateEntity(sourceId) { c ->
                c.with(LinkedExileComponent(allExiled))
            }
            return ExecutionResult(state = linkedState, events = result.events)
        }

        return result
    }

    private fun moveToLibraryTop(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        currentZone: ZoneKey
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
        val currentLibrary = newState.getZone(libraryZone)
        val newLibrary = listOf(entityId) + currentLibrary
        newState = newState.copy(zones = newState.zones + (libraryZone to newLibrary))

        newState = cleanupBattlefieldComponents(newState, entityId, currentZone)

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = currentZone.zoneType,
                    toZone = Zone.LIBRARY,
                    ownerId = ownerId
                )
            )
        )
    }

    private fun moveToLibraryBottom(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        currentZone: ZoneKey
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
        val currentLibrary = newState.getZone(libraryZone)
        val newLibrary = currentLibrary + entityId
        newState = newState.copy(zones = newState.zones + (libraryZone to newLibrary))

        newState = cleanupBattlefieldComponents(newState, entityId, currentZone)

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = currentZone.zoneType,
                    toZone = Zone.LIBRARY,
                    ownerId = ownerId
                )
            )
        )
    }

    /**
     * Move a card to a specific position in the owner's library (0-indexed from top).
     * For positionFromTop=2, the card becomes 3rd from the top.
     * If the library has fewer cards than the requested position, the card goes to the bottom.
     */
    private fun moveToLibraryNthFromTop(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        currentZone: ZoneKey,
        positionFromTop: Int
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
        val currentLibrary = newState.getZone(libraryZone)

        // If the library is too small, put at the bottom (or wherever possible)
        val insertIndex = positionFromTop.coerceAtMost(currentLibrary.size)
        val newLibrary = currentLibrary.toMutableList().apply { add(insertIndex, entityId) }
        newState = newState.copy(zones = newState.zones + (libraryZone to newLibrary))

        newState = cleanupBattlefieldComponents(newState, entityId, currentZone)

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = currentZone.zoneType,
                    toZone = Zone.LIBRARY,
                    ownerId = ownerId
                )
            )
        )
    }

    private fun moveToLibraryShuffled(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        currentZone: ZoneKey
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
        newState = newState.addToZone(libraryZone, entityId)

        // Shuffle the library
        val library = newState.getZone(libraryZone).shuffled()
        newState = newState.copy(zones = newState.zones + (libraryZone to library))

        newState = cleanupBattlefieldComponents(newState, entityId, currentZone)

        val events = listOf<GameEvent>(
            ZoneChangeEvent(
                entityId = entityId,
                entityName = cardComponent.name,
                fromZone = currentZone.zoneType,
                toZone = Zone.LIBRARY,
                ownerId = ownerId
            ),
            LibraryShuffledEvent(ownerId)
        )

        return ExecutionResult.success(newState, events)
    }

    private fun moveToBattlefieldTapped(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        currentZone: ZoneKey
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        val battlefieldZone = ZoneKey(ownerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, entityId)

        newState = newState.updateEntity(entityId) { c ->
            c.with(ControllerComponent(ownerId))
                .with(TappedComponent)
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = currentZone.zoneType,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = ownerId
                )
            )
        )
    }

    /**
     * Move a card to the battlefield tapped and attacking.
     * Used for effects like Meandering Towershell that return "tapped and attacking."
     * In a 2-player game, the opponent is automatically chosen as the defender.
     */
    private fun moveToBattlefieldTappedAndAttacking(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        controllerId: EntityId,
        currentZone: ZoneKey
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, entityId)

        // Find the opponent to attack (in 2-player, there's only one)
        val defenderId = state.turnOrder.firstOrNull { it != controllerId }
            ?: return ExecutionResult.error(state, "No valid defender for tapped-and-attacking creature")

        newState = newState.updateEntity(entityId) { c ->
            c.with(ControllerComponent(controllerId))
                .with(TappedComponent)
                .with(AttackingComponent(defenderId))
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = currentZone.zoneType,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = controllerId
                )
            )
        )
    }

    private fun moveToBattlefieldUnderControl(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        controllerId: EntityId,
        currentZone: ZoneKey
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        // Card enters the controller's battlefield zone
        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, entityId)

        newState = cleanupBattlefieldComponents(newState, entityId, currentZone)

        newState = newState.updateEntity(entityId) { c ->
            c.with(ControllerComponent(controllerId))
                .with(SummoningSicknessComponent)
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = currentZone.zoneType,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = ownerId
                )
            )
        )
    }

    /**
     * Move a card to the battlefield face down as a 2/2 morph creature.
     * Adds FaceDownComponent and MorphDataComponent (looked up from card registry).
     */
    private fun moveToBattlefieldFaceDown(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        ownerId: EntityId,
        controllerId: EntityId,
        currentZone: ZoneKey
    ): ExecutionResult {
        var newState = state.removeFromZone(currentZone, entityId)

        val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, entityId)

        newState = cleanupBattlefieldComponents(newState, entityId, currentZone)

        newState = newState.updateEntity(entityId) { c ->
            var updated = c.with(ControllerComponent(controllerId))
                .with(SummoningSicknessComponent)
                .with(FaceDownComponent)

            // Look up morph cost from card definition so the creature can be turned face up
            val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId)
            val morphAbility = cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Morph>()?.firstOrNull()
            if (morphAbility != null) {
                updated = updated.with(MorphDataComponent(morphAbility.morphCost, cardComponent.cardDefinitionId, morphAbility.faceUpEffect))
            }

            updated
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = currentZone.zoneType,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = ownerId
                )
            )
        )
    }

    /**
     * Remove battlefield-specific components when leaving the battlefield.
     */
    private fun cleanupBattlefieldComponents(
        state: GameState,
        entityId: EntityId,
        fromZone: ZoneKey
    ): GameState {
        if (fromZone.zoneType != Zone.BATTLEFIELD) return state
        var cleaned = cleanupReverseAttachmentLink(state, entityId)
        cleaned = cleanupCombatReferences(cleaned, entityId)
        return cleaned.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }
    }

    private fun findEntityZone(state: GameState, entityId: EntityId): ZoneKey? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) {
                return zoneKey
            }
        }
        return null
    }
}
