package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageDealtToCreaturesThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent

/**
 * Handles all dies/leaves-battlefield triggers.
 */
class DeathAndLeaveTriggerDetector(
    private val abilityResolver: TriggerAbilityResolver,
    private val matcher: TriggerMatcher
) {

    fun detectDeathTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.entityId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        // Face-down creatures have no abilities (Rule 707.2)
        if (container.has<FaceDownComponent>()) return

        // For "When this creature dies" - the creature might be in graveyard now
        // Look up abilities by card definition
        val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)
        val controllerId = event.ownerId

        for (ability in abilities) {
            if (!matcher.isDeathTrigger(ability.trigger)) continue

            when (ability.binding) {
                TriggerBinding.SELF -> {
                    // "When this creature dies" - always matches its own death
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entityId,
                            sourceName = cardComponent.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
                TriggerBinding.ANY -> {
                    // "Whenever a creature [matching filter] dies" - check if its own death matches
                    // (e.g., Sultai Flayer: "Whenever a creature you control with toughness 4 or greater dies")
                    if (matcher.matchesTrigger(ability.trigger, ability.binding, event, entityId, controllerId, state)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entityId,
                                sourceName = cardComponent.name,
                                controllerId = controllerId,
                                triggerContext = TriggerContext.fromEvent(event)
                            )
                        )
                    }
                }
                TriggerBinding.OTHER -> {
                    // "Whenever another creature dies" - never matches its own death
                }
            }
        }
    }

    /**
     * Rule 603.10: Handle "look back in time" for simultaneous deaths.
     * When multiple creatures die at the same time, each dead creature should
     * still see the others dying for trigger purposes. The main battlefield loop
     * misses these because the creatures are already in the graveyard.
     *
     * This checks dead creatures' non-self death triggers (e.g., ZoneChangeEvent with OTHER/ANY binding)
     * against other death events in the same batch.
     */
    fun detectSimultaneousDeathTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        // Collect all death events from this batch
        val deathEvents = events.filterIsInstance<ZoneChangeEvent>().filter {
            it.toZone == Zone.GRAVEYARD &&
                it.fromZone == Zone.BATTLEFIELD
        }
        if (deathEvents.size < 2) return // Need at least 2 simultaneous deaths

        // For each dead creature, check its triggers against OTHER death events
        for (deadEvent in deathEvents) {
            val deadEntityId = deadEvent.entityId
            val container = state.getEntity(deadEntityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) continue

            // Skip if this creature is still on the battlefield (already handled by main loop)
            if (deadEntityId in state.getBattlefield()) continue

            val abilities = abilityResolver.getTriggeredAbilities(deadEntityId, cardComponent.cardDefinitionId, state)
            val controllerId = deadEvent.ownerId

            for (ability in abilities) {
                for (otherDeathEvent in deathEvents) {
                    if (otherDeathEvent.entityId == deadEntityId) continue // Skip self

                    if (matcher.matchesTrigger(ability.trigger, ability.binding, otherDeathEvent, deadEntityId, controllerId, state)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = deadEntityId,
                                sourceName = cardComponent.name,
                                controllerId = controllerId,
                                triggerContext = TriggerContext.fromEvent(otherDeathEvent)
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Detect "when enchanted creature dies" triggers on auras.
     * When an aura goes to graveyard because its enchanted creature died, the ZoneChangeEvent
     * for the aura carries [ZoneChangeEvent.lastKnownAttachedTo] = the dying creature's ID.
     * We look up the aura's card definition for EnchantedCreatureDiesEvent triggers.
     */
    fun detectEnchantedCreatureDiesTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        // Only process if this is an aura that was attached to something
        val attachedCreatureId = event.lastKnownAttachedTo ?: return

        // "Dies" means moved to graveyard — verify the creature is actually in a graveyard
        // (not exiled or bounced). Check all graveyards for the creature.
        val creatureInGraveyard = state.turnOrder.any { playerId ->
            attachedCreatureId in state.getGraveyard(playerId)
        }
        if (!creatureInGraveyard) return

        val auraEntityId = event.entityId
        val container = state.getEntity(auraEntityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        // Look up abilities from card definition (aura is now in graveyard, so use card def)
        val abilities = abilityResolver.getTriggeredAbilities(auraEntityId, cardComponent.cardDefinitionId, state)

        for (ability in abilities) {
            if (ability.trigger !is GameEvent.EnchantedCreatureDiesEvent) continue

            val controllerId = event.ownerId
            triggers.add(
                PendingTrigger(
                    ability = ability,
                    sourceId = auraEntityId,
                    sourceName = cardComponent.name,
                    controllerId = controllerId,
                    triggerContext = TriggerContext(triggeringEntityId = attachedCreatureId)
                )
            )
        }
    }

    /**
     * Detect "when enchanted permanent leaves the battlefield" triggers on auras.
     * Similar to [detectEnchantedCreatureDiesTriggers] but fires for any zone change
     * (not just dying). The enchanted permanent may have been exiled, bounced, etc.
     */
    fun detectEnchantedPermanentLeavesBattlefieldTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        // Only process if this is an aura that was attached to something
        val attachedPermanentId = event.lastKnownAttachedTo ?: return

        val auraEntityId = event.entityId
        val container = state.getEntity(auraEntityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        // Look up abilities from card definition (aura is now in graveyard, so use card def)
        val abilities = abilityResolver.getTriggeredAbilities(auraEntityId, cardComponent.cardDefinitionId, state)

        for (ability in abilities) {
            if (ability.trigger !is GameEvent.EnchantedPermanentLeavesBattlefieldEvent) continue

            val controllerId = event.ownerId
            triggers.add(
                PendingTrigger(
                    ability = ability,
                    sourceId = auraEntityId,
                    sourceName = cardComponent.name,
                    controllerId = controllerId,
                    triggerContext = TriggerContext(triggeringEntityId = attachedPermanentId)
                )
            )
        }
    }

    /**
     * Detect "when equipped creature dies" triggers on equipment.
     * Unlike auras (which go to graveyard with the creature), equipment stays on the battlefield.
     * When a creature dies, we check the aurasByTarget index for equipment attached to it
     * that has EquippedCreatureDiesEvent triggers.
     */
    fun detectEquippedCreatureDiesTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        val dyingCreatureId = event.entityId

        for (entry in index.aurasByTarget[dyingCreatureId].orEmpty()) {
            // Only process equipment (not auras — those are handled by detectEnchantedCreatureDiesTriggers)
            val isEquipment = entry.cardComponent.typeLine.isEquipment
            if (!isEquipment) continue

            for (ability in entry.abilities) {
                if (ability.trigger !is GameEvent.EquippedCreatureDiesEvent) continue

                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = entry.entityId,
                        sourceName = entry.cardComponent.name,
                        controllerId = entry.controllerId,
                        triggerContext = TriggerContext(triggeringEntityId = dyingCreatureId)
                    )
                )
            }
        }
    }

    /**
     * Detect "whenever a creature dealt damage by this creature this turn dies" triggers.
     * Uses pre-indexed entities with CreatureDealtDamageBySourceDiesEvent triggers instead
     * of scanning all battlefield permanents.
     */
    fun detectCreatureDealtDamageBySourceDiesTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState,
        index: TriggerIndex
    ) {
        val dyingEntityId = event.entityId

        for (entry in index.creatureDamageDeathTrackers) {
            val container = state.getEntity(entry.entityId) ?: continue

            // Check if this permanent dealt damage to the dying creature this turn
            val damageTracking = container.get<DamageDealtToCreaturesThisTurnComponent>() ?: continue
            if (dyingEntityId !in damageTracking.creatureIds) continue

            for (ability in entry.abilities) {
                if (ability.trigger !is GameEvent.CreatureDealtDamageBySourceDiesEvent) continue
                if (ability.activeZone != Zone.BATTLEFIELD) continue

                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = entry.entityId,
                        sourceName = entry.cardComponent.name,
                        controllerId = entry.controllerId,
                        triggerContext = TriggerContext(triggeringEntityId = dyingEntityId)
                    )
                )
            }
        }
    }

    /**
     * Detect "leaves the battlefield" triggers on permanents that just left.
     * Similar to detectDeathTriggers, but handles ZoneChangeEvent(from=BATTLEFIELD) with SELF binding.
     */
    fun detectLeavesBattlefieldTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.entityId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        // Face-down creatures have no abilities (Rule 707.2)
        if (container.has<FaceDownComponent>()) return

        val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)
        val controllerId = event.ownerId

        for (ability in abilities) {
            if (matcher.isLeavesBattlefieldTrigger(ability.trigger) && ability.binding == TriggerBinding.SELF) {
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = entityId,
                        sourceName = cardComponent.name,
                        controllerId = controllerId,
                        triggerContext = TriggerContext.fromEvent(event)
                    )
                )
            }
        }
    }
}
