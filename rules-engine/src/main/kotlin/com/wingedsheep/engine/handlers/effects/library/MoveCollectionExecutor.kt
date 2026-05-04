package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for MoveCollectionEffect.
 *
 * Reads cards from a named collection in [EffectContext.storedCollections]
 * and moves them to the specified destination zone.
 *
 * Cards are removed from their current zone (determined by looking at which zone
 * currently contains each card) and placed in the destination.
 */
class MoveCollectionExecutor(
    private val cardRegistry: CardRegistry,
    private val targetFinder: TargetFinder? = null
) : EffectExecutor<MoveCollectionEffect> {

    override val effectType: KClass<MoveCollectionEffect> = MoveCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: MoveCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val cards = context.pipeline.storedCollections[effect.from]
            ?: return EffectResult.error(state, "No collection named '${effect.from}' in storedCollections")

        val destination = effect.destination
        if (cards.isEmpty()) {
            // Nothing to move, but for library shuffles we still shuffle (e.g., ShuffleGraveyardIntoLibrary
            // shuffles even when the graveyard is empty, per the card's rules text).
            if (destination is CardDestination.ToZone &&
                destination.zone == Zone.LIBRARY &&
                destination.placement == ZonePlacement.Shuffled) {
                val destPlayerId = resolvePlayer(destination.player, context, state)
                if (destPlayerId != null) {
                    val destZoneKey = ZoneKey(destPlayerId, Zone.LIBRARY)
                    val library = state.getZone(destZoneKey)
                    val newState = state.copy(zones = state.zones + (destZoneKey to library.shuffled()))
                    return EffectResult.success(newState, listOf(LibraryShuffledEvent(destPlayerId)))
                }
            }
            return EffectResult.success(state)
        }

        return when (destination) {
            is CardDestination.ToZone -> {
                var result = moveToZone(state, context, cards, destination, effect.order, effect.revealed, effect.moveType, effect.faceDown, effect.noRegenerate, effect.storeMovedAs, effect.underOwnersControl, effect.revealToSelf)
                if (effect.linkToSource && result.isSuccess) {
                    result = linkCardsToSource(result, context, cards)
                }
                if (effect.unlinkFromSource && result.isSuccess) {
                    result = unlinkCardsFromSource(result, context, cards)
                }
                val counterType = effect.addCounterType
                if (counterType != null && result.isSuccess) {
                    var newState = result.state
                    for (cardId in cards) {
                        newState = newState.updateEntity(cardId) { c ->
                            val existing = c.get<CountersComponent>() ?: CountersComponent()
                            c.with(existing.withAdded(counterType, 1))
                        }
                    }
                    result = EffectResult.success(newState, result.events).copy(updatedCollections = result.updatedCollections)
                }
                result
            }
        }
    }

    /**
     * Store the moved card IDs on the source permanent's LinkedExileComponent.
     * Used by Parallel Thoughts and similar cards that need to track exiled cards.
     */
    private fun linkCardsToSource(
        result: EffectResult,
        context: EffectContext,
        cards: List<EntityId>
    ): EffectResult {
        val sourceId = context.sourceId ?: return result
        var newState = result.state
        val sourceContainer = newState.getEntity(sourceId) ?: return result
        val existingLinked = sourceContainer.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
        val allExiled = (existingLinked?.exiledIds ?: emptyList()) + cards
        newState = newState.updateEntity(sourceId) { c ->
            c.with(com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent(allExiled))
        }
        return EffectResult.success(newState, result.events)
    }

    /**
     * Remove the moved card IDs from the source permanent's LinkedExileComponent.
     * Inverse of [linkCardsToSource] — used when taking cards from a linked exile pile.
     */
    private fun unlinkCardsFromSource(
        result: EffectResult,
        context: EffectContext,
        cards: List<EntityId>
    ): EffectResult {
        val sourceId = context.sourceId ?: return result
        var newState = result.state
        val sourceContainer = newState.getEntity(sourceId) ?: return result
        val existingLinked = sourceContainer.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>() ?: return result
        val movedSet = cards.toSet()
        val remaining = existingLinked.exiledIds.filter { it !in movedSet }
        newState = newState.updateEntity(sourceId) { c ->
            c.with(com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent(remaining))
        }
        return EffectResult.success(newState, result.events)
    }

    private fun moveToZone(
        state: GameState,
        context: EffectContext,
        cards: List<EntityId>,
        destination: CardDestination.ToZone,
        order: CardOrder,
        revealed: Boolean = false,
        moveType: MoveType = MoveType.Default,
        faceDown: Boolean = false,
        noRegenerate: Boolean = false,
        storeMovedAs: String? = null,
        underOwnersControl: Boolean = false,
        revealToSelf: Boolean = true
    ): EffectResult {
        val destPlayerId = resolvePlayer(destination.player, context, state)
            ?: return EffectResult.error(state, "Could not resolve destination player for MoveCollection")

        val destZone = destination.zone

        // ControllerChooses ordering: pause for player to see/reorder cards going to library
        if (order == CardOrder.ControllerChooses && destZone == Zone.LIBRARY) {
            val isBottom = destination.placement == ZonePlacement.Bottom
            // For top placement: always pause (even for 1 card, so player can see it)
            // For bottom placement: only pause when there are multiple cards to order
            if (!isBottom || cards.size > 1) {
                return pauseForOrderDecision(state, context, cards, destZone, destPlayerId, destination.placement)
            }
        }

        return moveCardsToZone(state, context, cards, destination, destPlayerId, revealed, moveType, faceDown, noRegenerate, storeMovedAs, underOwnersControl, revealToSelf)
    }

    /**
     * Pause execution and present a ReorderLibraryDecision so the player
     * can choose the order of multiple cards going to the top of a library.
     */
    private fun pauseForOrderDecision(
        state: GameState,
        context: EffectContext,
        cards: List<EntityId>,
        destZone: Zone,
        destPlayerId: EntityId,
        placement: ZonePlacement = ZonePlacement.Top
    ): EffectResult {
        val playerId = context.controllerId

        // Build card info map for the UI
        val cardInfoMap = cards.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null
            )
        }

        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val isOwnLibrary = destPlayerId == context.controllerId
        val libraryOwner = if (isOwnLibrary) "your" else "their"
        val promptText = when (placement) {
            ZonePlacement.Bottom -> "Put the revealed cards on the bottom of $libraryOwner library in any order."
            else -> if (cards.size == 1) "Look at the top card of $libraryOwner library."
                else "Look at the top ${cards.size} cards of $libraryOwner library. Put them back in any order."
        }

        val decision = ReorderLibraryDecision(
            id = decisionId,
            playerId = playerId,
            prompt = promptText,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            cards = cards,
            cardInfo = cardInfoMap
        )

        val continuation = MoveCollectionOrderContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            cards = cards,
            destinationZone = destZone,
            destinationPlayerId = destPlayerId,
            placement = placement
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "REORDER_LIBRARY",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Actually move the cards to the destination zone. Used both for direct moves
     * and after the player has chosen an order via MoveCollectionOrderContinuation.
     */
    internal fun moveCardsToZone(
        state: GameState,
        context: EffectContext,
        cards: List<EntityId>,
        destination: CardDestination.ToZone,
        destPlayerId: EntityId,
        revealed: Boolean = false,
        moveType: MoveType = MoveType.Default,
        faceDown: Boolean = false,
        noRegenerate: Boolean = false,
        storeMovedAs: String? = null,
        underOwnersControl: Boolean = false,
        revealToSelf: Boolean = true
    ): EffectResult {
        val destZone = destination.zone

        // When moving to battlefield, detect auras that need target selection (Rule 303.4f)
        if (destZone == Zone.BATTLEFIELD && targetFinder != null) {
            val auraCards = mutableListOf<EntityId>()
            val nonAuraCards = mutableListOf<EntityId>()

            for (cardId in cards) {
                val container = state.getEntity(cardId)
                val cardComponent = container?.get<CardComponent>()
                if (cardComponent?.isAura == true) {
                    auraCards.add(cardId)
                } else {
                    nonAuraCards.add(cardId)
                }
            }

            if (auraCards.isNotEmpty()) {
                // Move non-auras first, then pause for aura target selection
                var newState = state
                val events = mutableListOf<GameEvent>()

                if (nonAuraCards.isNotEmpty()) {
                    val nonAuraResult = moveCardsToZoneInternal(
                        newState, context, nonAuraCards, destination, destPlayerId, revealed, moveType, faceDown, noRegenerate, storeMovedAs, underOwnersControl, revealToSelf
                    )
                    newState = nonAuraResult.state
                    events.addAll(nonAuraResult.events)
                }

                // Rule 303.4f: the player putting the Aura onto the battlefield chooses its enchant target.
                // When underOwnersControl=true the Aura returns to its owner, so the owner picks the target.
                val firstAuraId = auraCards.first()
                val actualControllerId = if (underOwnersControl) {
                    val e = newState.getEntity(firstAuraId)
                    e?.get<OwnerComponent>()?.playerId ?: e?.get<CardComponent>()?.ownerId ?: destPlayerId
                } else destPlayerId

                return askAuraTargetForMoveCollection(
                    state = newState,
                    events = events,
                    auraId = firstAuraId,
                    controllerId = actualControllerId,
                    destPlayerId = actualControllerId,
                    remainingAuras = auraCards.drop(1),
                    sourceId = context.sourceId,
                    sourceName = context.sourceId?.let { newState.getEntity(it)?.get<CardComponent>()?.name },
                    underOwnersControl = underOwnersControl
                )
            }
        }

        return moveCardsToZoneInternal(state, context, cards, destination, destPlayerId, revealed, moveType, faceDown, noRegenerate, storeMovedAs, underOwnersControl, revealToSelf)
    }

    /**
     * Ask the player to choose a target for an Aura entering the battlefield via MoveCollectionEffect.
     * Per Rule 303.4f, targeting restrictions (hexproof, shroud) do not apply.
     * If no legal target exists, the Aura stays in its current zone (Rule 303.4g).
     *
     * [underOwnersControl] — when true the aura returns under its owner's control; the owner
     * is the one choosing the enchant target and controlling the battlefield zone.
     */
    private fun askAuraTargetForMoveCollection(
        state: GameState,
        events: List<GameEvent>,
        auraId: EntityId,
        controllerId: EntityId,
        destPlayerId: EntityId,
        remainingAuras: List<EntityId>,
        sourceId: EntityId?,
        sourceName: String?,
        underOwnersControl: Boolean = false
    ): EffectResult {
        val cardComponent = state.getEntity(auraId)?.get<CardComponent>()
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.cardDefinitionId) }
        val auraTarget = cardDef?.script?.auraTarget

        if (auraTarget == null) {
            // No aura target defined — skip this aura (leave in current zone)
            return continueAuraProcessingOrFinish(
                state, events, remainingAuras, controllerId, destPlayerId, sourceId, sourceName
            )
        }

        val legalTargets = targetFinder!!.findLegalTargets(
            state = state,
            requirement = auraTarget,
            controllerId = controllerId,
            sourceId = auraId,
            ignoreTargetingRestrictions = true
        )

        if (legalTargets.isEmpty()) {
            // No legal targets — Aura stays in current zone per Rule 303.4g
            return continueAuraProcessingOrFinish(
                state, events, remainingAuras, controllerId, destPlayerId, sourceId, sourceName, underOwnersControl
            )
        }

        val decisionId = UUID.randomUUID().toString()
        val auraName = cardComponent.name
        val requirementInfo = TargetRequirementInfo(
            index = 0,
            description = auraTarget.description,
            minTargets = 1,
            maxTargets = 1
        )
        val decision = ChooseTargetsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose what $auraName enchants",
            context = DecisionContext(
                sourceId = auraId,
                sourceName = auraName,
                phase = DecisionPhase.RESOLUTION
            ),
            targetRequirements = listOf(requirementInfo),
            legalTargets = mapOf(0 to legalTargets)
        )

        val continuation = MoveCollectionAuraTargetContinuation(
            decisionId = decisionId,
            auraId = auraId,
            controllerId = controllerId,
            destPlayerId = destPlayerId,
            remainingAuras = remainingAuras,
            sourceId = sourceId,
            sourceName = sourceName,
            underOwnersControl = underOwnersControl
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult(
            state = stateWithContinuation,
            events = events,
            pendingDecision = decision
        )
    }

    /**
     * Continue processing remaining auras, or finish if no more.
     */
    private fun continueAuraProcessingOrFinish(
        state: GameState,
        events: List<GameEvent>,
        remainingAuras: List<EntityId>,
        controllerId: EntityId,
        destPlayerId: EntityId,
        sourceId: EntityId?,
        sourceName: String?,
        underOwnersControl: Boolean = false
    ): EffectResult {
        if (remainingAuras.isNotEmpty()) {
            val nextAuraId = remainingAuras.first()
            val actualControllerId = if (underOwnersControl) {
                val e = state.getEntity(nextAuraId)
                e?.get<OwnerComponent>()?.playerId ?: e?.get<CardComponent>()?.ownerId ?: controllerId
            } else controllerId
            return askAuraTargetForMoveCollection(
                state = state,
                events = events,
                auraId = nextAuraId,
                controllerId = actualControllerId,
                destPlayerId = actualControllerId,
                remainingAuras = remainingAuras.drop(1),
                sourceId = sourceId,
                sourceName = sourceName,
                underOwnersControl = underOwnersControl
            )
        }
        return EffectResult.success(state, events)
    }

    /**
     * Move a single aura from its current zone to the battlefield with AttachedToComponent.
     */
    internal fun moveAuraToBattlefield(
        state: GameState,
        auraId: EntityId,
        targetId: EntityId,
        destPlayerId: EntityId
    ): Pair<GameState, List<GameEvent>> {
        val events = mutableListOf<GameEvent>()
        var newState = state

        val ownerId = newState.getEntity(auraId)?.get<OwnerComponent>()?.playerId ?: destPlayerId
        val cardName = newState.getEntity(auraId)?.get<CardComponent>()?.name ?: "Unknown"

        // Find and remove from current zone
        val fromZone = findCurrentZone(newState, auraId, ownerId)
        if (fromZone != null) {
            newState = newState.removeFromZone(ZoneKey(ownerId, fromZone), auraId)
        }

        // Add to battlefield
        val battlefieldZone = ZoneKey(destPlayerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, auraId)

        // Apply battlefield components + AttachedToComponent on aura
        val container = newState.getEntity(auraId)
        if (container != null) {
            val cardDef = container.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
            var newContainer = container
                .with(ControllerComponent(destPlayerId))
                .with(AttachedToComponent(targetId))
            // Wire up static/replacement abilities from the card definition
            if (cardDef != null) {
                val staticHandler = com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler(cardRegistry)
                newContainer = staticHandler.addContinuousEffectComponent(newContainer, cardDef)
                newContainer = staticHandler.addReplacementEffectComponent(newContainer, cardDef)
            }
            newState = newState.copy(entities = newState.entities + (auraId to newContainer))
        }

        // Update target's AttachmentsComponent to include this aura
        newState = newState.updateEntity(targetId) { targetContainer ->
            val existing = targetContainer.get<AttachmentsComponent>()
            val updatedIds = (existing?.attachedIds ?: emptyList()) + auraId
            targetContainer.with(AttachmentsComponent(updatedIds))
        }

        if (fromZone != null) {
            events.add(
                ZoneChangeEvent(
                    entityId = auraId,
                    entityName = cardName,
                    fromZone = fromZone,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = ownerId
                )
            )
        }

        return Pair(newState, events)
    }

    private fun moveCardsToZoneInternal(
        state: GameState,
        context: EffectContext,
        cards: List<EntityId>,
        destination: CardDestination.ToZone,
        destPlayerId: EntityId,
        revealed: Boolean = false,
        moveType: MoveType = MoveType.Default,
        faceDown: Boolean = false,
        noRegenerate: Boolean = false,
        storeMovedAs: String? = null,
        underOwnersControl: Boolean = false,
        revealToSelf: Boolean = true
    ): EffectResult {
        val destZone = destination.zone
        val events = mutableListOf<GameEvent>()
        var newState = state

        val movedIds = mutableListOf<EntityId>()
        // Track every library that received at least one card so per-card owner routing
        // (e.g., a permanent owned by another player going to its owner's library) shuffles
        // and reveal-marks every affected library, not just the destination's nominal owner.
        val librariesReceivingCards = linkedSetOf<EntityId>()

        // Determine library placement for ZoneTransitionService
        val libraryPlacement = when (destination.placement) {
            ZonePlacement.Top, ZonePlacement.Default -> com.wingedsheep.engine.handlers.effects.LibraryPlacement.Top
            ZonePlacement.Bottom -> com.wingedsheep.engine.handlers.effects.LibraryPlacement.Bottom
            ZonePlacement.Shuffled -> com.wingedsheep.engine.handlers.effects.LibraryPlacement.Bottom // shuffle at end
            else -> com.wingedsheep.engine.handlers.effects.LibraryPlacement.Top
        }

        for (cardId in cards) {
            val ownerId = newState.getEntity(cardId)?.get<OwnerComponent>()?.playerId ?: destPlayerId

            // For MoveType.Destroy, check indestructible and regeneration before moving
            if (moveType == MoveType.Destroy) {
                if (newState.projectedState.hasKeyword(cardId, Keyword.INDESTRUCTIBLE)) {
                    continue
                }
                if (!noRegenerate) {
                    val (shieldState, wasRegenerated) = ZoneMovementUtils.applyRegenerationShields(newState, cardId)
                    if (wasRegenerated) {
                        newState = ZoneMovementUtils.applyRegenerationReplacement(shieldState, cardId).state
                        continue
                    }
                }
            }

            // Find current zone for controller override logic
            val fromZone = findCurrentZone(newState, cardId, ownerId)

            // Determine actual destination player based on moveType and zones.
            // A permanent leaving the battlefield always goes to its owner's hand/library/exile/
            // graveyard — never to a "destination player" chosen by the effect — so routing
            // collapses to ownerId for those cases regardless of the destination's nominal player.
            val actualDestPlayerId = when {
                (moveType == MoveType.Sacrifice || moveType == MoveType.Destroy) && destZone == Zone.GRAVEYARD -> ownerId
                destZone == Zone.HAND && fromZone == Zone.BATTLEFIELD -> ownerId
                destZone == Zone.EXILE && fromZone == Zone.BATTLEFIELD -> ownerId
                destZone == Zone.LIBRARY && fromZone == Zone.BATTLEFIELD -> ownerId
                underOwnersControl && destZone == Zone.BATTLEFIELD -> ownerId
                else -> destPlayerId
            }

            val entryOptions = com.wingedsheep.engine.handlers.effects.ZoneEntryOptions(
                controllerId = actualDestPlayerId,
                libraryPlacement = libraryPlacement,
                tapped = destination.placement == ZonePlacement.Tapped || destination.placement == ZonePlacement.TappedAndAttacking,
                tappedAndAttacking = destination.placement == ZonePlacement.TappedAndAttacking,
                faceDownExile = faceDown && destZone == Zone.EXILE
            )

            // Delegate to ZoneTransitionService for full cleanup + entry
            val fromZoneKey = if (fromZone != null) ZoneKey(ownerId, fromZone) else null
            val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                newState, cardId, destZone, entryOptions, fromZoneKey
            )
            newState = transitionResult.state
            events.addAll(transitionResult.events)

            movedIds.add(cardId)
            if (destZone == Zone.LIBRARY) {
                librariesReceivingCards.add(actualDestPlayerId)
            }
        }

        // Handle shuffled placement — shuffle every affected library once after all cards are placed.
        // Per-card owner routing means a single MoveCollection can deposit cards in multiple libraries
        // (e.g., target opponent shuffles permanents they control but don't own).
        if (destination.placement == ZonePlacement.Shuffled && destZone == Zone.LIBRARY) {
            val toShuffle = if (librariesReceivingCards.isNotEmpty()) librariesReceivingCards else linkedSetOf(destPlayerId)
            for (libraryOwnerId in toShuffle) {
                val destZoneKey = ZoneKey(libraryOwnerId, Zone.LIBRARY)
                // Strip reveals before shuffling — once shuffled, no one knows positions any more
                newState = LibraryRevealUtils.clearLibraryReveals(newState, libraryOwnerId)
                val library = newState.getZone(destZoneKey)
                newState = newState.copy(zones = newState.zones + (destZoneKey to library.shuffled()))
                events.add(LibraryShuffledEvent(libraryOwnerId))
            }
        }

        // Persist reveals when cards are moved into a library at a known position.
        // The mover knows where each card landed; if revealed=true, everyone knows.
        // (Shuffled placement is handled above and intentionally does NOT mark.)
        if (destZone == Zone.LIBRARY && destination.placement != ZonePlacement.Shuffled && movedIds.isNotEmpty()) {
            val audience: Set<EntityId> = if (revealed) {
                newState.turnOrder.toSet()
            } else {
                setOf(context.controllerId)
            }
            newState = LibraryRevealUtils.markRevealed(newState, movedIds, audience)
        }

        // Emit discard event if configured
        if (moveType == MoveType.Discard && cards.isNotEmpty()) {
            val discardNames = cards.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            events.add(CardsDiscardedEvent(destPlayerId, cards, discardNames))
        }

        // Emit sacrifice event if configured
        if (moveType == MoveType.Sacrifice && cards.isNotEmpty()) {
            val sacrificeNames = cards.map { cardId ->
                state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            }
            events.add(0, PermanentsSacrificedEvent(context.controllerId, cards, sacrificeNames))
        }

        // Emit reveal event if configured
        if (revealed && cards.isNotEmpty()) {
            val cardNames = cards.map { cardId ->
                newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            }
            val imageUris = cards.map { cardId ->
                newState.getEntity(cardId)?.get<CardComponent>()?.imageUri
            }
            val sourceName = context.sourceId?.let { sourceId ->
                newState.getEntity(sourceId)?.get<CardComponent>()?.name
            }
            events.add(
                CardsRevealedEvent(
                    revealingPlayerId = context.controllerId,
                    cardIds = cards,
                    cardNames = cardNames,
                    imageUris = imageUris,
                    source = sourceName,
                    revealToSelf = revealToSelf
                )
            )
        }

        val updatedCollections = if (storeMovedAs != null) {
            mapOf(storeMovedAs to movedIds.toList())
        } else {
            emptyMap()
        }

        return EffectResult.success(newState, events).copy(updatedCollections = updatedCollections)
    }

    /**
     * Find which zone a card currently lives in.
     */
    private fun findCurrentZone(state: GameState, cardId: EntityId, ownerId: EntityId): Zone? {
        for (zone in Zone.entries) {
            val zoneKey = ZoneKey(ownerId, zone)
            if (cardId in state.getZone(zoneKey)) {
                return zone
            }
        }
        return null
    }

    private fun resolvePlayer(player: Player, context: EffectContext, state: GameState): EntityId? {
        return when (player) {
            is Player.You -> context.controllerId
            is Player.Opponent -> context.opponentId
            is Player.TargetOpponent -> context.opponentId
            is Player.TargetPlayer -> context.targets.firstOrNull()?.let { TargetResolutionUtils.run { it.toEntityId() } }
            is Player.ContextPlayer -> context.targets.getOrNull(player.index)?.let { TargetResolutionUtils.run { it.toEntityId() } }
            is Player.TriggeringPlayer -> context.triggeringEntityId
            else -> context.controllerId
        }
    }
}
