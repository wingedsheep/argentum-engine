package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.dsl.LibraryPatterns
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ScryEffect
import com.wingedsheep.sdk.scripting.effects.SurveilEffect
import kotlin.reflect.KClass

/**
 * Executor for the [ScryEffect] macro (CR 701.18). It carries no gather/select/move logic of its
 * own: it expands the marker into the shared [LibraryPatterns.scryPipeline] composite and delegates
 * to the registry's recursive [effectExecutor], which dispatches the composite through
 * `CompositeEffectExecutor`. That executor already owns the choose-pause → `EffectContinuation`
 * plumbing, so the `SelectCardsDecision` pause for "put any number on the bottom" still works, and
 * the `ScriedEvent` tail still fires "Whenever you scry" triggers (CR 701.18d).
 *
 * Collapsing scry to one node is purely a representation change — execution is byte-for-byte the
 * old expanded pipeline.
 */
class ScryExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
) : EffectExecutor<ScryEffect> {

    override val effectType: KClass<ScryEffect> = ScryEffect::class

    override fun execute(state: GameState, effect: ScryEffect, context: EffectContext): EffectResult =
        effectExecutor(state, LibraryPatterns.scryPipeline(effect.count), context)
}

/**
 * Executor for the [SurveilEffect] macro (CR 701.42) — the surveil twin of [ScryExecutor]. Expands
 * to [LibraryPatterns.surveilPipeline] and delegates to the same composite machinery; the
 * `SurveiledEvent` tail still fires "Whenever you surveil" / "scry or surveil" triggers.
 */
class SurveilExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
) : EffectExecutor<SurveilEffect> {

    override val effectType: KClass<SurveilEffect> = SurveilEffect::class

    override fun execute(state: GameState, effect: SurveilEffect, context: EffectContext): EffectResult =
        effectExecutor(state, LibraryPatterns.surveilPipeline(effect.count), context)
}
