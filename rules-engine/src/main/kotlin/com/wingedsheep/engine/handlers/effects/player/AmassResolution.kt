package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Shared resolution for the back half of "amass [subtype] N" (CR 701.47a), after the Army has been
 * chosen (or created): put N +1/+1 counters on it, and — if it isn't already that subtype — make it
 * that subtype in addition to its other types.
 *
 * Both [AmassExecutor] (the 0-army and single-army cases) and the multi-army continuation resumer
 * route through here so the counter-placement and type-changing behaviour stay identical.
 */
object AmassResolution {

    fun applyToArmy(
        state: GameState,
        armyId: EntityId,
        controllerId: EntityId,
        subtype: String,
        amount: Int,
        sourceId: EntityId?,
        executeEffect: (GameState, Effect, EffectContext) -> EffectResult
    ): EffectResult {
        val context = EffectContext(sourceId = sourceId, controllerId = controllerId)
        var newState = state
        val events = mutableListOf<GameEvent>()

        // Put N +1/+1 counters on the Army. Routing through AddCountersEffect keeps counter-placement
        // replacements (Hardened Scales, Doubling Season, …) applying to amassed counters.
        if (amount > 0) {
            val counterResult = executeEffect(
                newState,
                AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, amount, EffectTarget.SpecificEntity(armyId)),
                context
            )
            newState = counterResult.state
            events += counterResult.events
        }

        // "If it isn't a [subtype], it becomes a [subtype] in addition to its other types."
        if (!newState.projectedState.hasSubtype(armyId, subtype)) {
            newState = newState.addFloatingEffect(
                layer = Layer.TYPE,
                modification = SerializableModification.AddSubtype(subtype),
                affectedEntities = setOf(armyId),
                duration = Duration.Permanent,
                context = context
            )
        }

        // Expose the just-amassed Army to a follow-up sibling effect — Foray of Orcs and
        // Surrounded by Orcs read its power via `DynamicAmount.EntityProperty(AmassedArmy, …)`.
        // Stored under the shared key so the SDK reference and engine evaluator agree
        // without a cross-module import (see EntityReference.AmassedArmy).
        return EffectResult(
            state = newState,
            events = events,
            updatedCollections = mapOf(EntityReference.AmassedArmy.STORAGE_KEY to listOf(armyId))
        )
    }
}
