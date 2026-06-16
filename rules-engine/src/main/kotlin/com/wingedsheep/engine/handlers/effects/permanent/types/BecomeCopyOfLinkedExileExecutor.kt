package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.CopyWhileAttachedComponent
import com.wingedsheep.sdk.scripting.effects.BecomeCopyOfLinkedExileEffect
import kotlin.reflect.KClass

/**
 * Executor for [BecomeCopyOfLinkedExileEffect] (Assimilation Aegis).
 *
 * The [BecomeCopyOfLinkedExileEffect.affected] permanent (the creature the Equipment just attached
 * to) becomes a copy of the first creature card in the source Equipment's
 * [LinkedExileComponent] — the creature exiled by the Equipment's enter trigger (CR 707.2:
 * copiable characteristics only). The copy is baked into the affected permanent's [CardComponent]
 * exactly like Clone/Mirrorform; the pre-copy snapshot is kept on the [CopyOfComponent] so it can
 * revert. A [CopyWhileAttachedComponent] tags the entity with the Equipment's id so
 * [com.wingedsheep.engine.mechanics.sba.permanent.AttachedCopyExpiryCheck] reverts the copy the
 * moment the Equipment is no longer attached to it ("for as long as this Equipment remains attached
 * to it").
 *
 * No-ops (leave the affected permanent unchanged) when:
 *  - the source Equipment has no linked exile, or its linked exile holds no creature card
 *    (the exile target was declined, or the exiled card has since left exile);
 *  - the affected permanent can't be resolved or has left the battlefield.
 */
class BecomeCopyOfLinkedExileExecutor : EffectExecutor<BecomeCopyOfLinkedExileEffect> {

    override val effectType: KClass<BecomeCopyOfLinkedExileEffect> =
        BecomeCopyOfLinkedExileEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeCopyOfLinkedExileEffect,
        context: EffectContext
    ): EffectResult {
        val equipmentId = context.sourceId ?: return EffectResult.success(state)

        val affectedId = context.resolveTarget(effect.affected, state)
            ?: return EffectResult.success(state)
        if (affectedId !in state.getBattlefield()) return EffectResult.success(state)

        // The creature card exiled with this Equipment (first creature card in its linked exile).
        val linkedIds = state.getEntity(equipmentId)?.get<LinkedExileComponent>()?.exiledIds.orEmpty()
        val exiledCreatureCard = linkedIds
            .mapNotNull { id -> state.getEntity(id)?.get<CardComponent>()?.let { id to it } }
            .firstOrNull { (_, card) -> card.typeLine.isCreature }
            ?: return EffectResult.success(state)
        val copySourceCard = exiledCreatureCard.second

        val affectedContainer = state.getEntity(affectedId) ?: return EffectResult.success(state)
        val currentCard = affectedContainer.get<CardComponent>() ?: return EffectResult.success(state)

        // Preserve ownership; only copiable characteristics change (CR 707.2).
        val copiedCard = copySourceCard.copy(ownerId = currentCard.ownerId)

        // Keep the original pre-copy snapshot if the affected permanent is already a copy, so a
        // chain still reverts to the printed identity.
        val existingCopyOf = affectedContainer.get<CopyOfComponent>()
        val originalSnapshot = existingCopyOf?.originalCardComponent ?: currentCard
        val originalDefinitionId = existingCopyOf?.originalCardDefinitionId ?: currentCard.cardDefinitionId

        val newState = state.updateEntity(affectedId) { c ->
            c.with(copiedCard)
                .with(
                    CopyOfComponent(
                        originalCardDefinitionId = originalDefinitionId,
                        copiedCardDefinitionId = copySourceCard.cardDefinitionId,
                        originalCardComponent = originalSnapshot
                    )
                )
                .with(CopyWhileAttachedComponent(equipmentId))
        }

        return EffectResult.success(newState)
    }
}
