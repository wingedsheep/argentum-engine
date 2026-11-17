package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.CreatureStat
import com.wingedsheep.sdk.scripting.effects.MutualDamageEffect
import kotlin.reflect.KClass

/**
 * Executor for [MutualDamageEffect]: each creature deals damage equal to its chosen stat to the
 * other (Grim Contest). Both amounts are read from projected state before any damage is dealt and
 * both are dealt as one simultaneous batch. Unlike a fight it emits no `CreatureFoughtEvent`.
 */
class MutualDamageEffectExecutor(private val zones: ZoneTransitionService) : EffectExecutor<MutualDamageEffect> {

    override val effectType: KClass<MutualDamageEffect> = MutualDamageEffect::class

    override fun execute(
        state: GameState,
        effect: MutualDamageEffect,
        context: EffectContext
    ): EffectResult {
        // An illegal target is dropped before resolution reaches here; with one creature gone
        // neither deals or is dealt damage (Grim Contest's ruling).
        val target1Id = context.resolveTarget(effect.target1, state) ?: return EffectResult.success(state)
        val target2Id = context.resolveTarget(effect.target2, state) ?: return EffectResult.success(state)
        if (!FightEffectExecutor.bothStillCreatures(state, context, effect.target1 to target1Id, effect.target2 to target2Id)) {
            return EffectResult.success(state)
        }

        val projected = state.projectedState
        fun amountOf(id: com.wingedsheep.sdk.model.EntityId): Int = when (effect.stat) {
            CreatureStat.POWER -> projected.getPower(id)
            CreatureStat.TOUGHNESS -> projected.getToughness(id)
        } ?: 0

        return DamageReplacementChain.deal(zones, state, listOf(
            DamageReplacementChain.Instance(target1Id, target2Id, amountOf(target1Id)),
            DamageReplacementChain.Instance(target2Id, target1Id, amountOf(target2Id))
        ), effect, context)
    }
}
