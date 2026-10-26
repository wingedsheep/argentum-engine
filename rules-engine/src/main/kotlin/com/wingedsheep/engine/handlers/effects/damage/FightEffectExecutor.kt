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

        // A fight is one instruction — each creature deals damage equal to its power to the other
        // (CR 701.14a) — so an optional redirection shield is asked about both halves before either
        // is dealt (Blood of the Martyr).
        val (readyState, pause) = OptionalDamageRedirect.beforeDealing(
            state,
            listOf(
                OptionalDamageRedirect.Instance(target1Id, target2Id, power1),
                OptionalDamageRedirect.Instance(target2Id, target1Id, power2)
            ),
            effect,
            context,
            zones.predicateEvaluator
        )
        if (pause != null) return pause

        // Creature 1 deals damage equal to its power to creature 2
        var currentState = readyState
        val allEvents = mutableListOf<GameEvent>()

        // Excess damage (CR 120.4a) that target1 deals to target2 — the "creature an opponent
        // controls" half of the fight. Captured from the actual DamageDealtEvent (deathtouch- and
        // marked-damage-aware) so a following effect can read it as a pipeline number (The Last
        // Agni Kai). Stays 0 when target1 has no power or deals only lethal/sub-lethal damage.
        var excessToTarget2 = 0

        if (power1 > 0) {
            val result1 = dealDamageToTarget(zones, currentState, target2Id, power1, target1Id)
            currentState = result1.newState
            allEvents.addAll(result1.events)
            excessToTarget2 = result1.events
                .filterIsInstance<DamageDealtEvent>()
                .filter { it.targetId == target2Id }
                .sumOf { it.excessAmount }
        }

        // Creature 2 deals damage equal to its power to creature 1
        if (power2 > 0) {
            val result2 = dealDamageToTarget(zones, currentState, target1Id, power2, target2Id)
            currentState = result2.newState
            allEvents.addAll(result2.events)
        }

        val storedNumbers = effect.excessDamageVariable
            ?.let { mapOf(it to excessToTarget2) }
            ?: emptyMap()

        return EffectResult.success(currentState, allEvents)
            .copy(updatedStoredNumbers = storedNumbers)
    }
}
