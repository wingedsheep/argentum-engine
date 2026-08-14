package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.daynight.DayNightService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.SetDayNightEffect
import kotlin.reflect.KClass

/**
 * Executor for [SetDayNightEffect] — "It becomes day" / "It becomes night" (CR 731.1).
 *
 * All the behavior lives in [DayNightService], the single writer for `GameState.dayNight`: setting a
 * designation the game already has is a no-op (CR 731.1), and an actual change cascades the
 * daybound/nightbound transforms it entails (CR 702.145b/e) in the same event batch as the
 * [com.wingedsheep.engine.core.DayNightChangedEvent]. This executor only forwards the requested
 * designation and the effect's source name for event attribution.
 */
class SetDayNightExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<SetDayNightEffect> {

    override val effectType: KClass<SetDayNightEffect> = SetDayNightEffect::class

    override fun execute(
        state: GameState,
        effect: SetDayNightEffect,
        context: EffectContext
    ): EffectResult {
        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: effect.description
        val (newState, events) = DayNightService.set(state, cardRegistry, effect.designation, sourceName)
        return EffectResult.success(newState, events)
    }
}
