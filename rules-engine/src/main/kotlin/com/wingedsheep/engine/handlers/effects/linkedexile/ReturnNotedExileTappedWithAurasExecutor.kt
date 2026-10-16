package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.core.PermanentAttachedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.NotedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ReturnNotedExileTappedWithAurasEffect
import kotlin.reflect.KClass

/**
 * Executor for [ReturnNotedExileTappedWithAurasEffect] (Tawnos's Coffin's return).
 *
 * Reads the source's [NotedExileComponent] + [LinkedExileComponent], then:
 *  1. Returns the noted principal creature to the battlefield **tapped, under its owner's control**,
 *     with the noted number and kind of counters restored (CR: "tapped with the noted number and
 *     kind of counters on it").
 *  2. Returns the other linked-exiled cards (the creature's Auras) to the battlefield attached to
 *     that creature. Auras that can't legally re-attach are sent to their owners' graveyards by the
 *     unattached-Aura state-based action (CR 704.5m) — the "If you don't …" fallback.
 *
 * Both returns go through [ZoneTransitionService.moveToZone] (the canonical battlefield-entry
 * pipeline) so the re-entering objects get the full entry treatment — enters-the-battlefield
 * replacement effects, summoning sickness, and static/replacement-ability registration (the Aura's
 * own "enchanted creature gets …" static included) — and the standard `ZoneChangeEvent` so ETB
 * triggers fire. The noted counters are applied after the creature re-enters; the Aura is attached
 * after it re-enters (the "return attached" wording bypasses normal Aura targeting).
 *
 * A no-op when nothing is noted or the principal is no longer in exile, so firing it from both the
 * becomes-untapped and the leaves-the-battlefield trigger is safe (whichever fires first does the
 * return; the other finds nothing). Clears the noted/linked bookkeeping on the source after a
 * successful return.
 */
class ReturnNotedExileTappedWithAurasExecutor(private val zones: ZoneTransitionService) : EffectExecutor<ReturnNotedExileTappedWithAurasEffect> {

    override val effectType: KClass<ReturnNotedExileTappedWithAurasEffect> =
        ReturnNotedExileTappedWithAurasEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnNotedExileTappedWithAurasEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        val sourceContainer = state.getEntity(sourceId) ?: return EffectResult.success(state)

        val noted = sourceContainer.get<NotedExileComponent>() ?: return EffectResult.success(state)
        val linked = sourceContainer.get<LinkedExileComponent>()?.exiledIds ?: emptyList()

        fun inExile(id: EntityId): Boolean {
            val ownerId = ownerOf(state, id) ?: return false
            return id in state.getZone(ZoneKey(ownerId, Zone.EXILE))
        }

        val creatureId = noted.principalId
        if (!inExile(creatureId)) {
            // Principal already returned (or otherwise gone) — clear stale bookkeeping and stop.
            return EffectResult.success(clearBookkeeping(state, sourceId))
        }

        val creatureOwner = ownerOf(state, creatureId)
            ?: return EffectResult.success(clearBookkeeping(state, sourceId))

        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        var newState = state

        // 1. Return the creature tapped, under its owner's control, via the canonical pipeline.
        val creatureReturn = zones.moveToZone(
            newState, creatureId, Zone.BATTLEFIELD,
            options = ZoneEntryOptions(controllerId = creatureOwner, tapped = true)
        )
        newState = creatureReturn.state
        events.addAll(creatureReturn.events)

        // Restore the noted counters once it's back on the battlefield (a new object). It returns
        // "with" them — counters given as it enters (CR 122.6) — so each kind is an entry-counter
        // placement: the placement replacements apply, read against the battlefield before the
        // return, and the creature's controller is the placer (CR 122.6a).
        if (creatureId in newState.getBattlefield() && noted.notedCounters.isNotEmpty()) {
            val name = newState.getEntity(creatureId)?.get<CardComponent>()?.name ?: ""
            for ((kind, amount) in noted.notedCounters) {
                val (placed, placedEvents) = com.wingedsheep.engine.handlers.effects.EntersWithReplacements
                    .placeEntryCounters(
                        newState, creatureId, kind, amount, creatureOwner, name,
                        predicateEvaluator = zones.predicateEvaluator, replacementSourceState = state
                    )
                newState = placed
                events.addAll(placedEvents)
            }
        }

        // 2. Return the other linked-exiled cards (Auras) attached to the creature. If the creature
        // didn't make it back (e.g. a replacement redirected it), the Auras re-enter unattached and
        // the unattached-Aura SBA (CR 704.5m) bins them — the correct fallback.
        if (creatureId in newState.getBattlefield()) {
            val auraIds = linked.filter { it != creatureId && inExile(it) }
            for (auraId in auraIds) {
                val auraOwner = ownerOf(newState, auraId) ?: continue
                val auraName = newState.getEntity(auraId)?.get<CardComponent>()?.name ?: "Aura"

                val auraReturn = zones.moveToZone(
                    newState, auraId, Zone.BATTLEFIELD,
                    options = ZoneEntryOptions(controllerId = auraOwner)
                )
                newState = auraReturn.state
                events.addAll(auraReturn.events)
                if (auraId !in newState.getBattlefield()) continue

                // Attach to the creature (the "return attached" wording bypasses Aura targeting).
                newState = newState.updateEntity(auraId) { c -> c.with(AttachedToComponent(creatureId)) }
                newState = newState.updateEntity(creatureId) { c ->
                    val existing = c.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
                    c.with(AttachmentsComponent(existing + auraId))
                }
                events.add(
                    PermanentAttachedEvent(
                        attachmentId = auraId,
                        attachmentName = auraName,
                        attachedToId = creatureId,
                        controllerId = auraOwner
                    )
                )
            }
        }

        newState = clearBookkeeping(newState, sourceId)
        return EffectResult.success(newState, events)
    }

    private fun ownerOf(state: GameState, id: EntityId): EntityId? {
        val container = state.getEntity(id) ?: return null
        return container.get<OwnerComponent>()?.playerId
            ?: container.get<CardComponent>()?.ownerId
    }

    private fun clearBookkeeping(state: GameState, sourceId: EntityId): GameState =
        state.updateEntity(sourceId) { c ->
            c.without<NotedExileComponent>().without<LinkedExileComponent>()
        }
}
