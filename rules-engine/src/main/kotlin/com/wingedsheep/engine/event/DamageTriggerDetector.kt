package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Handles all damage-related triggers.
 */
class DamageTriggerDetector(
    private val abilityResolver: TriggerAbilityResolver,
    private val matcher: TriggerMatcher,
    private val predicateEvaluator: PredicateEvaluator
) {


    companion object {
        /**
         * Whether [ability] is the SELF-bound "whenever a source deals damage to this creature"
         * shape ([GameObjectFilter.Any]) — the one whose triggering entity is the **damage source**
         * rather than the creature that was dealt the damage.
         *
         * "That source's controller mills that many cards" (Belltower Sphinx) has nothing to name
         * otherwise: the damaged creature is the trigger's own `sourceId`, and its controller is
         * already `controllerId`, so binding it carried no information. This matches what the
         * source-filtered variants have always done (`detectDamagedBySourceTriggers`) and what
         * `TriggerContext.fromEvent` does for `DamagePreventedEvent`.
         *
         * Shared because this trigger is detected in **two** places — the main battlefield scan in
         * `TriggerDetector` while the creature is still alive, and
         * [detectDamageReceivedTriggers] once it has died to that same damage. They must agree, or
         * a card would behave differently depending on whether the damage happened to be lethal.
         */
        fun bindsDamageSource(ability: TriggeredAbility): Boolean {
            val trigger = ability.trigger
            return ability.binding == TriggerBinding.SELF &&
                trigger is EventPattern.DamageReceivedEvent &&
                trigger.source == GameObjectFilter.Any
        }

        /** The trigger context for [bindsDamageSource] abilities, built off the damage event. */
        fun damageReceivedContext(event: DamageDealtEvent): TriggerContext = TriggerContext(
            triggeringEntityId = event.sourceId,
            damageAmount = event.amount,
            excessDamageAmount = event.excessAmount.takeIf { it > 0 },
            recipientToughnessAtDamage = event.targetToughnessAtDamage
        )
    }

    /**
     * Detect "whenever this creature is dealt damage" triggers on creatures that
     * are no longer on the battlefield (e.g., died from the damage via SBAs).
     * Similar to detectDeathTriggers pattern.
     */
    fun detectDamageReceivedTriggers(
        state: GameState,
        statics: BattlefieldStaticsIndex,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val entityId = event.targetId
        val container = state.getEntity(entityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return
        // ControllerComponent is stripped when creature dies via SBAs, fall back to ownerId
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return

        // Face-down creatures have no abilities (Rule 708.2)
        // Check both current state AND the event's recorded face-down status, because
        // FaceDownComponent may have been stripped by stripBattlefieldComponents when
        // the creature died via SBAs before trigger detection runs.
        if (container.has<FaceDownComponent>() || event.targetWasFaceDown) return

        val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state, statics)

        for (ability in abilities) {
            val trigger = ability.trigger
            // Only match generic (source=Any) DamageReceivedEvent triggers here.
            // Source-filtered triggers (DamagedByCreature, DamagedBySpell) are handled
            // exclusively by detectDamagedBySourceTriggers.
            if (trigger is EventPattern.DamageReceivedEvent && bindsDamageSource(ability)) {
                triggers.add(
                    PendingTrigger(
                        ability = ability,
                        sourceId = entityId,
                        sourceName = cardComponent.name,
                        controllerId = controllerId,
                        // Binds the damage *source*, not the creature that was dealt the damage —
                        // see [bindsDamageSource]. The main battlefield scan in TriggerDetector
                        // applies the same rule for the case where the creature survived.
                        triggerContext = damageReceivedContext(event)
                    )
                )
            }
        }
    }

    fun detectDamageSourceTriggers(
        state: GameState,
        statics: BattlefieldStaticsIndex,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState
    ) {
        val sourceId = event.sourceId ?: return
        val container = state.getEntity(sourceId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return
        // Fall back to ownerId if ControllerComponent was stripped (e.g., creature died to SBA
        // during combat damage, but its damage trigger should still fire per Rule 603.10)
        val controllerId = projected.getController(sourceId)
            ?: container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId ?: return

        // Face-down creatures have no abilities (Rule 708.2)
        if (container.has<FaceDownComponent>()) return

        val abilities = abilityResolver.getTriggeredAbilities(sourceId, cardComponent.cardDefinitionId, state, statics)

        for (ability in abilities) {
            val trigger = ability.trigger
            if (trigger is EventPattern.DealsDamageEvent && ability.binding == TriggerBinding.SELF) {
                // Pass the ability's controller and source so the recipient's relative readings
                // ("a creature an opponent controls", "enchanted player") resolve against them.
                if (matcher.matchesDealsDamageTrigger(trigger, event, state, controllerId, sourceId)) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = sourceId,
                            sourceName = cardComponent.name,
                            controllerId = controllerId,
                            // fromEvent already sets triggeringEntityId = event.targetId (the damaged
                            // player for a deals-damage-to-a-player trigger), and both
                            // Player.TriggeringPlayer and ControllerPredicate.ControlledByTriggeringPlayer
                            // resolve as `triggeringPlayerId ?: triggeringEntityId`, so "…to a player,
                            // destroy target artifact that player controls" (Dreadmaw's Ire) resolves to
                            // the damaged player without any extra triggeringPlayerId copy here.
                            triggerContext = TriggerContext.fromEvent(event)
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect source-filtered "whenever [a source matching X] deals damage to this" triggers
     * (Tephraderm: "a creature", "a spell"). The triggering entity is the damage SOURCE, for
     * retaliation effects.
     *
     * Neither end has to still be on the battlefield: the damaged permanent may have died to the
     * damage, and combat damage is dealt simultaneously, so the attacker may have died to the same
     * exchange (CR 603.10). The source filter is evaluated against the source as it is now —
     * projected characteristics while it is still a permanent, its card's own once it has left.
     */
    fun detectDamagedBySourceTriggers(
        state: GameState,
        statics: BattlefieldStaticsIndex,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>
    ) {
        val sourceId = event.sourceId ?: return
        val damagedEntityId = event.targetId

        // Get the damaged entity (might be on battlefield or in graveyard)
        val container = state.getEntity(damagedEntityId) ?: return
        val cardComponent = container.get<CardComponent>() ?: return
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId ?: return

        // Face-down creatures have no abilities (Rule 708.2)
        if (container.has<FaceDownComponent>() || event.targetWasFaceDown) return
        if (state.getEntity(sourceId) == null) return

        val abilities = abilityResolver.getTriggeredAbilities(damagedEntityId, cardComponent.cardDefinitionId, state, statics)
        val context = PredicateContext(controllerId = controllerId, sourceId = damagedEntityId)

        for (ability in abilities) {
            val trigger = ability.trigger
            if (trigger !is EventPattern.DamageReceivedEvent || ability.binding != TriggerBinding.SELF) continue
            if (trigger.source == GameObjectFilter.Any) continue
            if (!predicateEvaluator.matches(state, state.projectedState, sourceId, trigger.source, context)) continue
            triggers.add(
                PendingTrigger(
                    ability = ability,
                    sourceId = damagedEntityId,
                    sourceName = cardComponent.name,
                    controllerId = controllerId,
                    triggerContext = TriggerContext(
                        triggeringEntityId = sourceId,
                        damageAmount = event.amount
                    )
                )
            )
        }
    }

    /**
     * Detect "whenever [a source matching X] deals damage to you" triggers on permanents
     * controlled by the damaged player. Uses pre-indexed damage-to-you observers
     * instead of scanning all battlefield permanents.
     *
     * *What* may deal the damage comes from the trigger's own `sourceFilter`, not from a hardcoded
     * type check here: `GameObjectFilter.Creature` for Aurification's "whenever a creature deals
     * damage to you", `Any.opponentControls()` for Farsight Mask's "a source an opponent controls",
     * and null for Sun Droplet's source-blind "whenever you're dealt damage" — which must fire for
     * a burn spell or an artifact just as it does for a creature.
     */
    fun detectDamageToControllerTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState,
        index: TriggerIndex
    ) {
        val damageSourceId = event.sourceId ?: return
        val damagedPlayerId = event.targetId

        for (entry in index.damageToYouObservers) {
            // Only triggers on permanents controlled by the damaged player
            if (entry.controllerId != damagedPlayerId) continue

            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger is EventPattern.DealsDamageEvent &&
                    trigger.recipient == Recipient.You &&
                    ability.binding == TriggerBinding.ANY &&
                    matchesDamageType(trigger.damageType, event) &&
                    matcher.matchesDamageSourceFilter(
                        trigger.sourceFilter, event, state, entry.controllerId
                    )) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = entry.controllerId,
                            triggerContext = TriggerContext(
                                triggeringEntityId = damageSourceId,
                                damageAmount = event.amount
                            )
                        )
                    )
                }
            }
        }
    }

    /** Combat/noncombat gate for a [EventPattern.DealsDamageEvent]; [DamageType.Any] matches both. */
    private fun matchesDamageType(damageType: DamageType, event: DamageDealtEvent): Boolean =
        damageType == DamageType.Any ||
            (damageType == DamageType.Combat && event.isCombatDamage) ||
            (damageType == DamageType.NonCombat && !event.isCombatDamage)

    /**
     * Detect general damage observer triggers (DealsDamageEvent with ANY binding)
     * that aren't handled by the specialized detectDamageToControllerTriggers or
     * detectSubtypeDamageToPlayerTriggers methods.
     * E.g., Kazarov: "Whenever a creature an opponent controls is dealt damage"
     */
    fun detectDamageObserverTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        for (entry in index.damageObservers) {
            for (ability in entry.abilities) {
                if (!isGeneralDamageObserver(ability)) continue
                matchDamageObserver(
                    state = state,
                    event = event,
                    triggers = triggers,
                    ability = ability,
                    sourceId = entry.entityId,
                    sourceName = entry.cardComponent.name,
                    controllerId = entry.controllerId
                )
            }
        }

        // Global granted abilities are attached to no permanent, so they are absent from every
        // battlefield index — and the generic TriggerMatcher deliberately returns false for
        // DealsDamageEvent (all damage patterns route here). Without this pass a floating
        // "whenever a creature you control deals combat damage to a player" ability (Mistway Spy's
        // turned-face-up payoff) would never fire. They are few and only live for their duration,
        // so the extra walk costs nothing on a board without one.
        for (global in state.globalGrantedTriggeredAbilities) {
            matchDamageObserver(
                state = state,
                event = event,
                triggers = triggers,
                ability = global.ability,
                sourceId = global.sourceId,
                sourceName = global.sourceName,
                controllerId = global.controllerId
            )
        }
    }

    /**
     * Match one ANY-bound [EventPattern.DealsDamageEvent] observer against [event] and queue it.
     * Shared by the indexed battlefield observers and the global granted abilities, which differ
     * only in where their identity comes from.
     */
    private fun matchDamageObserver(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>,
        ability: com.wingedsheep.sdk.scripting.TriggeredAbility,
        sourceId: com.wingedsheep.sdk.model.EntityId,
        sourceName: String,
        controllerId: com.wingedsheep.sdk.model.EntityId
    ) {
        val trigger = ability.trigger
        if (trigger !is EventPattern.DealsDamageEvent || ability.binding != TriggerBinding.ANY) return
        // Batch ("one or more") observers fire once per event batch, not once per
        // damage event — handled by detectDamageObserverBatchTriggers.
        if (trigger.batch) return
        if (!matcher.matchesDealsDamageTrigger(trigger, event, state, controllerId, sourceId)) return
        // When the trigger has a sourceFilter (e.g., "creature you control deals
        // combat damage"), the triggering entity is the damage SOURCE (the creature),
        // not the damage recipient. This allows effects like "exile it" to reference
        // the creature that dealt damage.
        val context = if (trigger.sourceFilter != null && event.sourceId != null) {
            // The triggering entity is the damage SOURCE (e.g. "a source you
            // control deals damage… exile it"). Still carry the recipient creature's
            // toughness so "equal to that creature's toughness" payoffs (Taii Wakeen)
            // can read it via ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS. When the
            // recipient is a player, also carry it as the triggering player so
            // "…to a player, [that player] …" payoffs (Fear of Burning Alive's
            // "target creature that player controls") resolve Player.TriggeringPlayer
            // to the damaged player rather than the source.
            TriggerContext(
                triggeringEntityId = event.sourceId,
                triggeringPlayerId = event.targetId.takeIf { it in state.turnOrder },
                damageAmount = event.amount,
                recipientToughnessAtDamage = event.targetToughnessAtDamage
            )
        } else {
            TriggerContext.fromEvent(event)
        }
        triggers.add(
            PendingTrigger(
                ability = ability,
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                triggerContext = context
            )
        )
    }

    /**
     * Detect batch ("one or more") damage observer triggers — `DealsDamageEvent(batch = true)`
     * with ANY binding, e.g. Magmatic Galleon's "Whenever one or more creatures your opponents
     * control are dealt excess noncombat damage, create a Treasure token."
     *
     * Runs once over the whole event batch (CR 603.2c: an ability triggers only once each time
     * its trigger event occurs): a sweeper dealing excess damage to several matching creatures
     * simultaneously fires the trigger once, not once per creature — the over-counting the
     * per-event [detectDamageObserverTriggers] path would produce. Each observer's filters
     * (damageType / recipient / sourceFilter / requireExcess) are evaluated per damage event via
     * the canonical [TriggerMatcher.matchesDealsDamageTrigger]; one matching event suffices.
     *
     * `triggeringEntityId` is the first matching recipient — batch triggers don't dispatch per
     * recipient, so cards needing per-recipient context use the singular (non-batch) trigger.
     */
    fun detectDamageObserverBatchTriggers(
        state: GameState,
        events: List<EngineGameEvent>,
        triggers: MutableList<PendingTrigger>,
        index: TriggerIndex
    ) {
        val damageEvents = events.filterIsInstance<DamageDealtEvent>()
        if (damageEvents.isEmpty()) return

        for (entry in index.damageObservers) {
            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger !is EventPattern.DealsDamageEvent || !trigger.batch) continue
                if (ability.binding != TriggerBinding.ANY) continue
                if (!isGeneralDamageObserver(ability)) continue

                val firstMatching = damageEvents.firstOrNull { event ->
                    matcher.matchesDealsDamageTrigger(trigger, event, state, entry.controllerId, entry.entityId)
                }
                if (firstMatching != null) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = entry.entityId,
                            sourceName = entry.cardComponent.name,
                            controllerId = entry.controllerId,
                            triggerContext = TriggerContext.fromEvent(firstMatching)
                        )
                    )
                }
            }
        }
    }

    /**
     * Detect "whenever a [subtype] deals combat damage to a player" triggers.
     * Uses pre-indexed subtype damage observers instead of scanning all battlefield permanents.
     */
    fun detectSubtypeDamageToPlayerTriggers(
        state: GameState,
        event: DamageDealtEvent,
        triggers: MutableList<PendingTrigger>,
        projected: ProjectedState,
        index: TriggerIndex
    ) {
        val damageSourceId = event.sourceId ?: return
        val damagedPlayerId = event.targetId

        // Verify the damage source is a creature (face-down creatures have no subtypes)
        val sourceContainer = state.getEntity(damageSourceId) ?: return
        val sourceCard = sourceContainer.get<CardComponent>() ?: return
        if (!sourceCard.typeLine.isCreature) return
        if (sourceContainer.has<FaceDownComponent>()) return

        for (entry in index.subtypeDamageObservers) {
            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger is EventPattern.DealsDamageEvent &&
                    trigger.damageType == DamageType.Combat &&
                    trigger.recipient == Recipient.AnyPlayer &&
                    trigger.sourceFilter != null) {
                    // Check if the sourceFilter has a subtype requirement
                    val filter = trigger.sourceFilter
                    val subtypeValue = if (filter is GameObjectFilter) matcher.extractSubtypeFromFilter(filter) else null
                    if (subtypeValue != null && projected.hasSubtype(damageSourceId, subtypeValue)) {
                        triggers.add(
                            PendingTrigger(
                                ability = ability,
                                sourceId = entry.entityId,
                                sourceName = entry.cardComponent.name,
                                controllerId = entry.controllerId,
                                triggerContext = TriggerContext(
                                    triggeringEntityId = damagedPlayerId,
                                    damageAmount = event.amount
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

/** Which detector owns an ANY-bound [EventPattern.DealsDamageEvent] observer. */
internal enum class DamageObserverBucket { ToYou, SubtypeToPlayer, General }

/**
 * Every "… deals damage to you" observer goes to the damage-to-you bucket, with or without a
 * sourceFilter, and only there: that path binds the damage *source* as the triggering entity
 * ("…exile it", Farsight Mask), and routing it to the general observers as well would fire it twice.
 */
internal fun damageObserverBucket(trigger: EventPattern.DealsDamageEvent): DamageObserverBucket {
    if (trigger.recipient == Recipient.You) return DamageObserverBucket.ToYou
    val filter = trigger.sourceFilter
    val subtypeCombatToPlayer = trigger.damageType == DamageType.Combat &&
        trigger.recipient == Recipient.AnyPlayer &&
        filter is GameObjectFilter &&
        filter.cardPredicates.any { it is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype }
    return if (subtypeCombatToPlayer) DamageObserverBucket.SubtypeToPlayer else DamageObserverBucket.General
}

/**
 * A permanent is filed under each bucket it has an observer for, so the general walk must skip the
 * abilities that belong to the You / subtype buckets or they would fire once per bucket.
 */
private fun isGeneralDamageObserver(ability: TriggeredAbility): Boolean {
    val trigger = ability.trigger
    return trigger is EventPattern.DealsDamageEvent && damageObserverBucket(trigger) == DamageObserverBucket.General
}
