package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.DamageUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.FightEffect
import kotlin.reflect.KClass

/**
 * Executor for FightEffect.
 * Each creature deals damage equal to its power to the other creature.
 */
class FightEffectExecutor(private val zones: ZoneTransitionService) : EffectExecutor<FightEffect> {

    override val effectType: KClass<FightEffect> = FightEffect::class


    override fun execute(
        state: GameState,
        effect: FightEffect,
        context: EffectContext
    ): EffectResult {
        val target1Id = context.resolveTarget(effect.target1, state)
            ?: return EffectResult.error(state, "No valid first target for fight")

        val target2Id = context.resolveTarget(effect.target2, state)
            ?: return EffectResult.error(state, "No valid second target for fight")

        // CR 701.14b: if either creature is no longer on the battlefield or no longer a creature,
        // neither fights or deals damage. "It fights" (Mind Meanderer) names the source, which may
        // have left — or left and come back as a new object — before its trigger resolves.
        val projected = state.projectedState
        val battlefield = state.getBattlefield()
        val canFight = listOf(effect.target1 to target1Id, effect.target2 to target2Id).all { (ref, id) ->
            id in battlefield && projected.isCreature(id) && !context.isUnavailableBattlefieldSource(ref, state)
        }
        if (!canFight) {
            return EffectResult.success(state)
                .copy(updatedStoredNumbers = effect.excessDamageVariable?.let { mapOf(it to 0) } ?: emptyMap())
        }

        // Get projected power for each creature (projected state accounts for buffs/debuffs)
        val power1 = projected.getPower(target1Id) ?: 0
        val power2 = projected.getPower(target2Id) ?: 0

        val result = DamageReplacementChain.deal(zones, state, listOf(
            DamageReplacementChain.Instance(target1Id, target2Id, power1),
            DamageReplacementChain.Instance(target2Id, target1Id, power2)
        ), effect, context)
        if (result.outcome is com.wingedsheep.engine.core.Outcome.Paused) return result
        val excess = result.events.filterIsInstance<DamageDealtEvent>()
            .filter { it.sourceId == target1Id && it.targetId == target2Id }.sumOf { it.excessAmount }
        return result.copy(updatedStoredNumbers = effect.excessDamageVariable?.let { mapOf(it to excess) } ?: emptyMap())
    }
}
