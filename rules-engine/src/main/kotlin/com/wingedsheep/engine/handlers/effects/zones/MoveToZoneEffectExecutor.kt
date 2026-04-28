package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
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
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for move to zone")

        // byDestruction delegates to destroyPermanent (handles indestructible)
        if (effect.byDestruction) {
            return destroyPermanent(state, targetId)
        }

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target entity not found: $targetId")

        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card: $targetId")

        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return EffectResult.error(state, "Cannot determine card owner")

        val currentZone = findEntityZone(state, targetId)
            ?: return EffectResult.error(state, "Card not found in any zone: $targetId")

        // If fromZone is specified, skip the move if the target is not in the expected zone
        if (effect.fromZone != null && currentZone.zoneType != effect.fromZone) {
            return EffectResult.success(state, emptyList())
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
        val extraEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        // Apply "enters with counters" replacement effects when a permanent enters the
        // battlefield from a non-stack zone (e.g., reanimation from graveyard, return from
        // exile). Spell resolution applies these in StackResolver, so this path covers
        // everything else. Skip face-down entries: morph creatures enter as 2/2 nameless
        // creatures with no replacement effects from their face-up identity.
        val actualDestZone = transitionResult.actualDestination
        if (actualDestZone == Zone.BATTLEFIELD && !effect.faceDown) {
            val (counterState, counterEvents) = EntersWithCountersHelper.applyEntersWithCounters(
                resultState, targetId, controllerId, cardRegistry
            )
            resultState = counterState
            extraEvents.addAll(counterEvents)
        }

        // Link exiled card to source permanent via LinkedExileComponent
        if (effect.linkToSource && effect.destination == Zone.EXILE) {
            val sourceId = context.sourceId
                ?: return EffectResult.success(resultState, transitionResult.events + extraEvents)
            val sourceContainer = resultState.getEntity(sourceId)
                ?: return EffectResult.success(resultState, transitionResult.events + extraEvents)
            val existingLinked = sourceContainer.get<LinkedExileComponent>()
            val allExiled = (existingLinked?.exiledIds ?: emptyList()) + listOf(targetId)
            resultState = resultState.updateEntity(sourceId) { c ->
                c.with(LinkedExileComponent(allExiled))
            }
        }

        // Auto-reveal: when a card moves from a publicly visible zone (graveyard/exile)
        // back into a hidden zone (hand/library), emit a CardsRevealedEvent so the UI
        // can show the opponent what card was returned and why. The card was already
        // public info, so this just surfaces the move in the reveal overlay.
        val revealEvents = autoRevealForReturn(
            fromZone = currentZone.zoneType,
            toZone = effect.destination,
            targetId = targetId,
            cardName = cardComponent.name,
            imageUri = cardComponent.imageUri,
            ownerId = ownerId,
            sourceId = context.sourceId,
            state = resultState
        )

        return EffectResult.success(resultState, transitionResult.events + extraEvents + revealEvents)
    }

    private fun autoRevealForReturn(
        fromZone: Zone,
        toZone: Zone,
        targetId: com.wingedsheep.sdk.model.EntityId,
        cardName: String,
        imageUri: String?,
        ownerId: com.wingedsheep.sdk.model.EntityId,
        sourceId: com.wingedsheep.sdk.model.EntityId?,
        state: GameState
    ): List<com.wingedsheep.engine.core.GameEvent> {
        val isFromPublic = fromZone == Zone.GRAVEYARD || fromZone == Zone.EXILE
        val isToReturnDestination =
            toZone == Zone.HAND || toZone == Zone.LIBRARY || toZone == Zone.BATTLEFIELD
        if (!isFromPublic || !isToReturnDestination) return emptyList()

        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        return listOf(
            CardsRevealedEvent(
                revealingPlayerId = ownerId,
                cardIds = listOf(targetId),
                cardNames = listOf(cardName),
                imageUris = listOf(imageUri),
                source = sourceName,
                // The owner moved the card themselves — they already know what/where it is.
                // Only the opponent needs the reveal overlay to surface the transition.
                revealToSelf = false,
                fromZone = fromZone,
                toZone = toZone
            )
        )
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
