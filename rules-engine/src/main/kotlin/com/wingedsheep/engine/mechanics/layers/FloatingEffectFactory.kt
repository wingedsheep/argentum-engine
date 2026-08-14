package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Creates a single [ActiveFloatingEffect] and appends it to this state's floatingEffects list.
 *
 * Handles all boilerplate: ID generation, source name lookup, timestamp, and state copy.
 * Callers specify only what varies: layer, modification, affected entities, and duration.
 *
 * The timestamp defaults to the state's monotonic [GameState.timestamp] (see principle §2.1).
 * Wall-clock time must never be used here — it makes Rule 613 layer ordering nondeterministic
 * and breaks replay, state verification, and MCTS simulation.
 */
fun GameState.addFloatingEffect(
    layer: Layer,
    modification: SerializableModification,
    affectedEntities: Set<EntityId>,
    duration: Duration,
    context: EffectContext,
    sublayer: Sublayer? = null,
    dynamicGroupFilter: GroupFilter? = null,
    sourceCondition: Condition? = null,
    timestamp: Long = this.timestamp
): GameState {
    val (effectId, stateWithId) = newEntity()
    val effect = createFloatingEffect(
        id = effectId,
        layer = layer,
        modification = modification,
        affectedEntities = affectedEntities,
        duration = duration,
        context = context,
        sublayer = sublayer,
        dynamicGroupFilter = dynamicGroupFilter,
        sourceCondition = sourceCondition,
        timestamp = timestamp
    )
    return stateWithId.copy(floatingEffects = stateWithId.floatingEffects + effect)
}

/**
 * Creates a single [ActiveFloatingEffect] without adding it to the state.
 *
 * Use when building multiple effects to add atomically via [addFloatingEffects],
 * or when the state copy needs custom logic (e.g., filtering existing effects first).
 *
 * The timestamp defaults to the state's monotonic [GameState.timestamp] — see the note
 * on [addFloatingEffect] for why wall-clock time is forbidden.
 */
fun GameState.createFloatingEffect(
    layer: Layer,
    modification: SerializableModification,
    affectedEntities: Set<EntityId>,
    duration: Duration,
    context: EffectContext,
    sublayer: Sublayer? = null,
    dynamicGroupFilter: GroupFilter? = null,
    sourceCondition: Condition? = null,
    timestamp: Long = this.timestamp,
    id: EntityId
): ActiveFloatingEffect {
    val sourceName = context.sourceId?.let { getEntity(it)?.get<CardComponent>()?.name }
    return ActiveFloatingEffect(
        id = id,
        effect = FloatingEffectData(
            layer = layer,
            sublayer = sublayer,
            modification = modification,
            affectedEntities = affectedEntities,
            dynamicGroupFilter = dynamicGroupFilter,
            sourceCondition = sourceCondition
        ),
        duration = duration,
        sourceId = context.sourceId,
        sourceName = sourceName,
        controllerId = context.controllerId,
        timestamp = timestamp
    )
}

/**
 * Appends multiple pre-built floating effects to this state.
 *
 * Use with [createFloatingEffect] for executors that create multiple effects atomically
 * (e.g., BecomeCreatureExecutor which creates 6-7 effects with a shared timestamp).
 */
fun GameState.addFloatingEffects(effects: List<ActiveFloatingEffect>): GameState =
    copy(floatingEffects = floatingEffects + effects)
