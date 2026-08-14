package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import kotlin.reflect.KClass

/**
 * Executor for AddManaEffect.
 * "Add {G}" or "Add {R}{R}" or "Add {R} for each Goblin on the battlefield."
 */
class AddManaExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AddManaEffect> {

    override val effectType: KClass<AddManaEffect> = AddManaEffect::class

    override fun execute(
        state: GameState,
        effect: AddManaEffect,
        context: EffectContext
    ): EffectResult {
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return EffectResult.success(state)
        }

        var newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            val updatedPool = when {
                effect.restriction != null ->
                    manaPool.addRestricted(effect.color, amount, effect.restriction!!, effect.riders, expiry = effect.expiry)
                effect.riders.isNotEmpty() ->
                    // Rider-carrying mana (Pyromancer's Goggles): stored as an AnySpend restricted
                    // entry so the rider set survives in the pool while the mana itself stays
                    // spendable on anything. Mirrors AddManaOfChoiceExecutor.
                    manaPool.addRestricted(effect.color, amount, ManaRestriction.AnySpend, effect.riders, expiry = effect.expiry)
                effect.expiry != ManaExpiry.END_OF_TURN ->
                    // Combat-duration (firebending) mana: spendable anywhere, but tagged so the
                    // pool discards it when combat ends. Stored as an AnySpend restricted entry
                    // so it flows through the normal spend logic (canPay / pay / solver).
                    manaPool.addRestricted(effect.color, amount, ManaRestriction.AnySpend, expiry = effect.expiry)
                else ->
                    manaPool.add(effect.color, amount)
            }
            container.with(updatedPool)
        }

        // Treasure tagging only applies to ordinary, plain-counter mana (the `add` branch above).
        if (effect.restriction == null && effect.riders.isEmpty() && effect.expiry == ManaExpiry.END_OF_TURN) {
            newState = ManaProvenanceTracker.tagAddedMana(newState, context.controllerId, context.sourceId, amount)
        }

        return EffectResult.success(newState)
    }
}
