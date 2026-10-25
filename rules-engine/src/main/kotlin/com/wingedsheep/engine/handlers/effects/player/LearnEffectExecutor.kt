package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.KeywordActionReplacements
import com.wingedsheep.engine.handlers.effects.ReplaceableKeywordAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.LearnEffect
import kotlin.reflect.KClass

/**
 * Executor for [LearnEffect] — "Learn" (CR 701.48), performed by the effect's controller.
 *
 * The learn proper is [LearnEffect.body], the discard-to-draw / Lesson pipeline the SDK composes.
 * Before it runs, every [com.wingedsheep.sdk.scripting.ReplaceKeywordAction] that would replace
 * this learn is collected through [KeywordActionReplacements.collectInstead] — on the battlefield
 * and, for "as long as this card is in your graveyard" (Retriever Phoenix), in graveyards — and put
 * to the player one at a time. Applying one replaces the learn outright; declining them all
 * re-issues the learn with `replacementsApplied = true`, so none is offered twice (CR 614.5).
 *
 * @param recurse registry entry point for running the body and the replacement effects.
 */
class LearnEffectExecutor(
    private val recurse: (GameState, Effect, EffectContext) -> EffectResult,
    private val cardRegistry: CardRegistry
) : EffectExecutor<LearnEffect> {

    override val effectType: KClass<LearnEffect> = LearnEffect::class

    override fun execute(state: GameState, effect: LearnEffect, context: EffectContext): EffectResult {
        if (!effect.replacementsApplied) {
            val offers = KeywordActionReplacements.collectInstead(
                state, context.controllerId, ReplaceableKeywordAction.LEARN, cardRegistry
            )
            if (offers.isNotEmpty()) {
                return KeywordActionReplacements.offerInstead(
                    state, ReplaceableKeywordAction.LEARN, offers,
                    effect.copy(replacementsApplied = true), context, recurse
                )
            }
        }
        return recurse(state, effect.body, context)
    }
}
