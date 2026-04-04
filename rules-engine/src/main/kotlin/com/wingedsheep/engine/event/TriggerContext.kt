package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.AbilityTriggeredEvent
import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.BecomesTargetEvent
import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
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
    /** The spell or ability entity that targeted a permanent (for ward triggers) */
    val targetingSourceEntityId: EntityId? = null
) {
    companion object {
        fun fromEvent(event: com.wingedsheep.engine.core.GameEvent): TriggerContext {
            return when (event) {
                is ZoneChangeEvent -> TriggerContext(
                    triggeringEntityId = event.entityId,
                    counterCount = if (event.lastKnownCounterCount > 0) event.lastKnownCounterCount else null,
                    xValue = event.xValue
                )
                is DamageDealtEvent -> TriggerContext(
                    triggeringEntityId = event.targetId,
                    damageAmount = event.amount
                )
                is SpellCastEvent -> TriggerContext(
                    triggeringEntityId = event.spellEntityId,
                    triggeringPlayerId = event.casterId
                )
                is CardsDrawnEvent -> TriggerContext(triggeringPlayerId = event.playerId)
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
                is ControlChangedEvent -> TriggerContext(
                    triggeringEntityId = event.permanentId,
                    triggeringPlayerId = event.newControllerId
                )
                is BecomesTargetEvent -> TriggerContext(
                    triggeringEntityId = event.targetEntityId,
                    targetingSourceEntityId = event.sourceEntityId
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
