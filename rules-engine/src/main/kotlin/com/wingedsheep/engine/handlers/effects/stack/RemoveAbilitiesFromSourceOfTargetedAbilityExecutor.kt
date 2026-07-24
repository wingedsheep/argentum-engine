package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.scripting.effects.RemoveAbilitiesFromSourceOfTargetedAbilityEffect
import kotlin.reflect.KClass

/**
 * Executor for [RemoveAbilitiesFromSourceOfTargetedAbilityEffect] — the ability-strip sibling of
 * [DestroySourceOfTargetedAbilityExecutor].
 *
 * Reads the first target from [EffectContext.targets] (expected to be a [ChosenTarget.Spell]
 * referencing a stack entity). If the entity is an activated or triggered ability whose `sourceId`
 * is currently a permanent on the battlefield, and — when the effect restricts card types — that
 * permanent's projected card types include one of them, a Layer-6 `RemoveAllAbilities` floating
 * effect is applied to the source for the effect's [RemoveAbilitiesFromSourceOfTargetedAbilityEffect.duration].
 * The floating effect is keyed to this effect's own source (via [EffectContext]), so a
 * `WhileSourceOnBattlefield` duration ends when that permanent leaves the battlefield.
 *
 * No-op when: the targeted entity is a spell (no ability component); its source is no longer on the
 * battlefield; or the source's type doesn't match the requested card types.
 */
class RemoveAbilitiesFromSourceOfTargetedAbilityExecutor :
    EffectExecutor<RemoveAbilitiesFromSourceOfTargetedAbilityEffect> {

    override val effectType: KClass<RemoveAbilitiesFromSourceOfTargetedAbilityEffect> =
        RemoveAbilitiesFromSourceOfTargetedAbilityEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveAbilitiesFromSourceOfTargetedAbilityEffect,
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

        // Type gate: "an artifact, creature, or planeswalker" — read from projected state so
        // continuous type-changing effects are honored (CR 613, projection rule).
        if (effect.sourceCardTypes.isNotEmpty()) {
            val projectedTypes = state.projectedState.getTypes(sourceId)
            val matches = effect.sourceCardTypes.any { it.name in projectedTypes }
            if (!matches) {
                return EffectResult.success(state)
            }
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RemoveAllAbilities,
            affectedEntities = setOf(sourceId),
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState, emptyList())
    }
}
