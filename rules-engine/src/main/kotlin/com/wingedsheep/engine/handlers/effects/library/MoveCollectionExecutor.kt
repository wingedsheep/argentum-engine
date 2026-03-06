package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
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
    private val cardRegistry: CardRegistry? = null,
    private val targetFinder: TargetFinder? = null
) : EffectExecutor<MoveCollectionEffect> {

    override val effectType: KClass<MoveCollectionEffect> = MoveCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: MoveCollectionEffect,
        context: EffectContext
    ): ExecutionResult {
        val cards = context.storedCollections[effect.from]
            ?: return ExecutionResult.error(state, "No collection named '${effect.from}' in storedCollections")

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
                    return ExecutionResult.success(newState, listOf(LibraryShuffledEvent(destPlayerId)))
                }
            }
            return ExecutionResult.success(state)
        }

        return when (destination) {
            is CardDestination.ToZone -> {
                val result = moveToZone(state, context, cards, destination, effect.order, effect.revealed, effect.moveType, effect.faceDown, effect.noRegenerate, effect.storeMovedAs, effect.underOwnersControl)
                if (effect.linkToSource && result.isSuccess) {
                    linkCardsToSource(result, context, cards)
                } else {
                    result
                }
            }
        }
    }

    /**
     * Store the moved card IDs on the source permanent's LinkedExileComponent.
     * Used by Parallel Thoughts and similar cards that need to track exiled cards.
     */
    private fun linkCardsToSource(
        result: ExecutionResult,
        context: EffectContext,
        cards: List<EntityId>
    ): ExecutionResult {
        val sourceId = context.sourceId ?: return result
        var newState = result.state
        val sourceContainer = newState.getEntity(sourceId) ?: return result
        val existingLinked = sourceContainer.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
        val allExiled = (existingLinked?.exiledIds ?: emptyList()) + cards
        newState = newState.updateEntity(sourceId) { c ->
            c.with(com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent(allExiled))
        }
        return ExecutionResult.success(newState, result.events)
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
        underOwnersControl: Boolean = false
    ): ExecutionResult {
        val destPlayerId = resolvePlayer(destination.player, context, state)
            ?: return ExecutionResult.error(state, "Could not resolve destination player for MoveCollection")

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

        return moveCardsToZone(state, context, cards, destination, destPlayerId, revealed, moveType, faceDown, noRegenerate, storeMovedAs, underOwnersControl)
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
    ): ExecutionResult {
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

        return ExecutionResult.paused(
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
        underOwnersControl: Boolean = false
    ): ExecutionResult {
        val destZone = destination.zone

        // When moving to battlefield, detect auras that need target selection (Rule 303.4f)
        if (destZone == Zone.BATTLEFIELD && cardRegistry != null && targetFinder != null) {
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
                        newState, context, nonAuraCards, destination, destPlayerId, revealed, moveType, faceDown, noRegenerate, storeMovedAs, underOwnersControl
                    )
                    newState = nonAuraResult.state
                    events.addAll(nonAuraResult.events)
                }

                return askAuraTargetForMoveCollection(
                    state = newState,
                    events = events,
                    auraId = auraCards.first(),
                    controllerId = destPlayerId,
                    destPlayerId = destPlayerId,
                    remainingAuras = auraCards.drop(1),
                    sourceId = context.sourceId,
                    sourceName = context.sourceId?.let { newState.getEntity(it)?.get<CardComponent>()?.name }
                )
            }
        }

        return moveCardsToZoneInternal(state, context, cards, destination, destPlayerId, revealed, moveType, faceDown, noRegenerate, storeMovedAs, underOwnersControl)
    }

    /**
     * Ask the player to choose a target for an Aura entering the battlefield via MoveCollectionEffect.
     * Per Rule 303.4f, targeting restrictions (hexproof, shroud) do not apply.
     * If no legal target exists, the Aura stays in its current zone (Rule 303.4g).
     */
    private fun askAuraTargetForMoveCollection(
        state: GameState,
        events: List<GameEvent>,
        auraId: EntityId,
        controllerId: EntityId,
        destPlayerId: EntityId,
        remainingAuras: List<EntityId>,
        sourceId: EntityId?,
        sourceName: String?
    ): ExecutionResult {
        val cardComponent = state.getEntity(auraId)?.get<CardComponent>()
        val cardDef = cardComponent?.let { cardRegistry?.getCard(it.cardDefinitionId) }
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
                state, events, remainingAuras, controllerId, destPlayerId, sourceId, sourceName
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
            sourceName = sourceName
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult(
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
        sourceName: String?
    ): ExecutionResult {
        if (remainingAuras.isNotEmpty()) {
            return askAuraTargetForMoveCollection(
                state = state,
                events = events,
                auraId = remainingAuras.first(),
                controllerId = controllerId,
                destPlayerId = destPlayerId,
                remainingAuras = remainingAuras.drop(1),
                sourceId = sourceId,
                sourceName = sourceName
            )
        }
        return ExecutionResult.success(state, events)
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

        // Apply battlefield components + AttachedToComponent
        val container = newState.getEntity(auraId)
        if (container != null) {
            val newContainer = container
                .with(ControllerComponent(destPlayerId))
                .with(AttachedToComponent(targetId))

            newState = newState.copy(entities = newState.entities + (auraId to newContainer))
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
        underOwnersControl: Boolean = false
    ): ExecutionResult {
        val destZone = destination.zone
        val events = mutableListOf<GameEvent>()
        var newState = state

        val destroyedIds = mutableListOf<EntityId>()

        // Determine where each card currently lives (for removal)
        for (cardId in cards) {
            val ownerId = newState.getEntity(cardId)?.get<OwnerComponent>()?.playerId ?: destPlayerId
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"

            // For MoveType.Destroy, check indestructible and regeneration before moving
            if (moveType == MoveType.Destroy) {
                // Indestructible permanents can't be destroyed — skip entirely
                if (newState.projectedState.hasKeyword(cardId, Keyword.INDESTRUCTIBLE)) {
                    continue
                }

                // Check for regeneration shields (unless noRegenerate is set)
                if (!noRegenerate) {
                    val (shieldState, wasRegenerated) = EffectExecutorUtils.applyRegenerationShields(newState, cardId)
                    if (wasRegenerated) {
                        newState = EffectExecutorUtils.applyRegenerationReplacement(shieldState, cardId).state
                        continue
                    }
                }
            }

            // Find and remove from current zone
            val fromZone = findCurrentZone(newState, cardId, ownerId)
            if (fromZone != null) {
                newState = newState.removeFromZone(ZoneKey(ownerId, fromZone), cardId)

                // Strip battlefield-specific components when leaving the battlefield
                // (face-down status, tapped, damage, counters, combat state, etc.)
                if (fromZone == Zone.BATTLEFIELD) {
                    newState = EffectExecutorUtils.cleanupCombatReferences(newState, cardId)
                    newState = newState.updateEntity(cardId) { c ->
                        EffectExecutorUtils.stripBattlefieldComponents(c)
                    }
                    // Remove floating effects targeting this entity (Rule 400.7)
                    if (moveType == MoveType.Destroy) {
                        newState = EffectExecutorUtils.removeFloatingEffectsTargeting(newState, cardId)
                    }
                }

                // Strip face-down status when leaving exile (card becomes visible in new zone)
                if (fromZone == Zone.EXILE) {
                    val container = newState.getEntity(cardId)
                    if (container != null && container.has<FaceDownComponent>()) {
                        newState = newState.updateEntity(cardId) { c -> c.without<FaceDownComponent>() }
                    }
                }
            }

            if (moveType == MoveType.Destroy) {
                destroyedIds.add(cardId)
            }

            // For sacrifice/destroy, cards always go to their owner's graveyard,
            // regardless of who controlled them. For return-to-hand, cards go to their
            // owner's hand (Rule 400.3). For underOwnersControl, cards enter the
            // battlefield under their owner's control. For all other moves, use the specified player.
            val actualDestPlayerId = when {
                (moveType == MoveType.Sacrifice || moveType == MoveType.Destroy) && destZone == Zone.GRAVEYARD -> ownerId
                destZone == Zone.HAND && fromZone == Zone.BATTLEFIELD -> ownerId
                destZone == Zone.EXILE && fromZone == Zone.BATTLEFIELD -> ownerId
                underOwnersControl && destZone == Zone.BATTLEFIELD -> ownerId
                else -> destPlayerId
            }

            // Add to destination zone based on placement
            val destZoneKey = ZoneKey(actualDestPlayerId, destZone)
            newState = when (destination.placement) {
                ZonePlacement.Top, ZonePlacement.Default -> {
                    if (destZone == Zone.LIBRARY) {
                        // Prepend to library (top)
                        val currentLibrary = newState.getZone(destZoneKey)
                        newState.copy(zones = newState.zones + (destZoneKey to listOf(cardId) + currentLibrary))
                    } else {
                        newState.addToZone(destZoneKey, cardId)
                    }
                }
                ZonePlacement.Bottom -> {
                    if (destZone == Zone.LIBRARY) {
                        // Append to library (bottom)
                        val currentLibrary = newState.getZone(destZoneKey)
                        newState.copy(zones = newState.zones + (destZoneKey to currentLibrary + cardId))
                    } else {
                        newState.addToZone(destZoneKey, cardId)
                    }
                }
                ZonePlacement.Shuffled -> {
                    newState.addToZone(destZoneKey, cardId)
                    // Shuffle will happen after all cards are added
                }
                ZonePlacement.Tapped -> {
                    newState.addToZone(destZoneKey, cardId)
                }
            }

            // Apply battlefield-specific components
            if (destZone == Zone.BATTLEFIELD) {
                val container = newState.getEntity(cardId)
                if (container != null) {
                    val controllerId = if (underOwnersControl) ownerId else destPlayerId
                    var newContainer = container.with(ControllerComponent(controllerId))

                    // Creatures enter with summoning sickness
                    val cardComp = container.get<CardComponent>()
                    if (cardComp?.typeLine?.isCreature == true) {
                        newContainer = newContainer.with(SummoningSicknessComponent)
                    }

                    // Apply tapped status if entering tapped
                    if (destination.placement == ZonePlacement.Tapped) {
                        newContainer = newContainer.with(TappedComponent)
                    }

                    newState = newState.copy(entities = newState.entities + (cardId to newContainer))
                }
            }

            // Apply face-down status when exiling face-down
            if (faceDown && destZone == Zone.EXILE) {
                newState = newState.updateEntity(cardId) { c -> c.with(FaceDownComponent) }
            }

            if (fromZone != null) {
                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardName,
                        fromZone = fromZone,
                        toZone = destZone,
                        ownerId = ownerId
                    )
                )
            }
        }

        // Handle shuffled placement
        if (destination.placement == ZonePlacement.Shuffled && destZone == Zone.LIBRARY) {
            val destZoneKey = ZoneKey(destPlayerId, Zone.LIBRARY)
            val library = newState.getZone(destZoneKey)
            newState = newState.copy(zones = newState.zones + (destZoneKey to library.shuffled()))
            events.add(LibraryShuffledEvent(destPlayerId))
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
                    source = sourceName
                )
            )
        }

        val updatedCollections = if (storeMovedAs != null && destroyedIds.isNotEmpty()) {
            mapOf(storeMovedAs to destroyedIds.toList())
        } else if (storeMovedAs != null) {
            mapOf(storeMovedAs to emptyList())
        } else {
            emptyMap()
        }

        return ExecutionResult.success(newState, events).copy(updatedCollections = updatedCollections)
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
            is Player.TargetPlayer -> context.opponentId
            is Player.ContextPlayer -> context.targets.getOrNull(player.index)?.let { EffectExecutorUtils.run { it.toEntityId() } }
            is Player.TriggeringPlayer -> context.triggeringEntityId
            else -> context.controllerId
        }
    }
}
