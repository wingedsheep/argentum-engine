package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils.dealDamageToTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.DealDamageEqualToGreatestPowerAmongCreatureCardsInYourGraveyardEffect
import kotlin.reflect.KClass

class DealDamageEqualToGreatestPowerAmongCreatureCardsInYourGraveyardHandler :
    EffectExecutor<DealDamageEqualToGreatestPowerAmongCreatureCardsInYourGraveyardEffect> {

    override val effectType: KClass<DealDamageEqualToGreatestPowerAmongCreatureCardsInYourGraveyardEffect> =
        DealDamageEqualToGreatestPowerAmongCreatureCardsInYourGraveyardEffect::class

    override fun execute(
        state: GameState,
        effect: DealDamageEqualToGreatestPowerAmongCreatureCardsInYourGraveyardEffect,
        context: EffectContext
    ): EffectResult {
        val graveyard = state.getZone(ZoneKey(context.controllerId, Zone.GRAVEYARD))
        val amount = graveyard
            .mapNotNull { state.getEntity(it)?.get<CardComponent>() }
            .filter { it.typeLine.isCreature }
            .mapNotNull { it.baseStats?.basePower }
            .maxOrNull() ?: 0

        if (amount <= 0) return EffectResult.success(state)

        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for damage")

        return dealDamageToTarget(state, targetId, amount, context.sourceId, effect.cantBePrevented)
    }
}
