package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.SurveiledEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.EmitSurveiledEventEffect
import kotlin.reflect.KClass

/**
 * Tail of the [com.wingedsheep.sdk.dsl.Patterns.Library.surveil] composite: emits a
 * [SurveiledEvent] so "Whenever you surveil" / "Whenever you scry or surveil" triggers
 * (CR 701.42) fire exactly once per surveil, after the kept/graveyard moves have all resolved.
 *
 * The carried count is the size of the named gather collection (`"surveiled"` by default) at
 * resolution time — the cards `GatherCardsEffect` actually pulled, which equals the surveil N
 * parameter unless the library held fewer (CR 701.42a). That count can be zero when the library
 * was empty; the event is still emitted, because CR 701.42d fires "whenever you surveil" triggers
 * "even if some or all of those actions were impossible." A literal "surveil 0" (CR 701.42c: no
 * surveil event occurs) never reaches this executor —
 * [com.wingedsheep.sdk.dsl.Patterns.Library.surveil] omits the tail entirely for N == 0.
 */
class EmitSurveiledEventExecutor : EffectExecutor<EmitSurveiledEventEffect> {

    override val effectType: KClass<EmitSurveiledEventEffect> = EmitSurveiledEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitSurveiledEventEffect,
        context: EffectContext
    ): EffectResult {
        // Actual cards looked at (CR 701.42a); may be 0 if the library was empty, in
        // which case the trigger still fires per CR 701.42d.
        val count = context.pipeline.storedCollections[effect.gatherCollection]?.size ?: 0

        val playerId = context.controllerId
        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: "Surveil"

        return EffectResult.success(
            state,
            listOf(SurveiledEvent(playerId = playerId, count = count, sourceName = sourceName))
        )
    }
}
