package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.ForcePlayEffect
import kotlin.reflect.KClass

class ForcePlayExecutor(private val enumerator: () -> LegalActionEnumerator) : EffectExecutor<ForcePlayEffect> {
    override val effectType: KClass<ForcePlayEffect> = ForcePlayEffect::class

    override fun execute(state: GameState, effect: ForcePlayEffect, context: EffectContext): EffectResult {
        val id = context.pipeline.storedCollections[effect.from]?.firstOrNull() ?: return EffectResult.success(state)
        val ref = state.objectRef(id) ?: return EffectResult.success(state)
        val player = context.resolvePlayerTarget(effect.player, state)?.takeIf { it in state.turnOrder }
            ?: return EffectResult.success(state)
        val finish = FinishForcedPlayContinuation(ref, player, effect.from, effect.storePlayedTo, context)
        val scoped = state.pushContinuation(finish).withPriority(player)
        val plays = enumerator().enumerate(scoped, player).filter { it.affordable && !it.hasUnfillableTargetRequirement }
        if (plays.isEmpty()) return EffectResult.success(state)
        return EffectResult.from(scoped.suspendForDecision(
            question = { decisionId -> PlayCardDecision(decisionId, player, "Play the chosen card",
                DecisionContext(sourceId = context.sourceId, phase = DecisionPhase.CASTING, subjectEntityId = id),
                id) },
            answer = ForcedPlayContinuation(ref, player),
        ))
    }
}
