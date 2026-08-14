package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.RemoveSuspectedEffect
import kotlin.reflect.KClass

/**
 * Executor for [RemoveSuspectedEffect] — "it's no longer suspected" (CR 701.60c).
 *
 * Suspect has no duration and isn't a copiable value, so the designation only ever comes off a
 * permanent that stays on the battlefield by an effect like this one. Removing it means removing
 * the floating effects `Effects.Suspect` created — all three of them, since the menace and the
 * "can't block" exist only for as long as the creature is suspected.
 *
 * **Identifying the bundle.** `Effects.Suspect` is a `CompositeEffect`, and `CompositeEffect`
 * deliberately does not tick `state.timestamp` between children so that Rule 613 treats the three
 * sub-effects as one application. That shared `(sourceId, timestamp)` is therefore the bundle's
 * identity, by design rather than by accident, and it is what this executor keys on. Two
 * independent suspects of the same creature can't collide: CR 701.60d makes the second a no-op, so
 * a creature never carries two bundles at once.
 *
 * The match is deliberately narrow — only the three modification kinds suspect applies
 * ([SerializableModification.SetSuspected], `GrantKeyword(MENACE)`, `SetCantBlock`), only where the
 * effect's affected set is exactly this one creature. Menace or can't-block from any other source
 * survives untouched; un-suspecting is not "lose menace".
 *
 * A creature that isn't suspected yields no matching bundle and the state is returned unchanged.
 */
class RemoveSuspectedExecutor : EffectExecutor<RemoveSuspectedEffect> {

    override val effectType: KClass<RemoveSuspectedEffect> = RemoveSuspectedEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveSuspectedEffect,
        context: EffectContext
    ): EffectResult {
        val entityId = TargetResolutionUtils.resolveTarget(effect.target, context, state)
            ?: return EffectResult.success(state)

        // The (sourceId, timestamp) pairs of every suspect application on this creature.
        val bundleKeys = state.floatingEffects
            .filter { it.effect.modification is SerializableModification.SetSuspected && it.affectsOnly(entityId) }
            .map { it.sourceId to it.timestamp }
            .toSet()

        if (bundleKeys.isEmpty()) return EffectResult.success(state)

        val remaining = state.floatingEffects.filterNot { fx ->
            (fx.sourceId to fx.timestamp) in bundleKeys &&
                fx.affectsOnly(entityId) &&
                fx.isSuspectPart()
        }

        return EffectResult.success(state.copy(floatingEffects = remaining))
    }
}

/** The floating effect's affected set is exactly [entityId] — the shape every suspect sub-effect has. */
private fun ActiveFloatingEffect.affectsOnly(entityId: EntityId): Boolean =
    effect.affectedEntities == setOf(entityId)

/** One of the three modifications `Effects.Suspect` applies. */
private fun ActiveFloatingEffect.isSuspectPart(): Boolean =
    when (val modification = effect.modification) {
        is SerializableModification.SetSuspected -> true
        is SerializableModification.SetCantBlock -> true
        is SerializableModification.GrantKeyword -> modification.keyword == Keyword.MENACE.name
        else -> false
    }
