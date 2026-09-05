package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.Effect
import java.util.UUID

/** Installs a one-shot source shield and its linked "when damage is prevented this way" trigger. */
internal fun GameState.installPreventAndReactShield(
    damageSourceId: EntityId,
    protectedEntityId: EntityId?,
    controllerId: EntityId,
    effectSourceId: EntityId?,
    effectSourceName: String?,
    onPrevented: Effect?,
    preventDamage: Boolean,
    objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
): GameState {
    val (stateWithSource, reactionSourceId) = if (effectSourceId != null) {
        this to effectSourceId
    } else {
        val (id, newState) = newEntity()
        newState to id
    }
    val sourceName = effectSourceName
        ?: stateWithSource.getEntity(reactionSourceId)?.get<CardComponent>()?.name
        ?: "Source"
    val delayedTriggerId = UUID.randomUUID().toString()

    var result = stateWithSource
    onPrevented?.let { reaction ->
        result = result.addDelayedTrigger(
            DelayedTriggeredAbility(
                id = delayedTriggerId,
                effect = reaction,
                objectReferences = objectReferences,
                sourceId = reactionSourceId,
                sourceName = sourceName,
                controllerId = controllerId,
                trigger = TriggerSpec(event = EventPattern.DamagePreventedEvent),
                watchedEntityId = damageSourceId,
                expiry = DelayedTriggerExpiry.EndOfTurn
            )
        )
    }

    return result.addFloatingEffect(
        layer = Layer.ABILITY,
        modification = SerializableModification.PreventNextDamageFromSourceShield(
            damageSourceId = damageSourceId,
            linkId = delayedTriggerId,
            preventDamage = preventDamage
        ),
        affectedEntities = protectedEntityId?.let(::setOf) ?: emptySet(),
        duration = Duration.EndOfTurn,
        context = EffectContext(sourceId = effectSourceId, controllerId = controllerId)
    )
}
