package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.scripting.effects.CaptureControllersEffect
import kotlin.reflect.KClass

/**
 * Executor for [CaptureControllersEffect]. Reads `storedCollections[from]` and writes a
 * parallel `List<EntityId>` of controllers to `storedCollections[storeAs]`.
 *
 * For each entity:
 *   1. `state.projectedState.getController(id)` if available (battlefield permanents).
 *   2. Else its raw `ControllerComponent.playerId`.
 *   3. Else `OwnerComponent.playerId` / `CardComponent.ownerId` as a last-resort fallback.
 *
 * Permanents on the battlefield always satisfy (1) or (2). Cards live somewhere with at
 * least an owner, so the captured list is the same length and order as [from].
 */
class CaptureControllersExecutor : EffectExecutor<CaptureControllersEffect> {

    override val effectType: KClass<CaptureControllersEffect> = CaptureControllersEffect::class

    override fun execute(
        state: GameState,
        effect: CaptureControllersEffect,
        context: EffectContext
    ): EffectResult {
        val cards = context.pipeline.storedCollections[effect.from]
            ?: return EffectResult.error(state, "No collection named '${effect.from}' in storedCollections")

        val controllers = cards.map { entityId ->
            val projected = state.projectedState.getController(entityId)
            if (projected != null) return@map projected
            val entity = state.getEntity(entityId) ?: return@map context.controllerId
            entity.get<ControllerComponent>()?.playerId
                ?: entity.get<OwnerComponent>()?.playerId
                ?: entity.get<CardComponent>()?.ownerId
                ?: context.controllerId
        }

        return EffectResult.success(state).copy(
            updatedCollections = mapOf(effect.storeAs to controllers)
        )
    }
}
