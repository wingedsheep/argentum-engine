package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.DamageDealtToCreaturesThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityId
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
        statics: BattlefieldStaticsIndex,
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
        val abilities = abilityResolver.getDepartedTriggeredAbilities(event, info.cardDefinitionId, state, statics)
        val controllerId = event.lastKnown?.controllerId ?: event.ownerId

        for (ability in abilities) {
            if (!matcher.isDeathTrigger(ability.trigger)) continue

            when (ability.binding) {
                TriggerBinding.SELF, TriggerBinding.ANY -> {
                    // SELF: "When this creature dies [, if it was enchanted]" — its own death, still
                    // narrowed by a `Triggers.self.matching(...)` filter, answered from the zone
                    // change's last-known snapshot (Gunner Conscript).
                    // ANY: "Whenever a creature [matching filter] dies" - check if its own death matches
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
     * Rule 603.10a: "look back in time" for permanents that leave the battlefield together.
     * When several permanents leave at once (a wipe, a mass exile, or one dying while another is
     * exiled), each departed permanent's leaves-the-battlefield abilities still see the others
     * leaving. The main battlefield loop misses these because the sources are already gone.
     *
     * Checks departed permanents' non-self triggers (OTHER/ANY binding) against the batch's other
     * battlefield departures. Death-to-death pairs match any trigger, as they always have; a pair
     * where either side left for a zone other than the graveyard only fires abilities that are
     * themselves leaves-the-battlefield abilities ([TriggerMatcher.battlefieldExitAtoms]), the
     * class CR 603.10a names — Slagstone Refinery exiled alongside another artifact still sees it.
     */
    fun detectSimultaneousDepartureTriggers(
        state: GameState,
        statics: BattlefieldStaticsIndex,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        val departures = events.filterIsInstance<ZoneChangeEvent>().filter { it.isBattlefieldDeparture }
        if (departures.size < 2) return // Need at least 2 simultaneous departures

        for (departedEvent in departures) {
            val departedId = departedEvent.entityId

            // Skip if this permanent is still on the battlefield (already handled by main loop)
            if (departedId in state.getBattlefield()) continue

            // Skip permanents whose abilities were stripped at leaving time (Xu-Ifit's
            // ability-stripped reanimate target contributes no triggers of its own even
            // when it dies alongside other creatures).
            if (departedEvent.lastKnown?.lostAllAbilities == true) continue

            val info = resolveDyingEntity(state, departedEvent) ?: continue

            val abilities = abilityResolver.getDepartedTriggeredAbilities(departedEvent, info.cardDefinitionId, state, statics)
            val controllerId = departedEvent.lastKnown?.controllerId ?: departedEvent.ownerId

            for (ability in abilities) {
                val isLeaveAbility = matcher.battlefieldExitAtoms(ability.trigger).isNotEmpty()
                for (otherEvent in departures) {
                    if (otherEvent.entityId == departedId) continue // Skip self
                    val bothDied = departedEvent.toZone == Zone.GRAVEYARD && otherEvent.toZone == Zone.GRAVEYARD
                    if (!bothDied && !isLeaveAbility) continue

                    if (matcher.matchesTrigger(ability.trigger, ability.binding, otherEvent, departedId, controllerId, state)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = departedId,
                                sourceName = info.name,
                                controllerId = controllerId,
                                triggerContext = TriggerContext.fromEvent(otherEvent)
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Rule 603.10a look-back for "whenever a creature dealt damage by this creature this turn dies"
     * (SELF shape: Soul Collector, Dread Slaver) when the damaging creature died in the same event as
     * its victim — the combat where they trade, or a source that damaged itself (Dread Slaver ruling,
     * 2012-05-01). [detectCreatureDealtDamageBySourceDiesTriggers] only walks live battlefield
     * trackers, and the source's own damage tracker is stripped as it leaves, so the damaging source
     * is read off the *victim's* last-known damage snapshot ([DamageSourceLki.sourceId]) instead.
     */
    fun detectDepartedDamagingSourceDiesTriggers(
        state: GameState,
        statics: BattlefieldStaticsIndex,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>
    ) {
        val deathEvents = events.filterIsInstance<ZoneChangeEvent>().filter {
            it.toZone == Zone.GRAVEYARD && it.isBattlefieldDeparture
        }
        if (deathEvents.isEmpty()) return

        for (sourceEvent in deathEvents) {
            val sourceId = sourceEvent.entityId
            // Still on the battlefield → the live tracker path already saw it.
            if (sourceId in state.getBattlefield()) continue
            if (sourceEvent.lastKnown?.lostAllAbilities == true) continue

            val info = resolveDyingEntity(state, sourceEvent) ?: continue
            val abilities = abilityResolver.getDepartedTriggeredAbilities(sourceEvent, info.cardDefinitionId, state, statics)
                .filter {
                    val trigger = it.trigger
                    trigger is EventPattern.CreatureDealtDamageBySourceDiesEvent &&
                        trigger.sourceFilter == null &&
                        it.binding == TriggerBinding.SELF &&
                        Zone.BATTLEFIELD in it.activeZones
                }
            if (abilities.isEmpty()) continue
            val controllerId = sourceEvent.lastKnown?.controllerId ?: sourceEvent.ownerId

            for (victimEvent in deathEvents) {
                val damagedBySource = victimEvent.lastKnown?.damageSources?.any { it.sourceId == sourceId } == true
                if (!damagedBySource) continue
                for (ability in abilities) {
                    val dyingFilter = (ability.trigger as EventPattern.CreatureDealtDamageBySourceDiesEvent).dyingFilter
                    if (dyingFilter != null && !matcher.matchesZoneChangeTrigger(
                            EventPattern.ZoneChangeEvent(filter = dyingFilter, from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD),
                            TriggerBinding.ANY,
                            victimEvent,
                            sourceId,
                            controllerId,
                            state
                        )
                    ) continue
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = sourceId,
                            sourceName = info.name,
                            controllerId = controllerId,
                            triggerContext = TriggerContext.fromEvent(victimEvent)
                        )
                    )
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
        statics: BattlefieldStaticsIndex,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        // An aura whose abilities were stripped at leaving time (e.g. via a Xu-Ifit-shaped
        // reanimate on an aura, or Humility-style suppression in effect at LTB) contributes
        // no attached zone-change triggers.
        if (event.lastKnown?.lostAllAbilities == true) return

        val attachedEntityId = event.lastKnown?.attachedTo ?: return

        val auraEntityId = event.entityId
        val info = resolveDyingEntity(state, event) ?: return
        val abilities = abilityResolver.getDepartedTriggeredAbilities(event, info.cardDefinitionId, state, statics)
        val controllerId = event.lastKnown?.controllerId ?: event.ownerId

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
                    sourceName = info.name,
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
                if (Zone.BATTLEFIELD !in ability.activeZones) continue

                val sourceFilter = trigger.sourceFilter
                val fires = if (sourceFilter == null) {
                    // SELF shape (Soul Collector): the permanent bearing the trigger must itself have
                    // dealt damage to the dying creature this turn.
                    //
                    // ATTACHED shape (Scythe of the Wretched): the *equipped* creature is the damaging
                    // source, so the tracker to read hangs off the attachment target rather than off the
                    // Equipment. Resolved here, at detection time — which is when the creature dies, and
                    // the Scythe's own ruling says the Equipment must be attached *then*, not when the
                    // damage was dealt. An unattached Equipment simply never fires.
                    val damagingSourceId = if (ability.binding == TriggerBinding.ATTACHED) {
                        state.getEntity(entry.entityId)?.get<AttachedToComponent>()?.targetId ?: continue
                    } else {
                        entry.entityId
                    }
                    val container = state.getEntity(damagingSourceId) ?: continue
                    val damageTracking = container.get<DamageDealtToCreaturesThisTurnComponent>() ?: continue
                    dyingEntityId in damageTracking.creatureIds
                } else {
                    // Observer shape (Shelob): any source that dealt damage to the dying creature this
                    // turn must match the filter, evaluated against its last-known state when it dealt
                    // the damage (CR 608.2h). "you control" resolves to the trigger's controller.
                    (event.lastKnown?.damageSources ?: emptySet()).any { it.matches(sourceFilter, entry.controllerId) }
                }
                // "another creature": never fire for the source observing its own death.
                if (fires && ability.binding == com.wingedsheep.sdk.scripting.TriggerBinding.ANY &&
                    dyingEntityId == entry.entityId) {
                    continue
                }
                if (!fires) continue

                // "a creature **with flying** dealt damage by ~ this turn dies" (Trophy Hunter).
                // Delegated to the zone-change matcher rather than re-evaluated here: the dying
                // creature is already in the graveyard, so every characteristic must be read off
                // the LKI snapshot (CR 608.2h), and that matcher is where the LKI-aware predicate
                // paths — keywords, projected type line, tokens swept by 704.5d — already live.
                // Trophy Hunter's ruling turns on exactly that: the check is whether the creature
                // had flying as it left, not what its printed card says.
                val dyingFilter = trigger.dyingFilter
                if (dyingFilter != null && !matcher.matchesZoneChangeTrigger(
                        EventPattern.ZoneChangeEvent(
                            filter = dyingFilter,
                            from = Zone.BATTLEFIELD,
                            to = Zone.GRAVEYARD
                        ),
                        TriggerBinding.ANY,
                        event,
                        entry.entityId,
                        entry.controllerId,
                        state
                    )
                ) continue

                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = entry.entityId,
                        sourceName = entry.cardComponent.name,
                        controllerId = entry.controllerId,
                        triggerContext = TriggerContext.fromEvent(event)
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
        if (!event.isBattlefieldDeparture || event.toZone != Zone.GRAVEYARD) return
        if (event.lastKnown?.wasToken == true) return
        if ((event.lastKnown?.minusOneMinusOneCounters ?: 0) > 0) return
        if (Keyword.PERSIST.name !in (event.lastKnown?.keywords ?: emptySet())) return

        val info = resolveDyingEntity(state, event) ?: return

        val persistAbility = TriggeredAbility.create(
            id = AbilityId("persist"),
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
                        counterType = CounterType.MINUS_ONE_MINUS_ONE,
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
     * Detect undying triggers (CR 702.93) when a permanent dies with no +1/+1 counters on it.
     *
     * The keyword is read from the event's projected last-known information, so printed and granted
     * instances follow the same path. The synthesized ability resolves from the graveyard and returns
     * the card under its owner's control with a +1/+1 counter. Tokens are not suppressed here: undying
     * still triggers for a token, but the token ceases to exist before that trigger can return it.
     */
    fun detectUndyingTriggers(
        state: GameState,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        if (!event.isBattlefieldDeparture || event.toZone != Zone.GRAVEYARD) return
        if ((event.lastKnown?.plusOnePlusOneCounters ?: 0) > 0) return
        if (Keyword.UNDYING.name !in (event.lastKnown?.keywords ?: emptySet())) return

        val info = resolveDyingEntity(state, event) ?: return

        val undyingAbility = TriggeredAbility.create(
            id = AbilityId("undying"),
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
                        counterType = CounterType.PLUS_ONE_PLUS_ONE,
                        count = 1,
                        target = EffectTarget.Self
                    )
                )
            ),
            descriptionOverride = "Undying — Return ${info.name} to the battlefield with a +1/+1 counter on it"
        )

        triggers.add(
            PendingTrigger(
                ability = undyingAbility,
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
        if (!event.isBattlefieldDeparture || event.toZone != Zone.GRAVEYARD) return
        if (event.lastKnown?.wasToken == true) return
        if (event.lastKnown?.typeLine?.isCreature != true) return
        if (Keyword.ENDURING.name !in event.lastKnown.keywords) return

        val info = resolveDyingEntity(state, event) ?: return

        val enduringAbility = TriggeredAbility.create(
            id = AbilityId("enduring"),
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
     * SELF and ANY bindings fire if the leaving entity itself matches the trigger's pattern and
     * filter against last-known information (e.g., Three Tree Scribe's "Whenever this creature or another creature
     * you control leaves the battlefield without dying" must fire when the Scribe itself
     * leaves — the main battlefield loop misses this because the source is no longer on the
     * battlefield by the time the event is processed).
     */
    fun detectLeavesBattlefieldTriggers(
        state: GameState,
        statics: BattlefieldStaticsIndex,
        event: ZoneChangeEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        // See [detectDeathTriggers]: an entity that lost all its abilities at leaving time
        // contributes no SELF/ANY leaves-battlefield triggers from itself.
        if (event.lastKnown?.lostAllAbilities == true) return

        val entityId = event.entityId
        val info = resolveDyingEntity(state, event) ?: return

        val abilities = abilityResolver.getDepartedTriggeredAbilities(event, info.cardDefinitionId, state, statics)
        val controllerId = event.lastKnown?.controllerId ?: event.ownerId

        for (ability in abilities) {
            val isGenericLeave = matcher.isLeavesBattlefieldTrigger(ability.trigger)
            val isSpecificZoneLeave =
                matcher.isLeavesBattlefieldToZoneTrigger(ability.trigger, event.toZone)
            if (!isGenericLeave && !isSpecificZoneLeave) continue
            // A disjunction naming both a dies atom and a generic leaves atom
            // (`Triggers.or(self.dies(), self.leaves())`) is owned by [detectDeathTriggers] when the
            // permanent went to the graveyard; firing it here too would trigger it twice for one event.
            if (event.toZone == Zone.GRAVEYARD && matcher.isDeathTrigger(ability.trigger)) continue

            when (ability.binding) {
                TriggerBinding.SELF, TriggerBinding.ANY -> {
                    // SELF: its own departure, still gated by the pattern's event-fact qualifiers
                    // (`requireCraftMaterial` — Market Gnome) and any `Triggers.self.matching(...)`
                    // filter, read from last-known information. Under a disjunction, any atom this
                    // departure satisfies fires it.
                    // ANY: "Whenever [this creature or] another creature you control leaves..." —
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
