package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

/**
 * Handles all damage-related triggers.
 */
class DamageTriggerDetector(
    private val abilityResolver: TriggerAbilityResolver,
    private val matcher: TriggerMatcher
) {

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
            // exclusively by detectDamagedBySourceTriggers to avoid firing with a wrong
            // triggeringEntityId (fromEvent uses targetId, not sourceId).
            if (trigger is EventPattern.DamageReceivedEvent &&
                ability.binding == TriggerBinding.SELF &&
                trigger.source == SourceFilter.Any
            ) {
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
                // Pass the ability's controller so RecipientFilter.Matching can evaluate
                // controller-relative recipient filters (e.g. "a creature an opponent controls").
                if (matcher.matchesDealsDamageTrigger(trigger, event, state, controllerId)) {
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
     * Detect "whenever a creature/spell deals damage to this" triggers.
     * For DamageReceivedEvent(source=Creature): source must be a creature on the battlefield.
     * For DamageReceivedEvent(source=Spell): source must be an instant or sorcery.
     * TriggeringEntityId is set to the damage SOURCE for retaliation effects.
     *
     * Handles both on-battlefield and off-battlefield cases (e.g., creature
     * dies from lethal damage but trigger still fires per Rule 603.10).
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

        val abilities = abilityResolver.getTriggeredAbilities(damagedEntityId, cardComponent.cardDefinitionId, state, statics)

        // Determine source type
        val sourceContainer = state.getEntity(sourceId) ?: return
        val sourceCard = sourceContainer.get<CardComponent>()
        // Do NOT require the source to still be on the battlefield: combat damage is dealt
        // simultaneously, so the attacker may have died from Tephraderm's damage in the same
        // combat step (Rule 603.10 look-back). We check the card's type line instead of
        // current zone to determine what it was when it dealt the damage.
        val isCreatureSource = sourceCard?.typeLine?.isCreature == true
        val isSpellSource = sourceCard != null && (sourceCard.typeLine.isInstant || sourceCard.typeLine.isSorcery)

        for (ability in abilities) {
            val trigger = ability.trigger
            val matches = when {
                trigger is EventPattern.DamageReceivedEvent && ability.binding == TriggerBinding.SELF &&
                    trigger.source == SourceFilter.Creature && isCreatureSource -> true
                trigger is EventPattern.DamageReceivedEvent && ability.binding == TriggerBinding.SELF &&
                    trigger.source == SourceFilter.Spell && isSpellSource -> true
                else -> false
            }

            if (matches) {
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
                    trigger.recipient == RecipientFilter.You &&
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
        if (!matcher.matchesDealsDamageTrigger(trigger, event, state, controllerId)) return
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

                val firstMatching = damageEvents.firstOrNull { event ->
                    matcher.matchesDealsDamageTrigger(trigger, event, state, entry.controllerId)
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
                    trigger.recipient == RecipientFilter.AnyPlayer &&
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
