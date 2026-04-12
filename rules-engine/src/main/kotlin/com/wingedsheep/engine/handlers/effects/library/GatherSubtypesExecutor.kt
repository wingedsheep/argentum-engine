package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GatherSubtypesEffect
import kotlin.reflect.KClass

/**
 * Executor for [GatherSubtypesEffect].
 *
 * Reads an entity collection from `storedCollections[from]`, extracts each
 * entity's projected subtypes, and stores the result as a `List<Set<String>>`
 * in `updatedSubtypeGroups[storeAs]`.
 *
 * Uses projected state for battlefield permanents so continuous effects
 * (e.g., Mistform Ultimus gaining all creature types) are reflected.
 */
class GatherSubtypesExecutor : EffectExecutor<GatherSubtypesEffect> {

    override val effectType: KClass<GatherSubtypesEffect> = GatherSubtypesEffect::class

    override fun execute(
        state: GameState,
        effect: GatherSubtypesEffect,
        context: EffectContext
    ): EffectResult {
        val entities = context.pipeline.storedCollections[effect.from]
            ?: return EffectResult.error(state, "No collection named '${effect.from}' in storedCollections")

        val projected = state.projectedState
        val subtypeGroups = entities.map { entityId ->
            // Try projected subtypes first (for battlefield permanents with continuous effects),
            // then fall back to base CardComponent subtypes (for hand/graveyard/exile entities)
            val projectedSubtypes = projected.getSubtypes(entityId)
            if (projectedSubtypes.isNotEmpty()) {
                projectedSubtypes
            } else {
                state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.subtypes
                    ?.map { it.value }?.toSet() ?: emptySet()
            }
        }

        return EffectResult.success(state).copy(
            updatedSubtypeGroups = mapOf(effect.storeAs to subtypeGroups)
        )
    }
}
