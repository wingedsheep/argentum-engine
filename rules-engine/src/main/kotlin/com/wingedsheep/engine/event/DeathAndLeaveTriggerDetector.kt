package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageDealtToCreaturesThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent

/**
 * Handles all dies/leaves-battlefield triggers.
 */
class DeathAndLeaveTriggerDetector(
    private val abilityResolver: TriggerAbilityResolver,
    private val matcher: TriggerMatcher
) {

    /**
     * Resolve the dying entity's identity either from its still-present container or from
     * last-known info on the event. The latter fallback is required for tokens, which are
     * removed from the game by 704.5s in the same SBA pass that puts them in the graveyard
     * (see [com.wingedsheep.engine.mechanics.sba.zone.TokensInWrongZonesCheck]).
     */
    private data class DyingEntityInfo(
        val cardDefinitionId: String,
        val name: String
    )

    private fun resolveDyingEntity(state: GameState, event: ZoneChangeEvent): DyingEntityInfo? {
        val container = state.getEntity(event.entityId)
        if (container != null) {
            // Face-down creatures have no abilities (Rule 707.2)
            if (container.has<FaceDownComponent>()) return null
            val cardComponent = container.get<CardComponent>() ?: return null
            return DyingEntityInfo(cardComponent.cardDefinitionId, cardComponent.name)
        }
        // Entity is gone (e.g., token already cleaned up by 704.5s). Fall back to the
        // last-known info captured on the event.
        val cardDefId = event.lastKnownCardDefinitionId ?: return null
        return DyingEntityInfo(cardDefId, event.entityName)
    }

    fun detectDeathTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.entityId
        val info = resolveDyingEntity(state, event) ?: return

        // For "When this creature dies" - the creature might be in graveyard now
        // Look up abilities by card definition
        val abilities = abilityResolver.getTriggeredAbilities(entityId, info.cardDefinitionId, state)
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
                            sourceName = info.name,
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
                                sourceName = info.name,
                                controllerId = controllerId,
                                triggerContext = TriggerContext.fromEvent(event)
                            )
                        )
                    }
                }
                TriggerBinding.OTHER -> {
                    // "Whenever another creature dies" - never matches its own death
                }
                TriggerBinding.ATTACHED -> {
                    // Handled by detectDeadAuraAttachmentTriggers
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

            // Skip if this creature is still on the battlefield (already handled by main loop)
            if (deadEntityId in state.getBattlefield()) continue

            val info = resolveDyingEntity(state, deadEvent) ?: continue

            val abilities = abilityResolver.getTriggeredAbilities(deadEntityId, info.cardDefinitionId, state)
            val controllerId = deadEvent.ownerId

            for (ability in abilities) {
                for (otherDeathEvent in deathEvents) {
                    if (otherDeathEvent.entityId == deadEntityId) continue // Skip self

                    if (matcher.matchesTrigger(ability.trigger, ability.binding, otherDeathEvent, deadEntityId, controllerId, state)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = deadEntityId,
                                sourceName = info.name,
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
     * Detect ATTACHED zone-change triggers on auras that went to graveyard with their creature.
     * When an aura goes to graveyard because its enchanted creature died/left, the ZoneChangeEvent
     * for the aura carries [ZoneChangeEvent.lastKnownAttachedTo] = the creature's ID.
     * We look up the aura's card definition for ATTACHED-bound ZoneChangeEvent triggers.
     *
     * Handles both "enchanted creature dies" and "enchanted permanent leaves the battlefield".
     */
    fun detectDeadAuraAttachmentTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val attachedEntityId = event.lastKnownAttachedTo ?: return

        val auraEntityId = event.entityId
        val container = state.getEntity(auraEntityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        val abilities = abilityResolver.getTriggeredAbilities(auraEntityId, cardComponent.cardDefinitionId, state)
        val controllerId = event.ownerId

        for (ability in abilities) {
            if (ability.binding != TriggerBinding.ATTACHED) continue
            val trigger = ability.trigger
            if (trigger !is GameEvent.ZoneChangeEvent) continue
            if (trigger.from != null && trigger.from != Zone.BATTLEFIELD) continue
            if (trigger.to != null) {
                // "Dies" trigger: verify creature is actually in graveyard (not exiled or bounced)
                val inTargetZone = state.turnOrder.any { attachedEntityId in state.getGraveyard(it) }
                if (!inTargetZone) continue
            }
            triggers.add(
                PendingTrigger(
                    ability = ability,
                    sourceId = auraEntityId,
                    sourceName = cardComponent.name,
                    controllerId = controllerId,
                    triggerContext = TriggerContext(triggeringEntityId = attachedEntityId)
                )
            )
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
     * Detect persist triggers (CR 702.79) when a nontoken creature dies with no -1/-1 counters on it.
     *
     * Persist is a triggered ability keyword: "When this creature is put into a graveyard from the
     * battlefield, if it had no -1/-1 counters on it, return it to the battlefield under its owner's
     * control with a -1/-1 counter on it."
     *
     * Checks projected keywords captured on the event — both intrinsic persist (printed on the card)
     * and granted persist (e.g., from a lord's static ability) reach projected keywords the same way.
     *
     * Suppresses on tokens: per 702.79b, persist does not return tokens (they cease to exist via
     * 704.5s before any return could happen).
     */
    fun detectPersistTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        if (event.fromZone != Zone.BATTLEFIELD || event.toZone != Zone.GRAVEYARD) return
        if (event.lastKnownWasToken) return
        if (event.lastKnownMinusOneMinusOneCounterCount > 0) return
        if (Keyword.PERSIST.name !in event.lastKnownKeywords) return

        val info = resolveDyingEntity(state, event) ?: return

        val persistAbility = TriggeredAbility.create(
            trigger = GameEvent.ZoneChangeEvent(
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.SELF,
            effect = CompositeEffect(
                listOf(
                    MoveToZoneEffect(
                        target = EffectTarget.Self,
                        destination = Zone.BATTLEFIELD
                    ),
                    AddCountersEffect(
                        counterType = Counters.MINUS_ONE_MINUS_ONE,
                        count = 1,
                        target = EffectTarget.Self
                    )
                )
            ),
            descriptionOverride = "Persist — Return ${info.name} to the battlefield with a -1/-1 counter on it"
        )

        triggers.add(
            PendingTrigger(
                ability = persistAbility,
                sourceId = event.entityId,
                sourceName = info.name,
                controllerId = event.ownerId,
                triggerContext = TriggerContext.fromEvent(event)
            )
        )
    }

    /**
     * Detect "leaves the battlefield" triggers on permanents that just left.
     * Similar to detectDeathTriggers, but handles ZoneChangeEvent(from=BATTLEFIELD).
     *
     * SELF binding always fires. ANY binding fires if the leaving entity itself matches the
     * trigger's filter (e.g., Three Tree Scribe's "Whenever this creature or another creature
     * you control leaves the battlefield without dying" must fire when the Scribe itself
     * leaves — the main battlefield loop misses this because the source is no longer on the
     * battlefield by the time the event is processed).
     */
    fun detectLeavesBattlefieldTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.entityId
        val info = resolveDyingEntity(state, event) ?: return

        val abilities = abilityResolver.getTriggeredAbilities(entityId, info.cardDefinitionId, state)
        val controllerId = event.ownerId

        for (ability in abilities) {
            if (!matcher.isLeavesBattlefieldTrigger(ability.trigger)) continue

            when (ability.binding) {
                TriggerBinding.SELF -> {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entityId,
                            sourceName = info.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
                TriggerBinding.ANY -> {
                    // "Whenever [this creature or] another creature you control leaves..." —
                    // check the event against the trigger's filter/excludeTo/binding.
                    if (matcher.matchesTrigger(ability.trigger, ability.binding, event, entityId, controllerId, state)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entityId,
                                sourceName = info.name,
                                controllerId = controllerId,
                                triggerContext = TriggerContext.fromEvent(event)
                            )
                        )
                    }
                }
                TriggerBinding.OTHER -> {
                    // "Whenever another permanent leaves" never fires for its own departure.
                }
                TriggerBinding.ATTACHED -> {
                    // Handled by attachment detectors.
                }
            }
        }
    }
}
