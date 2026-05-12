package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ReturnTargetCreatureCardFromGraveyardToHandEffect
import kotlin.reflect.KClass

class ReturnTargetCreatureCardFromGraveyardToHandHandler :
    EffectExecutor<ReturnTargetCreatureCardFromGraveyardToHandEffect> {

    override val effectType: KClass<ReturnTargetCreatureCardFromGraveyardToHandEffect> =
        ReturnTargetCreatureCardFromGraveyardToHandEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnTargetCreatureCardFromGraveyardToHandEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for return to hand")

        val result = ZoneTransitionService.moveToZone(state, targetId, Zone.HAND)
        return EffectResult.success(result.state, result.events)
    }
}
