package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for [EachPermanentBecomesCopyOfTargetEffect].
 *
 * Replaces each matching permanent's [CardComponent] with a copy of the target's,
 * mirroring the mechanism used by `EntersAsCopy` (Clone) — copy effects bake into
 * base state rather than living as a Layer 1 continuous effect. See
 * `docs/architecture-principles.md` §2.11.
 *
 * Rule 707: copiable values only. Counters, tapped state, attached auras/equipment,
 * and non-copy floating effects on the target are not copied — those live on other
 * components / floating effects, not on `CardComponent`, so replacing the card
 * component wholesale produces the correct behavior automatically.
 *
 * Used by Mirrorform: "Each nonland permanent you control becomes a copy of target
 * non-Aura permanent."
 */
class EachPermanentBecomesCopyOfTargetExecutor : EffectExecutor<EachPermanentBecomesCopyOfTargetEffect> {

    override val effectType: KClass<EachPermanentBecomesCopyOfTargetEffect> =
        EachPermanentBecomesCopyOfTargetEffect::class

    override fun execute(
        state: GameState,
        effect: EachPermanentBecomesCopyOfTargetEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        val targetCard = state.getEntity(targetId)?.get<CardComponent>()
            ?: return EffectResult.success(state)

        // Target must still be on the battlefield to serve as a copy source.
        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val affected = BattlefieldFilterUtils.findMatchingOnBattlefield(
            state,
            effect.filter.baseFilter,
            context,
            excludeSelfId = if (effect.filter.excludeSelf) context.sourceId else null
        )

        if (affected.isEmpty()) {
            return EffectResult.success(state)
        }

        var newState = state
        for (entityId in affected) {
            val container = newState.getEntity(entityId) ?: continue
            val originalCard = container.get<CardComponent>() ?: continue

            // Preserve the original ownership; only copiable card characteristics change.
            val copiedCard = targetCard.copy(ownerId = originalCard.ownerId)

            newState = newState.updateEntity(entityId) { c ->
                c.with(copiedCard)
                    .with(
                        CopyOfComponent(
                            originalCardDefinitionId = originalCard.cardDefinitionId,
                            copiedCardDefinitionId = targetCard.cardDefinitionId
                        )
                    )
            }
        }

        return EffectResult.success(newState)
    }
}
