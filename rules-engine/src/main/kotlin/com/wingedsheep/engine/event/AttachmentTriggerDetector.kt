package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent

/**
 * Handles all aura/equipment "enchanted/equipped creature" triggers using the unified
 * [TriggerBinding.ATTACHED] binding. Instead of matching specific attachment event types,
 * this detector checks if general events (DamageDealtEvent, AttackersDeclaredEvent, etc.)
 * match ATTACHED-bound triggers on auras/equipment attached to the event's entity.
 */
class AttachmentTriggerDetector(private val matcher: TriggerMatcher) {

    /**
     * Unified detection for all ATTACHED-bound triggers on battlefield auras/equipment.
     * Extracts relevant entity IDs from the engine event, looks them up in the aurasByTarget
     * index, and checks each ATTACHED trigger against the event.
     */
    fun detectAttachmentTriggers(
        state: GameState,
        event: EngineGameEvent,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        val relevantIds = getRelevantEntityIds(event)
        if (relevantIds.isEmpty()) return

        val isZoneChange = event is ZoneChangeEvent

        for (entityId in relevantIds) {
            for (entry in index.aurasByTarget[entityId].orEmpty()) {
                for (ability in entry.abilities) {
                    if (ability.binding != TriggerBinding.ATTACHED) continue
                    // For zone-change events on the attached creature (e.g., creature dies),
                    // skip auras — they go to graveyard with the creature and are handled by
                    // DeathAndLeaveTriggerDetector.detectDeadAuraAttachmentTriggers via the
                    // aura's own ZoneChangeEvent. Only equipment stays on the battlefield.
                    if (isZoneChange && ability.trigger is GameEvent.ZoneChangeEvent &&
                        !entry.cardComponent.typeLine.isEquipment) continue
                    if (matchesAttachedTrigger(ability.trigger, event, entityId, state)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entry.entityId,
                                sourceName = entry.cardComponent.name,
                                controllerId = entry.controllerId,
                                triggerContext = buildTriggerContext(event, entityId)
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Extract entity IDs from the engine event that should be looked up in aurasByTarget.
     * Returns a list because damage events need to check both source and target.
     */
    private fun getRelevantEntityIds(event: EngineGameEvent): List<com.wingedsheep.sdk.model.EntityId> {
        return when (event) {
            is DamageDealtEvent -> {
                // Check both target (for "enchanted creature takes damage") and
                // source (for "enchanted creature deals damage")
                buildList {
                    if (!event.targetIsPlayer) add(event.targetId)
                    event.sourceId?.let { add(it) }
                }
            }
            is AttackersDeclaredEvent -> event.attackers
            is TurnFaceUpEvent -> listOf(event.entityId)
            is TappedEvent -> listOf(event.entityId)
            is ZoneChangeEvent -> {
                if (event.fromZone == Zone.BATTLEFIELD) listOf(event.entityId) else emptyList()
            }
            else -> emptyList()
        }
    }

    /**
     * Check whether an ATTACHED trigger's event type matches the given engine event
     * for a specific attached entity.
     */
    private fun matchesAttachedTrigger(
        trigger: GameEvent,
        event: EngineGameEvent,
        attachedEntityId: com.wingedsheep.sdk.model.EntityId,
        state: GameState
    ): Boolean {
        return when (trigger) {
            is GameEvent.DamageReceivedEvent -> {
                event is DamageDealtEvent && event.targetId == attachedEntityId
            }
            is GameEvent.DealsDamageEvent -> {
                event is DamageDealtEvent &&
                    event.sourceId == attachedEntityId &&
                    matcher.matchesDealsDamageTrigger(trigger, event, state)
            }
            is GameEvent.AttackEvent -> {
                event is AttackersDeclaredEvent && attachedEntityId in event.attackers
            }
            is GameEvent.TapEvent -> {
                event is TappedEvent && event.entityId == attachedEntityId
            }
            is GameEvent.TurnFaceUpEvent -> {
                event is TurnFaceUpEvent && event.entityId == attachedEntityId
            }
            is GameEvent.ZoneChangeEvent -> {
                if (event !is ZoneChangeEvent) return false
                if (event.entityId != attachedEntityId) return false
                if (trigger.from != null && event.fromZone != trigger.from) return false
                if (trigger.to != null && event.toZone != trigger.to) return false
                true
            }
            else -> false
        }
    }

    /**
     * Build trigger context from the engine event, using the attached entity as the triggering entity.
     */
    private fun buildTriggerContext(
        event: EngineGameEvent,
        attachedEntityId: com.wingedsheep.sdk.model.EntityId
    ): TriggerContext {
        return when (event) {
            is DamageDealtEvent -> TriggerContext.fromEvent(event)
            else -> TriggerContext(triggeringEntityId = attachedEntityId)
        }
    }
}
