package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.LibraryPlacement
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.destroyPermanent
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import kotlin.reflect.KClass

/**
 * Executor for MoveToZoneEffect.
 * Unified zone-moving effect that consolidates destroy, exile, bounce,
 * shuffle-into-library, put-on-top, etc.
 *
 * Delegates all zone movement to [ZoneTransitionService] for consistent cleanup.
 */
class MoveToZoneEffectExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<MoveToZoneEffect> {

    override val effectType: KClass<MoveToZoneEffect> = MoveToZoneEffect::class

    override fun execute(
        state: GameState,
        effect: MoveToZoneEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target, state)
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
            context.resolveTarget(controllerOverride, state) ?: ownerId
        } else {
            ownerId
        }

        // Build ZoneEntryOptions based on placement and effect properties
        val entryOptions = buildEntryOptions(effect, cardComponent, controllerId)

        val transitionResult = ZoneTransitionService.moveToZone(
            state, targetId, effect.destination, entryOptions, currentZone
        )

        var resultState = transitionResult.state

        // Link exiled card to source permanent via LinkedExileComponent
        if (effect.linkToSource && effect.destination == Zone.EXILE) {
            val sourceId = context.sourceId ?: return ExecutionResult.success(resultState, transitionResult.events)
            val sourceContainer = resultState.getEntity(sourceId)
                ?: return ExecutionResult.success(resultState, transitionResult.events)
            val existingLinked = sourceContainer.get<LinkedExileComponent>()
            val allExiled = (existingLinked?.exiledIds ?: emptyList()) + listOf(targetId)
            resultState = resultState.updateEntity(sourceId) { c ->
                c.with(LinkedExileComponent(allExiled))
            }
        }

        return ExecutionResult.success(resultState, transitionResult.events)
    }

    /**
     * Build [ZoneEntryOptions] from the effect's placement and properties.
     */
    private fun buildEntryOptions(
        effect: MoveToZoneEffect,
        cardComponent: CardComponent,
        controllerId: com.wingedsheep.sdk.model.EntityId
    ): ZoneEntryOptions {
        val libraryPlacement = when {
            effect.positionFromTop != null && effect.destination == Zone.LIBRARY ->
                LibraryPlacement.NthFromTop(effect.positionFromTop!!)
            effect.placement == ZonePlacement.Top -> LibraryPlacement.Top
            effect.placement == ZonePlacement.Bottom -> LibraryPlacement.Bottom
            effect.placement == ZonePlacement.Shuffled -> LibraryPlacement.Shuffled
            else -> LibraryPlacement.Top
        }

        // Look up morph data for face-down entry
        val morphData = if (effect.faceDown && effect.destination == Zone.BATTLEFIELD) {
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            val morphAbility = cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Morph>()?.firstOrNull()
            if (morphAbility != null) {
                MorphDataComponent(morphAbility.morphCost, cardComponent.cardDefinitionId, morphAbility.faceUpEffect)
            } else null
        } else null

        return ZoneEntryOptions(
            controllerId = controllerId,
            libraryPlacement = libraryPlacement,
            tapped = effect.placement == ZonePlacement.Tapped,
            tappedAndAttacking = effect.placement == ZonePlacement.TappedAndAttacking,
            faceDown = effect.faceDown && effect.destination == Zone.BATTLEFIELD,
            morphData = morphData
        )
    }

    private fun findEntityZone(state: GameState, entityId: com.wingedsheep.sdk.model.EntityId): com.wingedsheep.engine.state.ZoneKey? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) {
                return zoneKey
            }
        }
        return null
    }
}
