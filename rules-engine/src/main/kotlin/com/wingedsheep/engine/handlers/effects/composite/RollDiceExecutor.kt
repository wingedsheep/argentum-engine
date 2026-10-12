package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.DieRolledEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RollDiceEffect
import kotlin.reflect.KClass

/** Pure seeded dice; the settle boundary detects observers after the entire effect finishes. */
class RollDiceExecutor(private val amountEvaluator: DynamicAmountEvaluator) : EffectExecutor<RollDiceEffect> {
    override val effectType: KClass<RollDiceEffect> = RollDiceEffect::class
    override fun execute(state: GameState, effect: RollDiceEffect, context: EffectContext): EffectResult {
        val count = amountEvaluator.evaluate(state, effect.count, context).coerceAtLeast(0)
        val players = context.resolvePlayerTargets(effect.player, state)
        var current = state
        val events = mutableListOf<DieRolledEvent>()
        val counts = effect.resultCounts.mapValues { 0 }.toMutableMap()
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Ability"
        for (player in players) repeat(count) {
            val (natural, next) = current.nextRandom { nextInt(effect.sides) }
            current = next
            val result = natural + 1
            events.add(DieRolledEvent(player, effect.sides, result, context.sourceId, sourceName))
            for ((key, values) in effect.resultCounts) if (result in values) counts[key] = counts.getValue(key) + 1
        }
        return EffectResult(current, events, updatedStoredNumbers = counts)
    }
}
