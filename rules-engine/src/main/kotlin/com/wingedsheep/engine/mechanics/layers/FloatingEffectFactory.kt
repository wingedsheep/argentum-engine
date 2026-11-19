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
    // Built on this state rather than the copy: only the entity counter differs, and this one's
    // projection (which a keyword grant may read) is usually cached already.
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
    // An object that "can't have or gain" a keyword never gets a resolving grant of it: the effect
    // isn't created for that object, so it stays without the keyword even once the prohibition
    // ends (the Born of the Gods Archetype rulings). A static grant is a continuous effect of its
    // source instead and resumes when the prohibition goes — that is projection's business.
    val affectedEntities = withoutProhibitedKeywordGrants(modification, affectedEntities)
    val damageSourceId = when (modification) {
        is SerializableModification.PreventNextDamage -> modification.onlyFromSource
        is SerializableModification.PreventAllDamageFromSource -> modification.damageSourceId
        is SerializableModification.PreventNextDamageInstanceFromSource -> modification.damageSourceId
        is SerializableModification.PreventNextDamageLeavingAmount -> modification.damageSourceId
        is SerializableModification.PreventNextDamageFromSourceShield -> modification.damageSourceId
        else -> null
    }
    val referredIds = if (damageSourceId == null) affectedEntities else affectedEntities + damageSourceId
    val referencedObjects = referredIds.mapNotNull(::objectRef)
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
        timestamp = timestamp,
        // "Until the end of your next turn" never includes the turn it was created on — not even
        // when that is the controller's own turn — so the floor is always the next turn number.
        referencedObjects = referencedObjects,
        // The same floor serves the upkeep-step and affected-controller variants.
        expiresAfterTurn = when (duration) {
            Duration.EndOfYourNextTurn,
            Duration.EndOfYourNextUpkeep,
            Duration.EndOfAffectedControllersNextTurn -> turnNumber + 1
            else -> null
        },
        sourceUntapCount = when (duration) {
            is Duration.WhileSourceTapped,
            is Duration.WhileSourceTappedAndAffectedPowerAtMostSource,
            is Duration.WhileYouControlSourceAndSourceTapped -> context.sourceUntapCount
            else -> null
        }
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

/**
 * [affectedEntities] minus the objects that "can't have or gain" the keyword [modification]
 * grants. Only a permanent with a keyword-prohibiting static (the Archetypes) can make this
 * non-identity, so the projection is read only while one is on the battlefield.
 */
internal fun GameState.withoutProhibitedKeywordGrants(
    modification: SerializableModification,
    affectedEntities: Set<EntityId>
): Set<EntityId> {
    if (modification !is SerializableModification.GrantKeyword || affectedEntities.isEmpty()) return affectedEntities
    val anyProhibition = getBattlefield().any { id ->
        getEntity(id)?.get<ContinuousEffectSourceComponent>()?.effects?.any {
            (it.modification as? Modification.RemoveKeyword)?.prohibitsGain == true
        } == true
    }
    if (!anyProhibition) return affectedEntities
    val projected = projectedState
    if (affectedEntities.none { projected.cantGainKeyword(it, modification.keyword) }) return affectedEntities
    return affectedEntities.filterTo(LinkedHashSet()) { !projected.cantGainKeyword(it, modification.keyword) }
}
