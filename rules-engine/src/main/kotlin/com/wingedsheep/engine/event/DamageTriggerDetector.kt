package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameEvent
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

        // Face-down creatures have no abilities (Rule 707.2)
        // Check both current state AND the event's recorded face-down status, because
        // FaceDownComponent may have been stripped by stripBattlefieldComponents when
        // the creature died via SBAs before trigger detection runs.
        if (container.has<FaceDownComponent>() || event.targetWasFaceDown) return

        val abilities = abilityResolver.getTriggeredAbilities(entityId, cardComponent.cardDefinitionId, state)

        for (ability in abilities) {
            val trigger = ability.trigger
            // Only match generic (source=Any) DamageReceivedEvent triggers here.
            // Source-filtered triggers (DamagedByCreature, DamagedBySpell) are handled
            // exclusively by detectDamagedBySourceTriggers to avoid firing with a wrong
            // triggeringEntityId (fromEvent uses targetId, not sourceId).
            if (trigger is GameEvent.DamageReceivedEvent &&
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

        // Face-down creatures have no abilities (Rule 707.2)
        if (container.has<FaceDownComponent>()) return

        val abilities = abilityResolver.getTriggeredAbilities(sourceId, cardComponent.cardDefinitionId, state)

        for (ability in abilities) {
            val trigger = ability.trigger
            if (trigger is GameEvent.DealsDamageEvent && ability.binding == TriggerBinding.SELF) {
                if (matcher.matchesDealsDamageTrigger(trigger, event, state)) {
                    triggers.add(
                        PendingTrigger(
                            ability = ability,
                            sourceId = sourceId,
                            sourceName = cardComponent.name,
                            controllerId = controllerId,
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

        // Face-down creatures have no abilities (Rule 707.2)
        if (container.has<FaceDownComponent>() || event.targetWasFaceDown) return

        val abilities = abilityResolver.getTriggeredAbilities(damagedEntityId, cardComponent.cardDefinitionId, state)

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
                trigger is GameEvent.DamageReceivedEvent && ability.binding == TriggerBinding.SELF &&
                    trigger.source == SourceFilter.Creature && isCreatureSource -> true
                trigger is GameEvent.DamageReceivedEvent && ability.binding == TriggerBinding.SELF &&
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
     * Detect "whenever a creature deals damage to you" triggers on permanents
     * controlled by the damaged player. Uses pre-indexed damage-to-you observers
     * instead of scanning all battlefield permanents.
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

        // Verify the damage source is a creature on the battlefield
        val sourceContainer = state.getEntity(damageSourceId) ?: return
        val sourceCard = sourceContainer.get<CardComponent>() ?: return
        if (!sourceCard.typeLine.isCreature) return

        for (entry in index.damageToYouObservers) {
            // Only triggers on permanents controlled by the damaged player
            if (entry.controllerId != damagedPlayerId) continue

            for (ability in entry.abilities) {
                val trigger = ability.trigger
                if (trigger is GameEvent.DealsDamageEvent &&
                    trigger.recipient == RecipientFilter.You &&
                    trigger.sourceFilter == null &&
                    ability.binding == TriggerBinding.ANY) {
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
                if (trigger is GameEvent.DealsDamageEvent &&
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
