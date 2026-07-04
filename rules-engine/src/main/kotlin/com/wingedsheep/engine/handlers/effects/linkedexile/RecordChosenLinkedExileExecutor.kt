package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChosenLinkedExileComponent
import com.wingedsheep.sdk.scripting.effects.RecordChosenLinkedExileEffect
import kotlin.reflect.KClass

/**
 * Executor for [RecordChosenLinkedExileEffect].
 *
 * Stamps the source permanent with a [ChosenLinkedExileComponent] pointing at the first card in the
 * pipeline collection named by the effect — the card the controller just selected out of the
 * source's linked-exile pile. A [com.wingedsheep.sdk.scripting.HasAbilitiesOfChosenLinkedExiledCard]
 * static ability on the source then grants it that card's activated and triggered abilities
 * (Koh, the Face Stealer's "Choose a creature card exiled with Koh. Koh has all activated and
 * triggered abilities of the last chosen card").
 *
 * No-op if the source has left the battlefield or the collection is empty (nothing was chosen).
 */
class RecordChosenLinkedExileExecutor : EffectExecutor<RecordChosenLinkedExileEffect> {

    override val effectType: KClass<RecordChosenLinkedExileEffect> = RecordChosenLinkedExileEffect::class

    override fun execute(
        state: GameState,
        effect: RecordChosenLinkedExileEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        if (sourceId !in state.getBattlefield()) return EffectResult.success(state)

        val chosenId = context.pipeline.storedCollections[effect.from]?.firstOrNull()
            ?: return EffectResult.success(state)

        val newState = state.updateEntity(sourceId) { container ->
            container.with(ChosenLinkedExileComponent(chosenId))
        }
        return EffectResult.success(newState)
    }
}
