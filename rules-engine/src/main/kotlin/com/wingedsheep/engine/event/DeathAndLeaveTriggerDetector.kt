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
import com.wingedsheep.sdk.scripting.EventPattern
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
     * Resolve the dying entity's identity from the last-known snapshot captured the instant it
     * left the battlefield (CR 113.7a / 603.10 / 608.2h), falling back to its still-present
     * container only when no snapshot was captured.
     *
     * The snapshot is required, not just sufficient: a double-faced permanent's [CardComponent]
     * is reset to its front face as part of the very same zone-move (CR 712.8a, applied in
     * [com.wingedsheep.engine.handlers.effects.ZoneTransitionService]) *before* trigger detection
     * ever runs. A back face's own "when this dies"/"when this leaves" trigger (Galian Beast)
     * must therefore be looked up by the card definition it had at the moment it died — captured
     * pre-reset on [ZoneChangeEvent.lastKnown] — not the already-reset live container, or the
     * lookup silently returns the front face's abilities instead and the back face's trigger
     * never fires. For every non-DFC card the two values are identical, so this is a no-op there.
     *
     * The live-container fallback below only matters when no snapshot was captured at all (a
     * theoretical safety net — every battlefield-leaving event reaching this function populates
     * [ZoneChangeEvent.lastKnown]); the container itself may already be gone for a token cleaned
     * up by 704.5s in the same SBA pass that put it in the graveyard (see
     * [com.wingedsheep.engine.mechanics.sba.zone.TokensInWrongZonesCheck]).
     */
    private data class DyingEntityInfo(
        val cardDefinitionId: String,
        val name: String
    )

    private fun resolveDyingEntity(state: GameState, event: ZoneChangeEvent): DyingEntityInfo? {
        val container = state.getEntity(event.entityId)
        // Face-down creatures have no abilities (Rule 708.2)
        if (container?.has<FaceDownComponent>() == true) return null

        event.lastKnown?.cardDefinitionId?.let { cardDefId ->
            return DyingEntityInfo(cardDefId, event.entityName)
        }
        if (container != null) {
            val cardComponent = container.get<CardComponent>() ?: return null
            return DyingEntityInfo(cardComponent.cardDefinitionId, cardComponent.name)
        }
        // Entity is gone (e.g., token already cleaned up by 704.5s). Fall back to the
        // last-known info captured on the event.
        val cardDefId = event.lastKnown?.cardDefinitionId ?: return null
        return DyingEntityInfo(cardDefId, event.entityName)
    }

    fun detectDeathTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        // The entity had its abilities stripped at leaving time (e.g. Xu-Ifit's
        // "It's a Skeleton ... and has no abilities" rider). Its own dies triggers don't
        // fire. Other creatures' "Whenever a creature dies" triggers are detected from
        // their own sources elsewhere, so they're unaffected by this short-circuit.
        if (event.lastKnown?.lostAllAbilities == true) return

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

            // Skip creatures whose abilities were stripped at leaving time (Xu-Ifit's
            // ability-stripped reanimate target contributes no triggers of its own even
            // when it dies alongside other creatures).
            if (deadEvent.lastKnown?.lostAllAbilities == true) continue

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
     * for the aura carries [ZoneChangeEvent.lastKnown?.attachedTo] = the creature's ID.
     * We look up the aura's card definition for ATTACHED-bound ZoneChangeEvent triggers.
     *
     * Handles both "enchanted creature dies" and "enchanted permanent leaves the battlefield".
     */
    fun detectDeadAuraAttachmentTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        // An aura whose abilities were stripped at leaving time (e.g. via a Xu-Ifit-shaped
        // reanimate on an aura, or Humility-style suppression in effect at LTB) contributes
        // no attached zone-change triggers.
        if (event.lastKnown?.lostAllAbilities == true) return

        val attachedEntityId = event.lastKnown?.attachedTo ?: return

        val auraEntityId = event.entityId
        val container = state.getEntity(auraEntityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return

        val abilities = abilityResolver.getTriggeredAbilities(auraEntityId, cardComponent.cardDefinitionId, state)
        val controllerId = event.ownerId

        for (ability in abilities) {
            if (ability.binding != TriggerBinding.ATTACHED) continue
            val trigger = ability.trigger
            if (trigger !is EventPattern.ZoneChangeEvent) continue
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
            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger !is EventPattern.CreatureDealtDamageBySourceDiesEvent) continue
                if (ability.activeZone != Zone.BATTLEFIELD) continue

                val sourceFilter = trigger.sourceFilter
                val fires = if (sourceFilter == null) {
                    // SELF shape (Soul Collector): the permanent bearing the trigger must itself have
                    // dealt damage to the dying creature this turn.
                    val container = state.getEntity(entry.entityId) ?: continue
                    val damageTracking = container.get<DamageDealtToCreaturesThisTurnComponent>() ?: continue
                    dyingEntityId in damageTracking.creatureIds
                } else {
                    // Observer shape (Shelob): any source that dealt damage to the dying creature this
                    // turn must match the filter, evaluated against its last-known state when it dealt
                    // the damage (CR 608.2h). "you control" resolves to the trigger's controller.
                    matchesDamageSourceFilter(event.lastKnown?.damageSources ?: emptySet(), sourceFilter, entry.controllerId)
                }
                // "another creature": never fire for the source observing its own death.
                if (fires && ability.binding == com.wingedsheep.sdk.scripting.TriggerBinding.ANY &&
                    dyingEntityId == entry.entityId) {
                    continue
                }
                if (!fires) continue

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
     * Evaluate an observer source filter ("a Spider you controlled") against the last-known snapshots
     * of the creatures that damaged the dying creature this turn. Returns true if at least one source
     * matches. Only the controller predicate, required subtype, and creature requirement are
     * evaluated — the snapshot intentionally captures just those facts (the shape used by Shelob and
     * generalizable "dealt damage by [subtype] you control/opponents control" triggers).
     */
    private fun matchesDamageSourceFilter(
        sources: Set<com.wingedsheep.engine.state.components.battlefield.DamageSourceLki>,
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter,
        observerControllerId: com.wingedsheep.sdk.model.EntityId,
    ): Boolean {
        if (sources.isEmpty()) return false
        val requiredSubtype = filter.cardPredicates
            .filterIsInstance<com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype>()
            .map { it.subtype }
        val requiresCreature = filter.cardPredicates.any {
            it is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature
        }
        val controllerPred = filter.controllerPredicate
        return sources.any { src ->
            if (requiresCreature && !src.sourceWasCreature) return@any false
            if (requiredSubtype.isNotEmpty() && !requiredSubtype.all { it in src.sourceSubtypes }) return@any false
            when (controllerPred) {
                null -> true
                is com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByYou ->
                    src.sourceControllerId == observerControllerId
                is com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByOpponent ->
                    src.sourceControllerId != observerControllerId
                else -> true
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
        if (event.lastKnown?.wasToken == true) return
        if ((event.lastKnown?.minusOneMinusOneCounters ?: 0) > 0) return
        if (Keyword.PERSIST.name !in (event.lastKnown?.keywords ?: emptySet())) return

        val info = resolveDyingEntity(state, event) ?: return

        val persistAbility = TriggeredAbility.create(
            trigger = EventPattern.ZoneChangeEvent(
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
     * Detect Enduring triggers (Duskmourn Glimmer cycle) when a permanent dies.
     *
     * Enduring: "When this permanent dies, if it was a creature, return it to the battlefield
     * under its owner's control. It's an enchantment. (It's not a creature.)"
     *
     * Modeled like persist — a synthesized SELF dies-trigger — with two Enduring-specific gates:
     *  - Fires only if the permanent was a creature when it died (CR intervening-if). This stops
     *    the returned enchantment from looping: when the enchantment-only instance later dies it
     *    isn't a creature, so the trigger doesn't fire again.
     *  - Suppressed on tokens (CR 111.7): a token copy ceases to exist via 704.5s before any
     *    return could happen, matching the printed ruling ("a token that's a copy of [it] won't
     *    return").
     *
     * The synthesized effect moves the card from graveyard to the battlefield under its owner's
     * control, then stamps [com.wingedsheep.engine.state.components.battlefield.EnduringReturnComponent]
     * via [com.wingedsheep.sdk.scripting.effects.MarkEnduringReturnEffect]; the card's
     * conditional type-changing static (gated on that marker) makes it enchantment-only.
     *
     * Reads projected keywords captured on the event so both intrinsic Enduring and any future
     * granted Enduring reach it identically.
     */
    fun detectEnduringTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        if (event.fromZone != Zone.BATTLEFIELD || event.toZone != Zone.GRAVEYARD) return
        if (event.lastKnown?.wasToken == true) return
        if (event.lastKnown?.typeLine?.isCreature != true) return
        if (Keyword.ENDURING.name !in (event.lastKnown?.keywords ?: emptySet())) return

        val info = resolveDyingEntity(state, event) ?: return

        val enduringAbility = TriggeredAbility.create(
            trigger = EventPattern.ZoneChangeEvent(
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.SELF,
            effect = CompositeEffect(
                listOf(
                    // No controllerOverride: MoveToZoneEffect returns the card under its owner's
                    // control by default (CR — "under its owner's control"). fromZone guards the
                    // move so a card that already left the graveyard isn't disturbed.
                    MoveToZoneEffect(
                        target = EffectTarget.Self,
                        destination = Zone.BATTLEFIELD,
                        fromZone = Zone.GRAVEYARD
                    ),
                    com.wingedsheep.sdk.scripting.effects.MarkEnduringReturnEffect
                )
            ),
            descriptionOverride =
                "Enduring — Return ${info.name} to the battlefield under its owner's control. " +
                    "It's an enchantment."
        )

        triggers.add(
            PendingTrigger(
                ability = enduringAbility,
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
        // See [detectDeathTriggers]: an entity that lost all its abilities at leaving time
        // contributes no SELF/ANY leaves-battlefield triggers from itself.
        if (event.lastKnown?.lostAllAbilities == true) return

        val entityId = event.entityId
        val info = resolveDyingEntity(state, event) ?: return

        val abilities = abilityResolver.getTriggeredAbilities(entityId, info.cardDefinitionId, state)
        val controllerId = event.ownerId

        for (ability in abilities) {
            val isGenericLeave = matcher.isLeavesBattlefieldTrigger(ability.trigger)
            val isSpecificZoneLeave =
                matcher.isLeavesBattlefieldToZoneTrigger(ability.trigger, event.toZone)
            if (!isGenericLeave && !isSpecificZoneLeave) continue

            when (ability.binding) {
                TriggerBinding.SELF -> {
                    // A SELF leave/exile trigger fires on its own departure, but event-fact
                    // qualifiers on the pattern (e.g. `requireCraftMaterial` — "while you're
                    // activating a craft ability", Market Gnome) still gate it, so an unrelated
                    // exile doesn't fire it. Verify those against the event.
                    val trig = ability.trigger
                    if (trig is EventPattern.ZoneChangeEvent && trig.requireCraftMaterial &&
                        !event.craftMaterial
                    ) {
                        continue
                    }
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
