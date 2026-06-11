package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.AbilityTriggeredEvent
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * Context information about what caused a trigger.
 */
@kotlinx.serialization.Serializable
data class TriggerContext(
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null,
    val damageAmount: Int? = null,
    val step: Step? = null,
    val xValue: Int? = null,
    /** Last known +1/+1 counter count when the source left the battlefield */
    val counterCount: Int? = null,
    /** Last known total counter count (all types) when the source left the battlefield */
    val totalCounterCount: Int? = null,
    /** Last known -1/-1 counter count when the source left the battlefield */
    val minusOneMinusOneCounterCount: Int? = null,
    /** The spell or ability entity that targeted a permanent (for ward triggers) */
    val targetingSourceEntityId: EntityId? = null,
    /** Last known power when the triggering entity left the battlefield (for dies/leaves triggers) */
    val lastKnownPower: Int? = null,
    /** Last known toughness when the triggering entity left the battlefield (for dies/leaves triggers) */
    val lastKnownToughness: Int? = null,
    /**
     * Last-known counter map (counter-type-string → count) when the triggering source left
     * the battlefield. Used by triggers that move every counter onto another permanent
     * (e.g., Essence Channeler's "put its counters on target creature you control").
     * Null when the trigger's source never left the battlefield (or had no counters).
     */
    val lastKnownCounters: Map<String, Int>? = null,
    /**
     * Per-player damage dealt to the triggering source this turn, captured at LTB time.
     * Read by LTB effects like Grothama's "each player draws X cards where X is the damage
     * dealt to ~ this turn by sources they controlled."
     */
    val lastKnownDamageDealtByPlayers: Map<EntityId, Int>? = null,
    /**
     * Creatures that were blocking, or blocked by, the triggering source when it left the
     * battlefield (CR 509 combat pairing), captured as last-known information. Read by
     * "destroy all creatures blocking or blocked by it" (Abu Ja'far). Null when the trigger's
     * source never left combat.
     */
    val lastKnownBlockingOrBlockedByIds: List<EntityId>? = null,
    /**
     * For SpellCastEvent triggers — number of mode picks the cast spell recorded. `null`
     * when the trigger was not driven by a spell cast. Read by
     * `ContextPropertyKey.MODES_CHOSEN_ON_TRIGGERING_SPELL` so abilities like Riku of
     * Many Paths can scale by "the number of times you chose a mode for that spell."
     */
    val modesChosenCount: Int? = null,
    /**
     * Power of the creature the trigger's source (an Aura/Equipment) was attached to, captured
     * when the trigger fired. Carried as last-known information (CR 608.2h) so that an
     * "enchanted creature deals damage equal to its power" ability still uses the right power
     * if the creature — and the aura — leave before the ability resolves. Null for non-attached
     * sources. Populated by [TriggerDetector], not [fromEvent] (which has no game state).
     */
    val enchantedCreatureLastKnownPower: Int? = null,
    /**
     * Number of cards actually looked at by the scry that caused this trigger to fire. Read
     * by `ContextPropertyKey.TRIGGER_SCRY_COUNT` so "Whenever you scry, ... for each card
     * looked at" payoffs (Celeborn the Wise, Elrond Master of Healing) scale correctly.
     * `null` when the trigger was not driven by a scry.
     */
    val scryCount: Int? = null,
    /**
     * Damage past lethal dealt to the trigger's creature recipient (CR 120.4a). Captured
     * from `DamageDealtEvent.excessAmount` so payoffs like Fall of Cair Andros — "amass
     * Orcs X, where X is the excess damage" — can read it via
     * `ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT`. `null` for non-damage triggers.
     */
    val excessDamageAmount: Int? = null
) {
    companion object {
        fun fromEvent(event: com.wingedsheep.engine.core.GameEvent): TriggerContext {
            return when (event) {
                is ZoneChangeEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    counterCount = if (event.lastKnownCounterCount > 0) event.lastKnownCounterCount else null,
                    totalCounterCount = if (event.lastKnownTotalCounterCount > 0) event.lastKnownTotalCounterCount else null,
                    minusOneMinusOneCounterCount = if (event.lastKnownMinusOneMinusOneCounterCount > 0)
                        event.lastKnownMinusOneMinusOneCounterCount else null,
                    xValue = event.xValue,
                    lastKnownPower = event.lastKnownPower,
                    lastKnownToughness = event.lastKnownToughness,
                    lastKnownCounters = event.lastKnownCounters.takeIf { it.isNotEmpty() },
                    lastKnownDamageDealtByPlayers =
                        event.lastKnownDamageDealtByPlayers.takeIf { it.isNotEmpty() },
                    lastKnownBlockingOrBlockedByIds =
                        event.lastKnownBlockingOrBlockedByIds.takeIf { it.isNotEmpty() }
                )
                is DamageDealtEvent -> TriggerContext(
                    triggeringEntityId = event.targetId,
                    damageAmount = event.amount,
                    excessDamageAmount = event.excessAmount.takeIf { it > 0 }
                )
                is com.wingedsheep.engine.core.DamagePreventedEvent -> TriggerContext(
                    // The prevented source — so "deal that much to that source's controller" resolves
                    // via EffectTarget.ControllerOfTriggeringEntity, and damageAmount feeds PREVENTED_DAMAGE_AMOUNT.
                    triggeringEntityId = event.sourceId,
                    damageAmount = event.amount
                )
                is com.wingedsheep.engine.core.CountersAddedEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    counterCount = event.amount
                )
                is SpellCastEvent -> TriggerContext(
                    triggeringEntityId = event.spellEntityId,
                    triggeringPlayerId = event.casterId,
                    modesChosenCount = event.chosenModesCount.takeIf { it > 0 }
                )
                is CardsDrawnEvent -> TriggerContext(triggeringPlayerId = event.playerId)
                is com.wingedsheep.engine.core.ScriedEvent -> TriggerContext(
                    triggeringPlayerId = event.playerId,
                    scryCount = event.count
                )
                is CardsDiscardedEvent -> TriggerContext(triggeringPlayerId = event.playerId)
                is CardRevealedFromDrawEvent -> TriggerContext(
                    triggeringEntityId = event.cardEntityId,
                    triggeringPlayerId = event.playerId
                )
                is CardCycledEvent -> TriggerContext(triggeringPlayerId = event.playerId)
                is AttackersDeclaredEvent -> TriggerContext()
                is BlockersDeclaredEvent -> TriggerContext()
                is TappedEvent -> TriggerContext(triggeringEntityId = event.entityId)
                is UntappedEvent -> TriggerContext(triggeringEntityId = event.entityId)
                is LifeChangedEvent -> TriggerContext(
                    triggeringEntityId = event.playerId,
                    triggeringPlayerId = event.playerId,
                    damageAmount = when {
                        event.reason == com.wingedsheep.engine.core.LifeChangeReason.LIFE_GAIN ->
                            event.newLife - event.oldLife
                        event.oldLife > event.newLife ->
                            event.oldLife - event.newLife
                        else -> null
                    }
                )
                is TurnFaceUpEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    triggeringPlayerId = event.controllerId,
                    xValue = event.xValue
                )
                is com.wingedsheep.engine.core.TransformedEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    triggeringPlayerId = event.controllerId
                )
                is ControlChangedEvent -> TriggerContext(
                    triggeringEntityId = event.permanentId,
                    triggeringPlayerId = event.newControllerId
                )
                is BecomesTargetEvent -> TriggerContext(
                    triggeringEntityId = event.targetEntityId,
                    targetingSourceEntityId = event.sourceEntityId
                )
                is com.wingedsheep.engine.core.TargetsChosenEvent -> TriggerContext(
                    triggeringEntityId = event.stackObjectId,
                    triggeringPlayerId = event.chooserId
                )
                is AbilityActivatedEvent -> TriggerContext(
                    triggeringEntityId = event.abilityEntityId,
                    triggeringPlayerId = event.controllerId
                )
                is AbilityTriggeredEvent -> TriggerContext(
                    triggeringEntityId = event.abilityEntityId,
                    triggeringPlayerId = event.controllerId
                )
                else -> TriggerContext()
            }
        }
    }
}
