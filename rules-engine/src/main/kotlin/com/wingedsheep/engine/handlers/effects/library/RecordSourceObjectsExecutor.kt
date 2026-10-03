package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.SourceObjectsRecordedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.SourceObjectRecord
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.RecordSourceObjectsEffect
import kotlin.reflect.KClass

/** Persistent, source-visit-scoped public battlefield histories; no characteristic snapshot. */
internal object SourceObjectRecords {
    fun sourceReference(state: GameState, context: EffectContext): ObjectRef? {
        val sourceId = context.sourceId ?: return null
        // Never advance the origin through authorized zone changes: blinking starts a new history.
        if (context.objectReferences.captured) {
            return context.objectReferences.origin?.takeIf { it.entityId == sourceId }
                ?: context.objectReferences.source?.takeIf { it.entityId == sourceId }
        }
        val timestamp = context.sourceBattlefieldTimestamp
        // Legacy contexts can identify departed visits by their battlefield timestamp.
        if (timestamp != null) {
            state.sourceObjectRecords.values.firstOrNull {
                it.source.entityId == sourceId && it.battlefieldTimestamp == timestamp
            }?.let { return it.source }
        }
        if (sourceId !in state.getBattlefield()) return null
        if (timestamp != null && timestamp != state.getEntity(sourceId)
                ?.get<BattlefieldEntryTimestampComponent>()?.timestamp) return null
        return state.objectRef(sourceId)
    }

    fun scopeKey(source: ObjectRef): String = "${source.entityId}:${source.generation}"

    fun gather(state: GameState, context: EffectContext, key: String): List<EntityId> {
        val source = sourceReference(state, context) ?: return emptyList()
        val slots = state.sourceObjectRecords[scopeKey(source)]?.slots ?: return emptyList()
        val battlefield = state.getBattlefield().toSet() // Phased-out objects are absent from this view.
        return slots[key].orEmpty().filter {
            it.entityId in battlefield && state.isCurrentObject(it)
        }.map { it.entityId }
    }
}

class RecordSourceObjectsExecutor : EffectExecutor<RecordSourceObjectsEffect> {
    override val effectType: KClass<RecordSourceObjectsEffect> = RecordSourceObjectsEffect::class

    override fun execute(state: GameState, effect: RecordSourceObjectsEffect, context: EffectContext): EffectResult {
        val source = SourceObjectRecords.sourceReference(state, context) ?: return EffectResult.success(state)
        val scope = SourceObjectRecords.scopeKey(source)
        val existing = state.sourceObjectRecords[scope] ?: SourceObjectRecord(
            source,
            context.sourceBattlefieldTimestamp ?: state.getEntity(source.entityId)
                ?.takeIf { state.isCurrentObject(source) }
                ?.get<BattlefieldEntryTimestampComponent>()?.timestamp,
        )
        val history = existing.slots[effect.key].orEmpty()
        val battlefield = state.getBattlefield().toSet()
        val additions = context.pipeline.storedCollections[effect.from].orEmpty()
            .filter { it in battlefield }.mapNotNull { state.objectRef(it) }.distinct().filter { it !in history }
        if (additions.isEmpty()) return EffectResult.success(state)
        val updated = existing.copy(slots = existing.slots + (effect.key to (history + additions)))
        return EffectResult.success(
            state.copy(sourceObjectRecords = state.sourceObjectRecords + (scope to updated)),
            listOf(SourceObjectsRecordedEvent(source.entityId, effect.key, additions.map { it.entityId })),
        )
    }
}
