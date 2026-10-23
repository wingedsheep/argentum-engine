package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.DiceRolledEvent
import com.wingedsheep.engine.core.DieRolledEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DiceRollModifiers
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RollDiceEffect
import kotlin.reflect.KClass

/**
 * Pure seeded dice; the settle boundary detects observers after the entire effect finishes.
 *
 * A [com.wingedsheep.sdk.scripting.RollAdditionalDice] replacement (Pixie Guide) adds dice to each
 * roller's instruction and ignores that many of the lowest. Ignored rolls are reported flagged
 * [DieRolledEvent.ignored] so the log shows them, but they never happened (CR 706.6): no trigger,
 * tally or batch summary sees them.
 */
class RollDiceExecutor(
    private val amountEvaluator: DynamicAmountEvaluator,
    private val cardRegistry: CardRegistry,
) : EffectExecutor<RollDiceEffect> {
    override val effectType: KClass<RollDiceEffect> = RollDiceEffect::class
    override fun execute(state: GameState, effect: RollDiceEffect, context: EffectContext): EffectResult {
        val count = amountEvaluator.evaluate(state, effect.count, context).coerceAtLeast(0)
        val players = context.resolvePlayerTargets(effect.player, state)
        var current = state
        val events = mutableListOf<GameEvent>()
        val counts = effect.resultCounts.mapValues { 0 }.toMutableMap()
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Ability"
        for (player in players) {
            if (count == 0) continue
            val extra = DiceRollModifiers.extraDice(current, cardRegistry, player)
            val rolled = List(count + extra) {
                val (natural, next) = current.nextRandom { nextInt(effect.sides) }
                current = next
                natural + 1
            }
            // Ties for lowest are interchangeable, so the earliest of them is ignored.
            val ignored = rolled.indices.sortedBy { rolled[it] }.take(extra).toSet()
            val results = mutableListOf<Int>()
            rolled.forEachIndexed { index, result ->
                val isIgnored = index in ignored
                events.add(DieRolledEvent(player, effect.sides, result, context.sourceId, sourceName, ignored = isIgnored))
                if (isIgnored) return@forEachIndexed
                results.add(result)
                for ((key, values) in effect.resultCounts) if (result in values) counts[key] = counts.getValue(key) + 1
            }
            // "Whenever you roll one or more dice" sees the instruction once, after its dice.
            events.add(DiceRolledEvent(player, effect.sides, results, context.sourceId))
        }
        return EffectResult(current, events, updatedStoredNumbers = counts)
    }
}
