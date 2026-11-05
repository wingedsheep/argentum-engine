package com.wingedsheep.engine.handlers.effects.permanent.protection

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.sdk.scripting.effects.GrantKeywordVariantsSharedByGroupEffect
import com.wingedsheep.sdk.core.Keyword
import kotlin.reflect.KClass

/**
 * Executor for [GrantKeywordVariantsSharedByGroupEffect] — Concerted Effort's protection clause,
 * Majestic Myriarch's and Odric's hexproof-from variants.
 *
 * Every protection ability lives in projected state as a `PROTECTION_FROM_…` keyword string
 * (colour, `CARDTYPE_`, `SUBTYPE_`, `SUPERTYPE_`, `EACH_OPPONENT`), and every hexproof-from ability
 * as a `HEXPROOF_FROM_…` one, whether it is printed, granted by a static, or granted until end of
 * turn — the combat, targeting and damage rules all read those strings. So "every variant a member
 * of the group has" is the union of the family's strings across the group's projected keywords,
 * read now, and each one is granted to the target as its own keyword grant for the effect's
 * duration. Snapshotting at resolution is the printed ruling: the gained variants stay even if the
 * creature that supplied them leaves.
 */
class GrantKeywordVariantsSharedByGroupExecutor(
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<GrantKeywordVariantsSharedByGroupEffect> {

    override val effectType: KClass<GrantKeywordVariantsSharedByGroupEffect> =
        GrantKeywordVariantsSharedByGroupEffect::class

    override fun execute(
        state: GameState,
        effect: GrantKeywordVariantsSharedByGroupEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state) ?: return EffectResult.success(state)
        if (targetId !in state.getBattlefield()) return EffectResult.success(state)
        val cardComponent = state.getEntity(targetId)?.get<CardComponent>() ?: return EffectResult.success(state)

        val projected = state.projectedState
        val members = BattlefieldFilterUtils.findMatchingOnBattlefield(state, effect.group.baseFilter, context, predicateEvaluator = predicateEvaluator)
        val prefix = VARIANT_PREFIXES.getValue(effect.keyword)
        val variants = members
            .flatMap { projected.getKeywords(it) }
            .filter { it.startsWith(prefix) }
            .toSortedSet()
        // A grant the target already has is redundant (multiple instances of the
        // same variant are redundant), but granting it again is harmless and keeps the grant
        // standing if the target's own copy is later removed this turn.
        if (variants.isEmpty()) return EffectResult.success(state)

        var newState = state
        for (variant in variants) {
            newState = newState.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.GrantKeyword(variant),
                affectedEntities = setOf(targetId),
                duration = effect.duration,
                context = context
            )
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val displayName = nameVisibleToAll(state, targetId, cardComponent.name)
        val events = variants.map { variant ->
            KeywordGrantedEvent(
                targetId = targetId,
                targetName = displayName,
                keyword = variant.lowercase().replace('_', ' '),
                sourceName = sourceName
            )
        }
        return EffectResult.success(newState, events)
    }

    private companion object {
        val VARIANT_PREFIXES = mapOf(
            Keyword.PROTECTION to "PROTECTION_FROM_",
            Keyword.HEXPROOF to "HEXPROOF_FROM_",
        )
    }
}
