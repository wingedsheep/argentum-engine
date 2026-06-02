package com.wingedsheep.engine.handlers.effects.permanent.protection

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GrantProtectionFromColorsOfEntityEffect
import com.wingedsheep.sdk.scripting.values.EntityReference
import kotlin.reflect.KClass

/**
 * Executor for [GrantProtectionFromColorsOfEntityEffect].
 *
 * Resolves the source entity from the [EntityReference] in context, reads its projected
 * colors, then adds one `PROTECTION_FROM_<COLOR>` floating effect per color for the
 * snapshot of permanents matching the filter. Source colors are read from projected state
 * when the source is still on the battlefield, otherwise falling back to base colors on
 * the [CardComponent] (last-known-information, CR 113.7a — the source is normally still on
 * the battlefield when this resolves because Éowyn-style cards run the grant before the
 * paired exile/destroy step).
 *
 * Colorless source → no grants (colorless is not a color, CR 105.2). One-shot at resolution:
 * recipients are captured now, so permanents entering [filter] later in [duration] don't
 * gain the grant (CR 611.1).
 */
class GrantProtectionFromColorsOfEntityExecutor : EffectExecutor<GrantProtectionFromColorsOfEntityEffect> {

    override val effectType: KClass<GrantProtectionFromColorsOfEntityEffect> =
        GrantProtectionFromColorsOfEntityEffect::class

    override fun execute(
        state: GameState,
        effect: GrantProtectionFromColorsOfEntityEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = resolveEntityId(effect.source, context, state)
            ?: return EffectResult.success(state)

        val colors = readColorsAsLastKnown(state, sourceId)
        if (colors.isEmpty()) return EffectResult.success(state)

        val matchingIds = BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, effect.filter.baseFilter, context,
            excludeSelfId = if (effect.filter.excludeSelf) context.sourceId else null,
        )
        if (matchingIds.isEmpty()) return EffectResult.success(state)

        val affected = matchingIds.toSet()
        var newState = state
        val events = mutableListOf<GameEvent>()
        val ownerName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"

        for (color in colors) {
            newState = newState.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.GrantProtectionFromColor(color),
                affectedEntities = affected,
                duration = effect.duration,
                context = context,
            )
            for (id in matchingIds) {
                val name = state.getEntity(id)?.get<CardComponent>()?.name ?: continue
                events += KeywordGrantedEvent(
                    targetId = id,
                    targetName = name,
                    keyword = "Protection from ${color.lowercase()}",
                    sourceName = ownerName,
                )
            }
        }
        return EffectResult.success(newState, events)
    }

    private fun resolveEntityId(ref: EntityReference, context: EffectContext, state: GameState): EntityId? =
        when (ref) {
            is EntityReference.Source -> context.sourceId
            is EntityReference.EnchantedCreature ->
                context.sourceId?.let { state.getEntity(it)?.get<AttachedToComponent>()?.targetId }
            is EntityReference.Target -> {
                val target = context.targets.getOrNull(ref.index)
                when (target) {
                    is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> target.entityId
                    is com.wingedsheep.engine.state.components.stack.ChosenTarget.Card -> target.cardId
                    is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> target.spellEntityId
                    else -> null
                }
            }
            is EntityReference.Sacrificed -> context.sacrificedPermanents.getOrNull(ref.index)?.entityId
            is EntityReference.TappedAsCost -> context.tappedPermanents.getOrNull(ref.index)
            is EntityReference.Triggering -> context.triggeringEntityId
            is EntityReference.AffectedEntity -> context.affectedEntityId
            is EntityReference.IterationEntity -> context.pipeline.iterationTarget
            is EntityReference.FromCostStorage ->
                context.pipeline.storedCollections[ref.collectionName]?.getOrNull(ref.index)
            is EntityReference.AmassedArmy ->
                context.pipeline.storedCollections[EntityReference.AmassedArmy.STORAGE_KEY]?.firstOrNull()
        }

    private fun readColorsAsLastKnown(state: GameState, entityId: EntityId): Set<String> {
        // On battlefield → projection is authoritative, including an empty set (the source is
        // legitimately colorless via Devoid or a Layer-5 "becomes colorless" effect). Off
        // battlefield → no projectedValues entry exists, so fall back to base printed colors
        // as LKI.
        if (state.getBattlefield().contains(entityId)) {
            return state.projectedState.getColors(entityId)
        }
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return emptySet()
        return card.colors.map { it.name }.toSet()
    }
}
