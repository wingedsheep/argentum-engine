package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.AmplifyDamageThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [AmplifyDamageThisTurnEffect].
 *
 * Resolves the bonus amount once (e.g. the `{X}` paid for the activating ability, read via
 * `DynamicAmount.XValue` → [EffectContext.xValue]) and installs an until-end-of-turn
 * [SerializableModification.AmplifyDamage] floating effect controlled by the resolver, carrying the
 * effect's `appliesTo` pattern. `DamageUtils.applyStaticDamageAmplification` reads it for every
 * damage instance, matching the pattern with that controller as "you" (CR 616). The floating effect
 * is cleared automatically by the cleanup step ([Duration.EndOfTurn]).
 *
 * Taii Wakeen, Perfect Shot: "{X}, {T}: If a source you control would deal noncombat damage to a
 * permanent or player this turn, it deals that much damage plus X instead."
 * Rankle and Torbran: "If a source would deal damage to a player or battle this turn, it deals that
 * much damage plus 2 instead."
 */
class AmplifyDamageThisTurnExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<AmplifyDamageThisTurnEffect> {

    override val effectType: KClass<AmplifyDamageThisTurnEffect> =
        AmplifyDamageThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: AmplifyDamageThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val bonus = amountEvaluator.evaluate(state, effect.bonus, context)
        // X may be 0 — installing a +0 shield is a harmless no-op, but skip it to avoid clutter.
        if (bonus <= 0) return EffectResult.success(state)

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.AmplifyDamage(bonus, effect.appliesTo),
            // affectedEntities is unused for this read-at-damage-time modification; the controller
            // (ActiveFloatingEffect.controllerId, set from context.controllerId) is what scopes it.
            affectedEntities = setOf(context.controllerId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return EffectResult.success(newState)
    }
}
