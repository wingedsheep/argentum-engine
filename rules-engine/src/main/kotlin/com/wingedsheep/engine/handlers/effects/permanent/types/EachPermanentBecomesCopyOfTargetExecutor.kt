package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.RevertCopyAtEndOfTurnComponent
import com.wingedsheep.sdk.scripting.Duration
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

        // Target must still be on the battlefield to serve as a copy source — unless the effect
        // allows a non-battlefield source (Lazav, Familiar Stranger copies a creature card it just
        // exiled), in which case its copiable characteristics are read wherever it currently is.
        if (!effect.sourceFromAnyZone && targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val affectedTarget = effect.affected
        val affected = if (affectedTarget != null) {
            // "target permanent A becomes a copy of target permanent B" — the affected set is the
            // single resolved [affected] target (Fleeting Reflection). Resolving to nothing is a
            // no-op. Must still be on the battlefield to become a copy.
            val affectedId = context.resolveTarget(affectedTarget, state)
            if (affectedId == null || affectedId !in state.getBattlefield()) {
                emptyList()
            } else {
                listOf(affectedId)
            }
        } else {
            BattlefieldFilterUtils.findMatchingOnBattlefield(
                state,
                effect.filter.baseFilter,
                context,
                excludeSelfId = if (effect.filter.excludeSelf) context.sourceId else null
            )
                // "each OTHER … becomes a copy of that …" — the copy source keeps its own identity
                // (and any counter just placed on it), so exclude the target from the affected set.
                .filterNot { effect.excludeTarget && it == targetId }
        }

        if (affected.isEmpty()) {
            return EffectResult.success(state)
        }

        // Only `Permanent` (Mirrorform/Clone, baked forever) and `EndOfTurn` (reverted at
        // cleanup) are supported; anything else degrades gracefully to permanent.
        val temporary = effect.duration == Duration.EndOfTurn

        var newState = state
        for (entityId in affected) {
            val container = newState.getEntity(entityId) ?: continue
            val currentCard = container.get<CardComponent>() ?: continue

            // Preserve the original ownership; only copiable card characteristics change.
            val copiedCard = targetCard.copy(ownerId = currentCard.ownerId)

            // If this permanent is already a copy, keep the existing pre-copy snapshot
            // so a chain of copy effects still reverts to the printed identity on exit.
            val existingCopyOf = container.get<CopyOfComponent>()
            val originalCardSnapshot = existingCopyOf?.originalCardComponent ?: currentCard
            val originalDefinitionId =
                existingCopyOf?.originalCardDefinitionId ?: currentCard.cardDefinitionId

            newState = newState.updateEntity(entityId) { c ->
                var updated = c.with(copiedCard)
                    .with(
                        CopyOfComponent(
                            originalCardDefinitionId = originalDefinitionId,
                            copiedCardDefinitionId = targetCard.cardDefinitionId,
                            originalCardComponent = originalCardSnapshot
                        )
                    )
                if (temporary) {
                    updated = updated.with(RevertCopyAtEndOfTurnComponent)
                }
                updated
            }
        }

        return EffectResult.success(newState)
    }
}
