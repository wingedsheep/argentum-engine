package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.SpellCostModifier
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ModifySpellCostsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for [ModifySpellCostsEffect].
 *
 * Evaluates the amount and the affected players **once, here**, and records one [SpellCostModifier]
 * per affected player on the game state. The cost calculator applies it to every matching spell
 * that player casts until the effect's
 * duration ends — [com.wingedsheep.engine.core.TurnManager.startTurn] clears end-of-turn entries,
 * [com.wingedsheep.engine.core.CleanupPhaseManager.expireUntilYourNextTurnEffects] the
 * "until your next turn" ones.
 *
 * Resolving the amount now rather than per cast is what the Scion cycle's rulings require — "the
 * value of X is determined only once, at the time the ability resolves". Mirrors
 * [GrantNextSpellAffinityExecutor], which installs the one-shot variant of the same idea.
 *
 * A resolved amount of 0 or less installs nothing: the modifier would be a no-op, and skipping it
 * keeps the state (and the client's cost display) free of dead entries.
 */
class ModifySpellCostsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<ModifySpellCostsEffect> {

    override val effectType: KClass<ModifySpellCostsEffect> = ModifySpellCostsEffect::class

    override fun execute(
        state: GameState,
        effect: ModifySpellCostsEffect,
        context: EffectContext
    ): EffectResult {
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) return EffectResult.success(state)

        val (effectiveState, sourceId) = if (context.sourceId != null) {
            state to context.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }
        val sourceName = effectiveState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val modifiers = context.resolvePlayerTargets(EffectTarget.PlayerRef(effect.player), effectiveState)
            .map { playerId ->
                SpellCostModifier(
                    controllerId = playerId,
                    spellFilter = effect.spellFilter,
                    amount = amount,
                    sourceId = sourceId,
                    sourceName = sourceName,
                    duration = effect.duration,
                    increase = effect.increase,
                    nextSpellOnly = effect.nextSpellOnly,
                )
            }
        return EffectResult.success(
            effectiveState.copy(
                spellCostModifiers = effectiveState.spellCostModifiers + modifiers
            )
        )
    }
}
