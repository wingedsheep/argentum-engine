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
 * [ScriedEvent] so "Whenever you scry" triggers (CR 701.18) fire exactly once per
 * scry, after the top/bottom moves have all resolved.
 *
 * The carried count is the size of the named gather collection (`"scried"` by default)
 * at resolution time — the cards `GatherCardsEffect` actually pulled, which equals
 * the scry N parameter unless the library held fewer (CR 701.18a). That count can be
 * zero when the library was empty; the event is still emitted, because CR 701.18d fires
 * "whenever you scry" triggers "even if some or all of those actions were impossible."
 * A literal "scry 0" (CR 701.18b: no scry event occurs) never reaches this executor —
 * [com.wingedsheep.sdk.dsl.LibraryPatterns.scry] omits the tail entirely for N == 0.
 */
class EmitScriedEventExecutor : EffectExecutor<EmitScriedEventEffect> {

    override val effectType: KClass<EmitScriedEventEffect> = EmitScriedEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitScriedEventEffect,
        context: EffectContext
    ): EffectResult {
        // Actual cards looked at (CR 701.18a); may be 0 if the library was empty, in
        // which case the trigger still fires per CR 701.18d.
        val count = context.pipeline.storedCollections[effect.gatherCollection]?.size ?: 0

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
