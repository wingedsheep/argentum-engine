package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.permanent.types.ensureDoubleFacedComponent
import com.wingedsheep.engine.handlers.effects.permanent.types.prepareDfcFaceSwap
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ReturnSelfToBattlefieldAttachedEffect
import kotlin.reflect.KClass

/**
 * Executor for ReturnSelfToBattlefieldAttachedEffect.
 *
 * Moves the source permanent (typically an Aura in the graveyard) to the battlefield
 * attached to the effect's target. Used by the Dragon aura cycle (Dragon Shadow, Dragon
 * Breath, etc.) and — attached to a *player*, and transformed — by Radiant Grace.
 *
 * Steps:
 * 1. Resolve the attachment host (a permanent, or a player for a Curse)
 * 2. Verify the source is in a non-battlefield zone (graveyard)
 * 3. Flip to the back face first when the effect returns the card transformed
 * 4. Move source to the new controller's battlefield
 * 5. Add AttachedToComponent pointing to the host
 * 6. Set up continuous effects from static abilities
 *
 * **Who controls the returned Aura** differs by host, and the two readings are printed
 * differently. A *permanent* host hands control to that permanent's controller — the Dragon
 * cycle's Aura follows its creature, which is the only sensible reading of "attached to that
 * creature" with no controller clause. A *player* host leaves it under the ability's controller:
 * "return this card to the battlefield transformed **under your control** attached to target
 * opponent" would otherwise hand the curse to the very player it curses.
 *
 * The face swap happens **before** the move, so the battlefield entry registers the back face's
 * static abilities and replacement effects rather than the front's. Per the standing ruling a
 * single-faced card told to enter transformed doesn't move at all, so a card with no back face is
 * a quiet no-op, not an error.
 */
class ReturnSelfToBattlefieldAttachedExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<ReturnSelfToBattlefieldAttachedEffect> {

    override val effectType: KClass<ReturnSelfToBattlefieldAttachedEffect> =
        ReturnSelfToBattlefieldAttachedEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnSelfToBattlefieldAttachedEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "No source entity for ReturnSelfToBattlefieldAttached")

        val attachTargetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid attachment target")

        // The host is either a player still in the game or a permanent still on the battlefield.
        // A host that has since left (creature died, player lost) makes the return do nothing —
        // the Aura stays where it is rather than entering unattached and immediately dying to
        // CR 704.5m.
        val hostIsPlayer = attachTargetId in state.turnOrder
        if (!hostIsPlayer && !state.getBattlefield().contains(attachTargetId)) {
            return EffectResult.success(state)
        }

        val sourceContainer = state.getEntity(sourceId)
            ?: return EffectResult.error(state, "Source entity not found: $sourceId")

        val cardComponent = sourceContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Source is not a card: $sourceId")

        val ownerId = sourceContainer.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return EffectResult.error(state, "Cannot determine source owner")

        // Find current zone
        val currentZone = findEntityZone(state, sourceId)
            ?: return EffectResult.error(state, "Source not found in any zone: $sourceId")

        // Don't return if already on battlefield
        if (currentZone.zoneType == Zone.BATTLEFIELD) {
            return EffectResult.success(state)
        }

        // "under your control" for a player host; an Aura on a permanent follows its host.
        val newControllerId = if (hostIsPlayer) {
            context.controllerId
        } else {
            state.getEntity(attachTargetId)?.get<ControllerComponent>()?.playerId ?: ownerId
        }

        // Flip to the back face while the card is still in its old zone, so the entry below sees
        // the face that is actually going to be up.
        var newState = state
        if (effect.transformed) {
            newState = ensureDoubleFacedComponent(newState, cardRegistry, sourceId)
                ?.let { prepareDfcFaceSwap(it, cardRegistry, sourceId, DoubleFacedComponent.Face.BACK) }
                ?: return EffectResult.success(state)
        }

        // The name and definition to stamp are the *entering* face's, which the swap above may
        // have just changed.
        val enteringCard = newState.getEntity(sourceId)?.get<CardComponent>() ?: cardComponent

        // Move from current zone to battlefield
        newState = newState.removeFromZone(currentZone, sourceId)
        newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
            .place(newState, newControllerId, sourceId)

        // Add controller and attachment components
        newState = newState.updateEntity(sourceId) { container ->
            var updated = container
                .with(ControllerComponent(newControllerId))
                .with(AttachedToComponent(attachTargetId))

            // Set up continuous effects from static abilities
            val cardDef = cardRegistry.getCard(enteringCard.cardDefinitionId)
            if (cardDef != null) {
                val staticAbilityHandler = StaticAbilityHandler(cardRegistry)
                updated = staticAbilityHandler.addContinuousEffectComponent(updated, cardDef)
                updated = staticAbilityHandler.addReplacementEffectComponent(updated, cardDef)
            }

            updated
        }

        val events = listOf(
            ZoneChangeEvent(
                entityId = sourceId,
                entityName = enteringCard.name,
                fromZone = currentZone.zoneType,
                toZone = Zone.BATTLEFIELD,
                ownerId = ownerId,
                oldObject = state.objectRef(sourceId),
                newObject = newState.objectRef(sourceId)
            )
        )

        return EffectResult.success(newState, events)
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
