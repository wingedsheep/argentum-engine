package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.scripting.effects.CreateRandomCreatureTokenWithManaValueEffect
import kotlin.reflect.KClass

/**
 * Executor for [CreateRandomCreatureTokenWithManaValueEffect] — the Momir Basic avatar payoff.
 *
 * Composes three atomic steps: gather the candidate creatures, select one at random with the
 * game's seeded RNG, and mint a token copy of it.
 *
 *  1. **Gather** — read the set-scoped eligible-creature pool from the active
 *     [Format.MomirBasic.eligibleCreatureNames] and filter to creatures whose mana value equals
 *     the resolved [CreateRandomCreatureTokenWithManaValueEffect.manaValue]. The list is *filtered*
 *     (not re-collected from the registry, whose map order is unspecified) and re-sorted by name so
 *     the candidate order is deterministic regardless of how the pool was supplied — a precondition
 *     for replay-stable RNG.
 *  2. **Select** — `GameRng.pick` over the sorted candidates, threading the advanced RNG back onto
 *     the state. If no creature has that mana value, nothing happens (the activation cost was still
 *     paid — CR: an effect that can't do anything does nothing).
 *  3. **Mint** — [TokenFromDefinition.mint] puts a token copy of the chosen definition onto the
 *     controller's battlefield.
 */
class CreateRandomCreatureTokenWithManaValueExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val staticAbilityHandler: StaticAbilityHandler? = null,
    private val cardRegistry: CardRegistry,
) : EffectExecutor<CreateRandomCreatureTokenWithManaValueEffect> {

    override val effectType: KClass<CreateRandomCreatureTokenWithManaValueEffect> =
        CreateRandomCreatureTokenWithManaValueEffect::class

    override fun execute(
        state: GameState,
        effect: CreateRandomCreatureTokenWithManaValueEffect,
        context: EffectContext
    ): EffectResult {
        val manaValue = amountEvaluator.evaluate(state, effect.manaValue, context)

        // The candidate pool is the active Momir/Vanguard format's set-scoped creature list.
        // Outside that format there is no pool, so the effect does nothing.
        val pool = (state.format as? Format.MomirBasic)?.eligibleCreatureNames ?: emptyList()

        val candidates = pool
            .mapNotNull { cardRegistry.getCard(it) }
            .filter { it.isCreature && it.cmc == manaValue }
            .sortedBy { it.name }

        if (candidates.isEmpty()) {
            // No creature of that mana value — cost already paid, nothing to create (CR 608.2g).
            return EffectResult.success(state)
        }

        val (chosen, stateAfterPick) = state.nextRandom { pick(candidates) }

        return TokenFromDefinition.mint(
            state = stateAfterPick,
            cardDef = chosen,
            controllerId = context.controllerId,
            staticAbilityHandler = staticAbilityHandler,
        )
    }
}
