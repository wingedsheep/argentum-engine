package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ScriedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.EmitScriedEventEffect
import kotlin.reflect.KClass

/**
 * Tail of the [com.wingedsheep.sdk.dsl.LibraryPatterns.scry] composite: emits a
 * [ScriedEvent] so "Whenever you scry" triggers (CR 701.22) fire exactly once per
 * scry, after the top/bottom moves have all resolved.
 *
 * The carried count is the size of the named gather collection (`"scried"` by default)
 * at resolution time — the cards `GatherCardsEffect` actually pulled, which equals
 * the scry N parameter unless the library held fewer (CR 701.22a). When that count
 * is zero, no event is emitted at all, satisfying CR 701.22b ("if a player is
 * instructed to scry 0, no scry event occurs").
 */
class EmitScriedEventExecutor : EffectExecutor<EmitScriedEventEffect> {

    override val effectType: KClass<EmitScriedEventEffect> = EmitScriedEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitScriedEventEffect,
        context: EffectContext
    ): EffectResult {
        val count = context.pipeline.storedCollections[effect.gatherCollection]?.size ?: 0
        if (count == 0) return EffectResult.success(state)

        val playerId = context.controllerId
        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: "Scry"

        return EffectResult.success(
            state,
            listOf(ScriedEvent(playerId = playerId, count = count, sourceName = sourceName))
        )
    }
}
