package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ReturnSelfToBattlefieldAttachedEffect
import kotlin.reflect.KClass

/**
 * Executor for ReturnSelfToBattlefieldAttachedEffect.
 *
 * Moves the source permanent (typically an Aura in the graveyard) to the battlefield
 * attached to the specified target creature. Used by the Dragon aura cycle
 * (Dragon Shadow, Dragon Breath, etc.).
 *
 * Steps:
 * 1. Resolve the attachment target (typically the triggering entity)
 * 2. Verify the source is in a non-battlefield zone (graveyard)
 * 3. Move source to the target creature's controller's battlefield
 * 4. Add AttachedToComponent pointing to the target
 * 5. Set up continuous effects from static abilities
 */
class ReturnSelfToBattlefieldAttachedExecutor(
    private val cardRegistry: CardRegistry? = null
) : EffectExecutor<ReturnSelfToBattlefieldAttachedEffect> {

    override val effectType: KClass<ReturnSelfToBattlefieldAttachedEffect> =
        ReturnSelfToBattlefieldAttachedEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnSelfToBattlefieldAttachedEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source entity for ReturnSelfToBattlefieldAttached")

        val attachTargetId = resolveTarget(effect.target, context, state)
            ?: return ExecutionResult.error(state, "No valid attachment target")

        // Verify the attachment target is on the battlefield
        val targetOnBattlefield = state.getBattlefield().contains(attachTargetId)
        if (!targetOnBattlefield) {
            return ExecutionResult.success(state) // Target left battlefield, do nothing
        }

        val sourceContainer = state.getEntity(sourceId)
            ?: return ExecutionResult.error(state, "Source entity not found: $sourceId")

        val cardComponent = sourceContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source is not a card: $sourceId")

        val ownerId = sourceContainer.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine source owner")

        // Find current zone
        val currentZone = findEntityZone(state, sourceId)
            ?: return ExecutionResult.error(state, "Source not found in any zone: $sourceId")

        // Don't return if already on battlefield
        if (currentZone.zoneType == Zone.BATTLEFIELD) {
            return ExecutionResult.success(state)
        }

        // Determine the controller: use the attachment target's controller
        val targetController = state.getEntity(attachTargetId)
            ?.get<ControllerComponent>()?.playerId ?: ownerId

        // Move from current zone to battlefield
        var newState = state.removeFromZone(currentZone, sourceId)
        val battlefieldZone = ZoneKey(targetController, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, sourceId)

        // Add controller and attachment components
        newState = newState.updateEntity(sourceId) { container ->
            var updated = container
                .with(ControllerComponent(targetController))
                .with(AttachedToComponent(attachTargetId))

            // Set up continuous effects from static abilities
            if (cardRegistry != null) {
                val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
                if (cardDef != null) {
                    val staticAbilityHandler = StaticAbilityHandler(cardRegistry)
                    updated = staticAbilityHandler.addContinuousEffectComponent(updated, cardDef)
                    updated = staticAbilityHandler.addReplacementEffectComponent(updated, cardDef)
                }
            }

            updated
        }

        val events = listOf(
            ZoneChangeEvent(
                entityId = sourceId,
                entityName = cardComponent.name,
                fromZone = currentZone.zoneType,
                toZone = Zone.BATTLEFIELD,
                ownerId = ownerId
            )
        )

        return ExecutionResult.success(newState, events)
    }

    private fun findEntityZone(state: GameState, entityId: com.wingedsheep.sdk.model.EntityId): ZoneKey? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) {
                return zoneKey
            }
        }
        return null
    }
}
