package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.CommanderZoneChoiceAskedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.CardsLeftGraveyardThisTurnComponent
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.NonTokenCreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.OpponentCreaturesExiledThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerDescendedThisTurnComponent
import com.wingedsheep.engine.state.components.player.SacrificedFoodThisTurnComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId


/**
 * Options controlling how an entity enters a destination zone.
 */
data class ZoneEntryOptions(
    val controllerId: EntityId? = null,
    val libraryPlacement: LibraryPlacement = LibraryPlacement.Top,
    val tapped: Boolean = false,
    val tappedAndAttacking: Boolean = false,
    val faceDown: Boolean = false,
    val morphData: MorphDataComponent? = null,
    val skipZoneChangeRedirect: Boolean = false,
    val faceDownExile: Boolean = false,
    val lastKnownAttachedTo: EntityId? = null
)

/**
 * How to place a card in the library zone.
 */
sealed interface LibraryPlacement {
    data object Top : LibraryPlacement
    data object Bottom : LibraryPlacement
    data object Shuffled : LibraryPlacement
    data class NthFromTop(val position: Int) : LibraryPlacement
}

/**
 * Result of a zone transition.
 */
data class ZoneTransitionResult(
    val state: GameState,
    val events: List<EngineGameEvent>,
    val redirectResult: ZoneChangeRedirectResult? = null,
    val actualDestination: Zone? = null
)

/**
 * Single canonical zone transition pipeline.
 *
 * ALL zone movement in the engine should go through this service.
 * This ensures that every zone change applies the full cleanup/setup pipeline
 * consistently, preventing bugs from missing steps.
 *
 * Pipeline:
 * 1. Look up entity, CardComponent, owner, current zone
 * 2. Capture last-known info if leaving battlefield
 * 3. Check zone change redirect (unless skipZoneChangeRedirect)
 * 4. EXIT CLEANUP if leaving battlefield:
 *    a. cleanupReverseAttachmentLink
 *    b. cleanupCombatReferences
 *    c. stripBattlefieldComponents
 *    d. removeFloatingEffectsTargeting
 * 5. Strip face-down if leaving exile
 * 6. Remove from current zone
 * 7. ENTRY SETUP based on destination
 * 8. Emit ZoneChangeEvent
 * 9. Apply redirect additional effects if any
 */
object ZoneTransitionService {

    /**
     * Handler used to register static abilities and replacement effects on permanents that
     * enter the battlefield through [moveToZone] (reanimation, returns from exile, leyline
     * starts, etc.). Wired by [com.wingedsheep.engine.core.EngineServices] at construction
     * time. The cast pipeline ([com.wingedsheep.engine.mechanics.stack.StackResolver]) places
     * permanents via [GameState.addToZone] directly, not [moveToZone], so it owns its own
     * call to the handler and is unaffected by this wiring.
     */
    lateinit var staticAbilityHandler: StaticAbilityHandler

    /**
     * Move one entity between zones with full cleanup + setup.
     *
     * @param state The current game state
     * @param entityId The entity to move
     * @param destinationZone The target zone
     * @param options Entry options (tapped, controller override, library placement, etc.)
     * @param fromZoneKey Override the source zone key (if caller already knows it).
     *        If null, the service will find it automatically.
     * @return ZoneTransitionResult with updated state, events, and redirect info
     */
    fun moveToZone(
        state: GameState,
        entityId: EntityId,
        destinationZone: Zone,
        options: ZoneEntryOptions = ZoneEntryOptions(),
        fromZoneKey: ZoneKey? = null
    ): ZoneTransitionResult {
        // 1. Look up entity info
        val container = state.getEntity(entityId)
            ?: return ZoneTransitionResult(state, emptyList())

        val cardComponent = container.get<CardComponent>()
            ?: return ZoneTransitionResult(state, emptyList())

        val ownerId = cardComponent.ownerId
            ?: return ZoneTransitionResult(state, emptyList())

        val currentZoneKey = fromZoneKey ?: findEntityZone(state, entityId)
            ?: return ZoneTransitionResult(state, emptyList())

        val fromZone = currentZoneKey.zoneType
        val leavingBattlefield = fromZone == Zone.BATTLEFIELD

        // 2. Capture last-known info if leaving battlefield
        var lastKnownCounterCount = 0
        var lastKnownMinusOneMinusOneCounterCount = 0
        var lastKnownTotalCounterCount = 0
        var lastKnownCounters: Map<String, Int> = emptyMap()
        var lastKnownPower: Int? = null
        var lastKnownToughness: Int? = null
        var lastKnownTypeLine: TypeLine? = null
        var lastKnownKeywords: Set<String> = emptySet()
        var lastKnownLostAllAbilities = false
        var lastKnownAttachedTo = options.lastKnownAttachedTo
        var lastKnownWasToken = false
        var lastKnownDamageDealtByPlayers: Map<EntityId, Int> = emptyMap()

        if (leavingBattlefield) {
            val countersComponent = container.get<CountersComponent>()
            lastKnownCounterCount = countersComponent?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            lastKnownMinusOneMinusOneCounterCount =
                countersComponent?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0
            lastKnownTotalCounterCount = countersComponent?.counters?.values?.sum() ?: 0
            lastKnownCounters = countersComponent?.counters
                ?.filterValues { it > 0 }
                ?.mapKeys { (type, _) ->
                    com.wingedsheep.engine.handlers.effects.permanent.counters
                        .counterTypeToString(type)
                }
                ?: emptyMap()
            val projected = state.projectedState
            lastKnownPower = projected.getPower(entityId)
            lastKnownToughness = projected.getToughness(entityId)
            // Capture the projected typeLine so leaves-battlefield triggers see types/subtypes
            // granted by continuous effects (e.g., Ygra makes other creatures Food artifacts).
            lastKnownTypeLine = buildProjectedTypeLine(cardComponent, state, entityId)
            // Capture projected keywords so dies/leaves triggers can check keyword filters
            // (e.g., Jackdaw Savior: "whenever a creature you control with flying dies").
            lastKnownKeywords = projected.getKeywords(entityId)
            // Capture whether the entity had its abilities stripped at leaving time so the
            // dies / leaves-battlefield detectors can suppress the entity's own triggers
            // (e.g., Xu-Ifit reanimating Festering Goblin without its "When this dies" trigger).
            lastKnownLostAllAbilities = projected.hasLostAllAbilities(entityId)
            if (lastKnownAttachedTo == null) {
                lastKnownAttachedTo = container.get<AttachedToComponent>()?.targetId
            }
            lastKnownWasToken = container.has<TokenComponent>()
            lastKnownDamageDealtByPlayers =
                container.get<DamageDealtByPlayersThisTurnComponent>()?.perPlayer ?: emptyMap()
        }

        // 3. Check zone change redirect (unless skipped)
        val redirectResult = if (!options.skipZoneChangeRedirect) {
            ZoneMovementUtils.checkZoneChangeRedirect(state, entityId, fromZone, destinationZone)
        } else {
            ZoneChangeRedirectResult(destinationZone)
        }
        val actualDestZone = redirectResult.destinationZone

        // Determine controller and destination zone key
        val controllerId = if (leavingBattlefield) {
            container.get<ControllerComponent>()?.playerId ?: ownerId
        } else {
            ownerId
        }

        val destControllerId = options.controllerId ?: ownerId
        val destZoneKey = if (actualDestZone == Zone.BATTLEFIELD) {
            ZoneKey(destControllerId, actualDestZone)
        } else {
            ZoneKey(ownerId, actualDestZone)
        }

        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        // 4. EXIT CLEANUP if leaving battlefield
        if (leavingBattlefield) {
            newState = cleanupReverseAttachmentLink(newState, entityId)
            newState = cleanupCombatReferences(newState, entityId)
        }

        // 5. Strip face-down if leaving exile
        if (fromZone == Zone.EXILE) {
            val entityContainer = newState.getEntity(entityId)
            if (entityContainer != null && entityContainer.has<FaceDownComponent>()) {
                newState = newState.updateEntity(entityId) { c -> c.without<FaceDownComponent>() }
            }
            // A suspended card that leaves exile by any non-cast path (returned to hand,
            // shuffled in, exiled elsewhere) is no longer suspended (CR 702.62). The cast
            // path is guarded separately by the countdown's intervening-if, so a leftover
            // marker is inert there.
            if (entityContainer != null &&
                entityContainer.has<com.wingedsheep.engine.state.components.battlefield.SuspendedComponent>()
            ) {
                newState = newState.updateEntity(entityId) { c ->
                    c.without<com.wingedsheep.engine.state.components.battlefield.SuspendedComponent>()
                }
            }
        }

        // 6. Remove from current zone
        // Use the provided fromZoneKey directly — it already identifies the correct zone.
        // Don't derive from ControllerComponent, as the card may be on a different
        // player's battlefield zone (e.g., control-changed permanents in some zone layouts).
        val removeZoneKey = currentZoneKey
        newState = newState.removeFromZone(removeZoneKey, entityId)

        // Drop any remaining linked-exile reference held by a granter still on the
        // battlefield (e.g. Maralen, Fae Ascendant). The card has just left exile by
        // some non-cast path — return, blink, exile-elsewhere — so the granter must
        // forget it. Cast paths through StackResolver.removeFromCurrentZone unlink
        // separately because they bypass this service.
        if (fromZone == Zone.EXILE) {
            newState = ZoneMovementUtils.unlinkFromAllLinkedExiles(newState, entityId)
        }

        // Strip battlefield components and remove floating effects AFTER removal
        if (leavingBattlefield) {
            // Capture LinkedExileComponent BEFORE stripping so LTB triggers (e.g. Seam Rip's
            // "return linked exile" on LeavesBattlefield) can still read it.
            // Rule 400.7 only applies once the card re-enters the battlefield as a new object;
            // until then, graveyard/exile instances need the component for last-known-info triggers.
            val preStripLinkedExile = newState.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()

            // Revert permanent-level copy effects (Clone / Mockingbird / "becomes a copy of").
            // Per CR 400.7, a card that changes zones becomes a new object — its copy effect
            // ends and the card returns to its printed characteristics.
            val copyOf = newState.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.identity.CopyOfComponent>()
            val originalCardComponent = copyOf?.originalCardComponent
            if (originalCardComponent != null) {
                newState = newState.updateEntity(entityId) { c -> c.with(originalCardComponent) }
            }

            newState = newState.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }
            newState = removeFloatingEffectsTargeting(newState, entityId)

            // Re-attach LinkedExileComponent on any non-battlefield destination so LTB triggers
            // that reference it (like Seam Rip's return effect or Champion of the Clachan's
            // bounce-back) still have access to it after the source has left. Rule 400.7 is
            // honoured because applyBattlefieldEntry strips the component when the card
            // re-enters the battlefield as a new object.
            if (preStripLinkedExile != null && actualDestZone != Zone.BATTLEFIELD) {
                newState = newState.updateEntity(entityId) { c ->
                    c.with(preStripLinkedExile)
                }
            }
        }

        // 7. ENTRY SETUP based on destination
        when (actualDestZone) {
            Zone.BATTLEFIELD -> {
                // Rule 400.7: a card that changes zones becomes a new object with no memory
                // of its previous existence. Floating effects targeting it are stripped on the
                // way out (see step 4); granted triggered/activated abilities have to be
                // dropped here, on re-entry, because the leaves-battlefield trigger detection
                // for the previous incarnation needs to read them via state during the exit
                // event. By the time we reach this point those triggers are already queued on
                // the stack with their own captured ability data, so it is safe to wipe.
                newState = newState.copy(
                    grantedTriggeredAbilities = newState.grantedTriggeredAbilities
                        .filter { it.entityId != entityId },
                    grantedActivatedAbilities = newState.grantedActivatedAbilities
                        .filter { it.entityId != entityId }
                )
                newState = newState.addToZone(destZoneKey, entityId)
                newState = applyBattlefieldEntry(
                    newState, entityId, cardComponent, destControllerId, options, fromZone
                )
                // Record entry for per-player ETB-by-type tracking (Mechan Shieldmate and similar).
                // This pipeline records via PermanentEntryTracker.record directly rather than
                // BattlefieldEntry.place because the read must happen *after* applyBattlefieldEntry
                // wires the controller — only then does projection see the right controller.
                newState = PermanentEntryTracker.record(newState, destControllerId, entityId)
                // Handle Saga entering the battlefield (Rule 714.3a)
                val (sagaState, sagaEvents) = applySagaEntryIfNeeded(newState, entityId)
                newState = sagaState
                events.addAll(sagaEvents)
            }
            Zone.LIBRARY -> {
                if (options.libraryPlacement is LibraryPlacement.Shuffled) {
                    // Drop reveals on every other library card before mixing the new one in
                    newState = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
                        .clearLibraryReveals(newState, ownerId)
                }
                newState = placeInLibrary(newState, entityId, destZoneKey, options.libraryPlacement)
                if (options.libraryPlacement is LibraryPlacement.Shuffled) {
                    events.add(com.wingedsheep.engine.core.LibraryShuffledEvent(ownerId))
                }
            }
            Zone.EXILE -> {
                newState = newState.addToZone(destZoneKey, entityId)
                if (options.faceDownExile) {
                    newState = newState.updateEntity(entityId) { c -> c.with(FaceDownComponent) }
                }
            }
            else -> {
                // HAND, GRAVEYARD, STACK — simple addToZone
                newState = newState.addToZone(destZoneKey, entityId)
            }
        }

        // 7b. Rule 712.8a: while a DFC is in a zone other than the battlefield or stack, it has
        // only the characteristics of its front face. Restore the saved front-face CardComponent.
        if (actualDestZone != Zone.BATTLEFIELD && actualDestZone != Zone.STACK) {
            val entityContainer = newState.getEntity(entityId)
            if (entityContainer != null) {
                val dfc = entityContainer.get<DoubleFacedComponent>()
                if (dfc != null && dfc.isBack && dfc.frontFaceCard != null) {
                    newState = newState.updateEntity(entityId) { c ->
                        c.with(dfc.frontFaceCard)
                            .with(dfc.copy(currentFace = DoubleFacedComponent.Face.FRONT, frontFaceCard = null))
                    }
                }
            }
        }

        // 7c. Clear the CR 903.9a "already asked this stay" marker on every commander zone
        // change. The marker is attached by CommanderZoneChoiceCheck when the owner declines
        // the prompt; clearing it here means the next entry into a non-command zone produces a
        // fresh question, matching the rule's "since the last time state-based actions were
        // checked" wording (a new zone entry resets the clock). Cheap unconditional strip —
        // without<> is a no-op when the component is absent.
        if (newState.getEntity(entityId)?.has<CommanderComponent>() == true) {
            newState = newState.updateEntity(entityId) { c ->
                c.without<CommanderZoneChoiceAskedComponent>()
            }
        }

        // 8. Emit ZoneChangeEvent
        events.add(
            ZoneChangeEvent(
                entityId = entityId,
                entityName = cardComponent.name,
                fromZone = fromZone,
                toZone = actualDestZone,
                ownerId = ownerId,
                lastKnownController = if (leavingBattlefield) controllerId else null,
                lastKnownCounterCount = lastKnownCounterCount,
                lastKnownMinusOneMinusOneCounterCount = lastKnownMinusOneMinusOneCounterCount,
                lastKnownTotalCounterCount = lastKnownTotalCounterCount,
                lastKnownCounters = lastKnownCounters,
                lastKnownWasToken = lastKnownWasToken,
                lastKnownPower = lastKnownPower,
                lastKnownToughness = lastKnownToughness,
                lastKnownTypeLine = lastKnownTypeLine,
                lastKnownKeywords = lastKnownKeywords,
                lastKnownLostAllAbilities = lastKnownLostAllAbilities,
                lastKnownAttachedTo = if (leavingBattlefield) lastKnownAttachedTo else null,
                lastKnownCardDefinitionId = if (leavingBattlefield) cardComponent.cardDefinitionId else null,
                lastKnownDamageDealtByPlayers = lastKnownDamageDealtByPlayers
            )
        )

        // 8a2. Void: track that a nonland permanent left the battlefield this turn.
        // Uses the last-known projected type line so that creature-lands (which carry the
        // land type) correctly do NOT enable void.
        if (leavingBattlefield && lastKnownTypeLine?.isLand == false) {
            newState = newState.copy(nonlandPermanentLeftBattlefieldThisTurn = true)
        }

        // 8b. Track creature deaths inline so subsequent effects can see counts
        if (leavingBattlefield && actualDestZone == Zone.GRAVEYARD && cardComponent.typeLine.isCreature) {
            val isToken = container.has<TokenComponent>()
            // Track all creature deaths (including tokens)
            newState = newState.updateEntity(controllerId) { playerContainer ->
                val existing = playerContainer.get<CreaturesDiedThisTurnComponent>()
                    ?: CreaturesDiedThisTurnComponent()
                playerContainer.with(CreaturesDiedThisTurnComponent(existing.count + 1))
            }
            // Track non-token creature deaths separately
            if (!isToken) {
                newState = newState.updateEntity(controllerId) { playerContainer ->
                    val existing = playerContainer.get<NonTokenCreaturesDiedThisTurnComponent>()
                        ?: NonTokenCreaturesDiedThisTurnComponent()
                    playerContainer.with(NonTokenCreaturesDiedThisTurnComponent(existing.count + 1))
                }
            }
        }

        // 8b2. Track creatures exiled from battlefield for opponent's tracking
        // Used by Vren, the Relentless: "creatures exiled under your opponents' control this turn"
        if (leavingBattlefield && actualDestZone == Zone.EXILE && cardComponent.typeLine.isCreature) {
            // For each opponent of the creature's controller, increment their exile count
            val allPlayers = newState.turnOrder
            for (opponentId in allPlayers) {
                if (opponentId != controllerId) {
                    newState = newState.updateEntity(opponentId) { playerContainer ->
                        val existing = playerContainer.get<OpponentCreaturesExiledThisTurnComponent>()
                            ?: OpponentCreaturesExiledThisTurnComponent()
                        playerContainer.with(OpponentCreaturesExiledThisTurnComponent(existing.count + 1))
                    }
                }
            }
        }

        // 8c. Track cards leaving the graveyard
        if (fromZone == Zone.GRAVEYARD) {
            newState = newState.updateEntity(ownerId) { playerContainer ->
                val existing = playerContainer.get<CardsLeftGraveyardThisTurnComponent>()
                    ?: CardsLeftGraveyardThisTurnComponent()
                playerContainer.with(CardsLeftGraveyardThisTurnComponent(existing.count + 1))
            }
        }

        // 8d. Descend (CR 700.11): track permanent cards put into a player's graveyard
        // from any zone. Tokens are excluded per Scryfall ruling — although tokens are
        // briefly placed in the graveyard before ceasing to exist, that placement does
        // not count as the owner having descended. Non-permanent cards (instants /
        // sorceries entering the graveyard from the stack, hand, or library) are also
        // excluded. The count is keyed on the card's owner, not its last controller —
        // "your graveyard" is the owner's graveyard.
        if (actualDestZone == Zone.GRAVEYARD &&
            cardComponent.typeLine.isPermanent &&
            !container.has<TokenComponent>()
        ) {
            newState = newState.updateEntity(ownerId) { playerContainer ->
                val existing = playerContainer.get<PlayerDescendedThisTurnComponent>()
                    ?: PlayerDescendedThisTurnComponent()
                playerContainer.with(PlayerDescendedThisTurnComponent(existing.count + 1))
            }
        }

        // 9. Apply redirect additional effects if any
        if (redirectResult.additionalEffect != null) {
            newState = ZoneMovementUtils.applyReplacementAdditionalEffect(
                newState, redirectResult.additionalEffect, redirectResult.effectControllerId, entityId
            )
        }

        return ZoneTransitionResult(
            state = newState,
            events = events,
            redirectResult = redirectResult,
            actualDestination = actualDestZone
        )
    }

    /**
     * Move multiple entities to a zone. For library destinations, defers
     * shuffling until all cards are placed.
     */
    fun moveToZoneBatch(
        state: GameState,
        entityIds: List<EntityId>,
        destinationZone: Zone,
        options: ZoneEntryOptions = ZoneEntryOptions()
    ): ZoneTransitionResult {
        var currentState = state
        val allEvents = mutableListOf<EngineGameEvent>()

        for (entityId in entityIds) {
            // For batch library moves with Shuffled placement, don't shuffle per-card
            val perCardOptions = if (destinationZone == Zone.LIBRARY &&
                options.libraryPlacement is LibraryPlacement.Shuffled
            ) {
                // Place at bottom first, shuffle once at end
                options.copy(libraryPlacement = LibraryPlacement.Bottom)
            } else {
                options
            }

            val result = moveToZone(currentState, entityId, destinationZone, perCardOptions)
            currentState = result.state
            allEvents.addAll(result.events)
        }

        // Final shuffle if needed
        if (destinationZone == Zone.LIBRARY && options.libraryPlacement is LibraryPlacement.Shuffled) {
            // Find the owner from the first entity (all should have same owner for batch)
            val ownerId = entityIds.firstOrNull()?.let {
                state.getEntity(it)?.get<CardComponent>()?.ownerId
            }
            if (ownerId != null) {
                currentState = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
                    .clearLibraryReveals(currentState, ownerId)
                val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
                val (library, shuffledState) = currentState.nextRandom { shuffle(currentState.getZone(libraryZone)) }
                currentState = shuffledState.copy(zones = shuffledState.zones + (libraryZone to library))
                allEvents.add(com.wingedsheep.engine.core.LibraryShuffledEvent(ownerId))
            }
        }

        return ZoneTransitionResult(state = currentState, events = allEvents)
    }

    /**
     * Move a card from a player's hand to their graveyard as a discard.
     *
     * Emits the standard `CardsDiscardedEvent` plus the `ZoneChangeEvent` produced by
     * `moveToZone`, so dies/discard triggers and animations both see the canonical pair.
     */
    fun discardCard(state: GameState, playerId: EntityId, cardId: EntityId): ZoneTransitionResult =
        discardCards(state, playerId, listOf(cardId))

    /**
     * Move multiple cards from a player's hand to their graveyard as a single discard.
     *
     * Emits one combined `CardsDiscardedEvent` (so the client renders "You discarded X, Y"
     * as a single log entry) plus one `ZoneChangeEvent` per card from `moveToZone`.
     */
    fun discardCards(state: GameState, playerId: EntityId, cardIds: List<EntityId>): ZoneTransitionResult {
        if (cardIds.isEmpty()) return ZoneTransitionResult(state, emptyList())
        val cardNames = cardIds.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
        var newState = state
        val moveEvents = mutableListOf<EngineGameEvent>()
        for (cardId in cardIds) {
            val result = moveToZone(
                state = newState,
                entityId = cardId,
                destinationZone = Zone.GRAVEYARD,
                fromZoneKey = ZoneKey(playerId, Zone.HAND)
            )
            newState = result.state
            moveEvents.addAll(result.events)
        }
        val discardEvent = CardsDiscardedEvent(playerId, cardIds, cardNames)
        return ZoneTransitionResult(newState, listOf(discardEvent) + moveEvents)
    }

    /**
     * Track Food sacrifice for the given permanents.
     * Call this when permanents are sacrificed to mark if any were Food artifacts.
     */
    fun trackFoodSacrifice(state: GameState, permanentIds: List<EntityId>, controllerId: EntityId): GameState {
        var newState = state
        val projected = state.projectedState
        for (permId in permanentIds) {
            newState.getEntity(permId)?.get<CardComponent>() ?: continue
            if (projected.hasSubtype(permId, Subtype.FOOD.value)) {
                newState = newState.updateEntity(controllerId) { container ->
                    container.with(SacrificedFoodThisTurnComponent)
                }
                break // Only need to mark once
            }
        }
        return newState
    }

    // ── Private helpers ──

    /**
     * Apply battlefield entry components to an entity.
     */
    private fun applyBattlefieldEntry(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        controllerId: EntityId,
        options: ZoneEntryOptions,
        fromZone: Zone? = null
    ): GameState {
        return state.updateEntity(entityId) { c ->
            var updated = c.with(ControllerComponent(controllerId))

            // Clear stale LinkedExileComponent from previous battlefield visit (Rule 400.7:
            // a permanent that re-enters the battlefield is a new object with no memory of
            // its previous existence, so it should not retain links to previously exiled cards)
            updated = updated.without<LinkedExileComponent>()

            // Same for CraftedFromExiledComponent (CR 702.167c materials link): the
            // re-entering object is a new object, so it has no recorded materials. The
            // craft-return executor explicitly re-attaches the component immediately after
            // this entry path runs.
            updated = updated.without<CraftedFromExiledComponent>()

            // Track that this permanent entered the battlefield this turn
            updated = updated.with(EnteredThisTurnComponent)

            // Track reanimation (direct graveyard → battlefield) for triggers that care.
            if (fromZone == Zone.GRAVEYARD) {
                updated = updated.with(
                    com.wingedsheep.engine.state.components.battlefield.EnteredFromGraveyardComponent
                )
            }

            // All permanents enter summoning sick (CR 302.6 / 508.1a — the control-continuity
            // check is about the permanent, not whether it was a creature the whole turn).
            // Downstream checks gate on isCreature/{T}-cost so this is a no-op for lands and
            // non-creature artifacts until they become creatures (Crew, animate-land, etc.).
            updated = updated.with(SummoningSicknessComponent)

            // Tapped entry
            if (options.tapped || options.tappedAndAttacking) {
                updated = updated.with(TappedComponent)
            }

            // Tapped and attacking
            if (options.tappedAndAttacking) {
                val defenderId = state.turnOrder.firstOrNull { it != controllerId }
                if (defenderId != null) {
                    updated = updated.with(AttackingComponent(defenderId))
                }
            }

            // Face-down entry (morph)
            if (options.faceDown) {
                updated = updated.with(FaceDownComponent)
                if (options.morphData != null) {
                    updated = updated.with(options.morphData)
                }
            }

            // Register static abilities (continuous effects) and runtime replacement effects.
            // Without this, a permanent placed on the battlefield via moveToZone — leyline
            // starts, reanimation, returns from exile — would carry its CardComponent but
            // never surface its static / replacement payload to the projector or the
            // replacement-application paths. Face-down entries are excluded because face-down
            // permanents have no abilities (CR 708.2). The cast pipeline owns its own call,
            // so this does not double-run for cast spells.
            if (!options.faceDown && ::staticAbilityHandler.isInitialized) {
                updated = staticAbilityHandler.addContinuousEffectComponent(updated)
                updated = staticAbilityHandler.addReplacementEffectComponent(updated)
            }

            updated
        }
    }

    /**
     * Place a card in the library according to the LibraryPlacement strategy.
     */
    private fun placeInLibrary(
        state: GameState,
        entityId: EntityId,
        libraryZoneKey: ZoneKey,
        placement: LibraryPlacement
    ): GameState {
        val currentLibrary = state.getZone(libraryZoneKey)
        return when (placement) {
            LibraryPlacement.Top -> {
                state.copy(zones = state.zones + (libraryZoneKey to listOf(entityId) + currentLibrary))
            }
            LibraryPlacement.Bottom -> {
                state.copy(zones = state.zones + (libraryZoneKey to currentLibrary + entityId))
            }
            LibraryPlacement.Shuffled -> {
                val (newLibrary, shuffledState) = state.nextRandom { shuffle(currentLibrary + entityId) }
                shuffledState.copy(zones = shuffledState.zones + (libraryZoneKey to newLibrary))
            }
            is LibraryPlacement.NthFromTop -> {
                val insertIndex = placement.position.coerceAtMost(currentLibrary.size)
                val newLibrary = currentLibrary.toMutableList().apply { add(insertIndex, entityId) }
                state.copy(zones = state.zones + (libraryZoneKey to newLibrary))
            }
        }
    }

    // ── Cleanup helpers (moved from ZoneMovementUtils) ──

    /**
     * Apply Saga entry setup to an entity entering the battlefield (Rule 714.3a).
     */
    private fun applySagaEntryIfNeeded(
        state: GameState,
        entityId: EntityId
    ): Pair<GameState, List<EngineGameEvent>> {
        return ZoneMovementUtils.applySagaEntryIfNeeded(state, entityId)
    }

    /**
     * Clean up combat references to a leaving entity on other creatures.
     */
    private fun cleanupCombatReferences(state: GameState, entityId: EntityId): GameState {
        return ZoneMovementUtils.cleanupCombatReferences(state, entityId)
    }

    /**
     * Remove floating effects targeting an entity leaving the battlefield (Rule 400.7).
     */
    private fun removeFloatingEffectsTargeting(state: GameState, entityId: EntityId): GameState {
        return ZoneMovementUtils.removeFloatingEffectsTargeting(state, entityId)
    }

    /**
     * Clean up the reverse attachment link on the permanent this entity was attached to.
     */
    private fun cleanupReverseAttachmentLink(state: GameState, entityId: EntityId): GameState {
        return ZoneMovementUtils.cleanupReverseAttachmentLink(state, entityId)
    }

    /**
     * Strip all battlefield-specific components from an entity leaving the battlefield.
     */
    private fun stripBattlefieldComponents(container: ComponentContainer): ComponentContainer {
        return ZoneMovementUtils.stripBattlefieldComponents(container)
    }

    /**
     * Build a TypeLine that reflects the projected types/subtypes for a permanent
     * leaving the battlefield. This lets leaves-battlefield triggers match on types
     * granted by continuous effects (e.g., "whenever a Food is put into a graveyard"
     * firing for creatures Ygra turned into Food artifacts).
     *
     * Falls back to the base typeLine if projection has no entry for the entity.
     */
    private fun buildProjectedTypeLine(
        cardComponent: CardComponent,
        state: GameState,
        entityId: EntityId
    ): TypeLine {
        val baseTypeLine = cardComponent.typeLine
        val projected = state.projectedState.getProjectedValues(entityId) ?: return baseTypeLine

        val cardTypes = projected.types
            .mapNotNull { runCatching { CardType.valueOf(it) }.getOrNull() }
            .toSet()
            .ifEmpty { baseTypeLine.cardTypes }
        val subtypes = projected.subtypes.map { Subtype(it) }.toSet()
        return baseTypeLine.copy(
            cardTypes = cardTypes,
            subtypes = subtypes
        )
    }

    /**
     * Find which zone an entity is currently in.
     */
    private fun findEntityZone(state: GameState, entityId: EntityId): ZoneKey? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) {
                return zoneKey
            }
        }
        return null
    }
}
