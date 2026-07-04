package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.bend.BendEvents
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.scripting.effects.ExileTargetSpellEffect
import kotlin.reflect.KClass

/**
 * Executor for [ExileTargetSpellEffect] (CR 718, "exile target spell" — Aven Interrupter).
 *
 * Resolves the chosen spell target and delegates to [StackResolver.exileSpell], which removes
 * it from the stack and puts the card into its owner's exile. This is **not** a counter: the
 * spell is exiled even if it can't be countered, and no "spell was countered" trigger fires —
 * but it still fails to resolve because it left the stack. When [ExileTargetSpellEffect.makePlotted]
 * is set, the exiled card becomes plotted for its owner (free cast on a later turn).
 *
 * If the target is no longer on the stack at resolution (it already left), the effect fizzles
 * silently rather than erroring.
 */
class ExileTargetSpellExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<ExileTargetSpellEffect> {

    override val effectType: KClass<ExileTargetSpellEffect> = ExileTargetSpellEffect::class

    override fun execute(
        state: GameState,
        effect: ExileTargetSpellEffect,
        context: EffectContext
    ): EffectResult {
        val target = context.targets.firstOrNull() as? ChosenTarget.Spell
            ?: return EffectResult.success(state)
        val spellId = target.spellEntityId
        if (spellId !in state.stack) return EffectResult.success(state)

        val resolver = StackResolver(cardRegistry = cardRegistry)
        val exiled = EffectResult.from(
            resolver.exileSpell(
                state,
                spellId,
                makePlotted = effect.makePlotted,
                fixedAlternativeManaCost = effect.fixedAlternativeManaCost
            )
        )
        // CR 701.65b: airbending a spell fires "whenever you airbend" — but only once the spell is
        // actually exiled (the early returns above already skip a target that left the stack).
        if (!effect.emitAirbend || !exiled.isSuccess) return exiled
        val (bendState, bendEvent) = BendEvents.record(exiled.state, context.controllerId, BendType.AIR)
        return exiled.copy(state = bendState, events = exiled.events + bendEvent)
    }
}
