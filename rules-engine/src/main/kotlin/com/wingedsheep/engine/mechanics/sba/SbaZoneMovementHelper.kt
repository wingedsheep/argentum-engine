package com.wingedsheep.engine.mechanics.sba

import com.wingedsheep.engine.core.CreatureDestroyedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Consolidates zone movement logic for state-based actions.
 *
 * All SBAs that move permanents off the battlefield call this helper instead of
 * doing inline zone movement. This ensures the full cleanup pipeline runs:
 *
 * 1. Capture last-known information (counters, P/T, type line)
 * 2. Check ExileOnDeath floating effect replacement
 * 3. checkZoneChangeRedirect() for replacement effects
 * 4. cleanupReverseAttachmentLink()
 * 5. Remove from zone / add to zone
 * 6. cleanupCombatReferences()
 * 7. stripBattlefieldComponents()
 * 8. removeFloatingEffectsTargeting() (Rule 400.7 — the previously missing step)
 * 9. Apply additional replacement effects
 * 10. Handle ExileControllerGraveyardOnDeath if applicable
 * 11. Emit events
 */
object SbaZoneMovementHelper {

    /**
     * Move a creature to graveyard via SBA (zero toughness, lethal damage).
     * Emits both CreatureDestroyedEvent and ZoneChangeEvent.
     * Respects ExileOnDeath, zone change redirects, and ExileControllerGraveyardOnDeath.
     *
     * @param passStartState The state as it stood when the SBA check pass began, before any
     *        creature in this batch was moved. CR 704.3 performs all applicable state-based
     *        actions simultaneously as a single event, so a permanent hosting a "would die →
     *        exile it instead" replacement (Head of the Hunt, The Darkness Crystal, Valgavoth)
     *        still shields the creatures dying alongside it — it is on the battlefield right up
     *        until the event happens (CR 614.1). Callers move creatures one at a time through a
     *        progressively mutated state, so the replacement lookup must read this snapshot
     *        instead; otherwise battlefield iteration order decides whether the shield applies.
     *        Defaults to [state] for callers outside a batch.
     */
    fun putCreatureInGraveyard(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        reason: String,
        passStartState: GameState = state
    ): ExecutionResult {
        val container = state.getEntity(entityId) ?: return ExecutionResult.success(state)
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.success(state)

        // Check for ExileOnDeath replacement effect
        val exileOnDeathIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.ExileOnDeath &&
                entityId in effect.effect.affectedEntities
        }
        val exileInstead = exileOnDeathIndex != -1

        // Check for RedirectZoneChange replacement effects. Battlefield-sourced shields are read
        // off the pass-start snapshot so a shield dying in the same SBA batch still applies.
        val redirectResult = ZoneMovementUtils.checkZoneChangeRedirect(
            state, entityId, Zone.BATTLEFIELD, Zone.GRAVEYARD,
            battlefieldSourceState = passStartState
        )
        val destinationZone = if (exileInstead) Zone.EXILE else redirectResult.destinationZone

        var newState = state

        // Consume the ExileOnDeath floating effect if used
        if (exileInstead) {
            val updatedEffects = state.floatingEffects.toMutableList()
            updatedEffects.removeAt(exileOnDeathIndex)
            newState = newState.copy(floatingEffects = updatedEffects)
        }

        // Check for ExileControllerGraveyardOnDeath marker
        val exileGraveyardIndex = newState.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.ExileControllerGraveyardOnDeath &&
                entityId in effect.effect.affectedEntities
        }
        val exileGraveyard = exileGraveyardIndex != -1

        // Consume the ExileControllerGraveyardOnDeath floating effect if present
        if (exileGraveyard) {
            val updatedEffects = newState.floatingEffects.toMutableList()
            val currentIndex = updatedEffects.indexOfFirst { effect ->
                effect.effect.modification is SerializableModification.ExileControllerGraveyardOnDeath &&
                    entityId in effect.effect.affectedEntities
            }
            if (currentIndex != -1) {
                updatedEffects.removeAt(currentIndex)
                newState = newState.copy(floatingEffects = updatedEffects)
            }
        }

        // Delegate zone movement to ZoneTransitionService for full cleanup. A card-intrinsic
        // redirect into the library (Darksteel Colossus, Progenitus) shuffles the card in — the
        // redirect check ran above, so carry its Shuffled placement through the skipped move.
        val deathLibraryPlacement =
            if (redirectResult.shuffleIntoLibrary && destinationZone == Zone.LIBRARY) {
                com.wingedsheep.engine.handlers.effects.LibraryPlacement.Shuffled
            } else {
                com.wingedsheep.engine.handlers.effects.LibraryPlacement.Top
            }
        val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            newState, entityId, destinationZone,
            com.wingedsheep.engine.handlers.effects.ZoneEntryOptions(
                skipZoneChangeRedirect = true,
                libraryPlacement = deathLibraryPlacement
            )
        )
        newState = transitionResult.state

        val events = mutableListOf<GameEvent>(
            CreatureDestroyedEvent(entityId, cardComponent.name, reason, controllerId)
        )
        events.addAll(transitionResult.events)

        // If ExileControllerGraveyardOnDeath was triggered, exile the controller's graveyard
        if (exileGraveyard) {
            val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
            val exileZone = ZoneKey(controllerId, Zone.EXILE)
            val graveyardCardIds = newState.getZone(graveyardZone).toList()
            for (cardId in graveyardCardIds) {
                val cardComp = newState.getEntity(cardId)?.get<CardComponent>()
                val cardOwnerId = cardComp?.ownerId ?: controllerId
                val ownerExileZone = ZoneKey(cardOwnerId, Zone.EXILE)
                newState = newState.removeFromZone(graveyardZone, cardId)
                newState = newState.addToZone(ownerExileZone, cardId)
                // Same stamp ZoneTransitionService writes on an effect-driven exile — these cards
                // came from a graveyard, so a later CR 610.3 "return it to its previous zone"
                // (CardDestination.ToZoneExiledFrom) must not reanimate them via the fallback.
                newState = newState.updateEntity(cardId) { c ->
                    c.with(
                        com.wingedsheep.engine.state.components.identity
                            .ExiledFromZoneComponent(Zone.GRAVEYARD)
                    )
                }
                events.add(
                    ZoneChangeEvent(
                        cardId,
                        cardComp?.name ?: "Unknown",
                        Zone.GRAVEYARD,
                        Zone.EXILE,
                        cardOwnerId
                    )
                )
            }
        }

        // Link the exiled card to a RedirectZoneChange(linkToSource) source (Valgavoth) — the
        // move above ran with skipZoneChangeRedirect=true, so the link is applied here.
        //
        // Only while the source is still on the battlefield *after* this move. Since the
        // replacement is now looked up in the pass-start snapshot, the source may itself have died
        // in this same SBA batch — the shield still applies (CR 704.3), but a permanent that left
        // has had its LinkedExileComponent stripped, and writing one back would leave a stale exile
        // list on a card in the graveyard that nothing cleans up (removeFromLinkedExiles only walks
        // the battlefield). Returning to the battlefield makes it a new object with no memory of
        // what the old one exiled (CR 400.7), so the link must not survive the source's death.
        if (!exileInstead && destinationZone == Zone.EXILE && redirectResult.linkSourceId != null &&
            redirectResult.linkSourceId in newState.getBattlefield()
        ) {
            newState = ZoneMovementUtils.linkExiledToSource(newState, entityId, redirectResult.linkSourceId)
        }

        // Apply additional replacement effect (e.g., Ugin's Nexus extra turn, Darigaaz egg
        // counters, The Darkness Crystal's "you gain 2 life").
        if (redirectResult.additionalEffect != null) {
            val (updatedState, extraEvents) = ZoneMovementUtils.applyReplacementAdditionalEffect(
                newState, redirectResult.additionalEffect, redirectResult.effectControllerId, entityId,
                sourceId = redirectResult.effectSourceId
            )
            newState = updatedState
            events.addAll(extraEvents)
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Move a permanent to graveyard via SBA (planeswalker loyalty, saga sacrifice,
     * unattached aura, legend rule). Emits ZoneChangeEvent only (no CreatureDestroyedEvent).
     * Respects zone change redirects.
     */
    fun putPermanentInGraveyard(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        lastKnownAttachedTo: EntityId? = null
    ): ExecutionResult {
        // Delegate zone movement to ZoneTransitionService for full cleanup
        val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            state, entityId, Zone.GRAVEYARD,
            com.wingedsheep.engine.handlers.effects.ZoneEntryOptions(lastKnownAttachedTo = lastKnownAttachedTo)
        )

        return ExecutionResult.success(transitionResult.state, transitionResult.events)
    }
}
