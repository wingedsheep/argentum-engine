package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PermanentExploredEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.EmitExploredEventEffect
import kotlin.reflect.KClass

/**
 * Emits a [PermanentExploredEvent] so "whenever a creature you control explores" triggers
 * (CR 701.44) fire. Appended to the tail of the nonland explore branch (after the top/graveyard
 * decision resolves) so the event lands in a completed resolution batch — see
 * [com.wingedsheep.sdk.scripting.effects.EmitExploredEventEffect]. The land and empty-library
 * branches of [ExploreEffectExecutor] emit the event inline instead (no pause).
 */
class EmitExploredEventExecutor : EffectExecutor<EmitExploredEventEffect> {

    override val effectType: KClass<EmitExploredEventEffect> = EmitExploredEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitExploredEventEffect,
        context: EffectContext
    ): EffectResult {
        val exploringId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        return EffectResult.success(
            state,
            listOf(
                PermanentExploredEvent(
                    exploringPermanentId = exploringId,
                    controllerId = context.controllerId,
                    revealedCardWasLand = effect.revealedCardWasLand,
                    sourceName = sourceName
                )
            )
        )
    }
}
