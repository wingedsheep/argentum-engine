package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.BlockerDeclarationPolicyChangedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.GrantCantBeBlockedExceptByCollectionEffect
import kotlin.reflect.KClass

class GrantCantBeBlockedExceptByCollectionExecutor : EffectExecutor<GrantCantBeBlockedExceptByCollectionEffect> {
    override val effectType: KClass<GrantCantBeBlockedExceptByCollectionEffect> =
        GrantCantBeBlockedExceptByCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: GrantCantBeBlockedExceptByCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val battlefield = state.getBattlefield().toHashSet()
        val attacker = TargetResolutionUtils.resolveTarget(effect.target, context, state)
            ?.takeIf { it in battlefield } ?: return EffectResult.success(state)
        val blockers = context.pipeline.storedCollections[effect.collection].orEmpty()
            .filter { it in battlefield }.mapNotNull(state::objectRef).toSet()
        return EffectResult.success(
            state.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.CantBeBlockedExceptByCollection(blockers, effect.alternativeFilter),
                affectedEntities = setOf(attacker),
                duration = effect.duration,
                context = context
            ),
            listOf(BlockerDeclarationPolicyChangedEvent)
        )
    }
}
