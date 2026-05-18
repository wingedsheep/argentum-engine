package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.destroyPermanent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.scripting.effects.DestroySourceOfTargetedAbilityEffect
import kotlin.reflect.KClass

/**
 * Executor for [DestroySourceOfTargetedAbilityEffect].
 *
 * Reads the first target from [EffectContext.targets] (expected to be a
 * [ChosenTarget.Spell] referencing a stack entity). If the entity is an activated or
 * triggered ability whose `sourceId` is currently a permanent on the battlefield, the
 * source is destroyed via [destroyPermanent] (which respects indestructible). If the
 * targeted entity is a spell, or its source is no longer on the battlefield, this
 * effect is a no-op.
 */
class DestroySourceOfTargetedAbilityExecutor : EffectExecutor<DestroySourceOfTargetedAbilityEffect> {

    override val effectType: KClass<DestroySourceOfTargetedAbilityEffect> =
        DestroySourceOfTargetedAbilityEffect::class

    override fun execute(
        state: GameState,
        effect: DestroySourceOfTargetedAbilityEffect,
        context: EffectContext
    ): EffectResult {
        val target = context.targets.firstOrNull() as? ChosenTarget.Spell
            ?: return EffectResult.success(state)

        val stackEntity = state.getEntity(target.spellEntityId)
            ?: return EffectResult.success(state)

        val sourceId = stackEntity.get<ActivatedAbilityOnStackComponent>()?.sourceId
            ?: stackEntity.get<TriggeredAbilityOnStackComponent>()?.sourceId
            ?: return EffectResult.success(state)

        if (sourceId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        return destroyPermanent(state, sourceId)
    }
}
