package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.effects.SuspectEffect
import kotlin.reflect.KClass

/**
 * Executor for the Suspect keyword action (CR 701.60) — the whole mechanic in one place.
 *
 * Suspecting a creature applies three layer modifications: the named "suspected" designation, the
 * menace it gains, and the "this creature can't block" restriction. All three are applied here
 * rather than composed from three separate effects, because every reason a creature might *not*
 * become suspected has to suppress all three together:
 *
 *  - **CR 701.60d** — "a suspected permanent can't become suspected again", so a redundant suspect
 *    is a whole no-op. Without that the named-status floating list would grow and a "becomes
 *    suspected" trigger would fire more than once per suspect.
 *  - **"Can't become suspected"** ([AbilityFlag.CANT_BECOME_SUSPECTED], Airtight Alibi) — a gate on
 *    the designation alone would still land menace and can't-block, leaving a creature that isn't
 *    suspected yet carries suspect's two downsides. That is the bug this single executor exists to
 *    make unrepresentable.
 *
 * **One application, three effects.** [addFloatingEffect] does not tick [GameState.timestamp], so
 * the three modifications created below share one `(sourceId, timestamp)` pair. Rule 613 therefore
 * orders them as a single application, and that shared pair is the bundle identity
 * [RemoveSuspectedExecutor] keys on to lift the whole suspect back off again.
 */
class SuspectExecutor : EffectExecutor<SuspectEffect> {

    override val effectType: KClass<SuspectEffect> = SuspectEffect::class

    override fun execute(
        state: GameState,
        effect: SuspectEffect,
        context: EffectContext
    ): EffectResult {
        // State-aware overload: the attachment-relative targets (EnchantedCreature — Convenient
        // Target's "suspect enchanted creature") resolve only against the battlefield, and the
        // state-less overload returns null for them, which would silently drop the whole suspect.
        val entityId = TargetResolutionUtils.resolveTarget(effect.target, context, state)
            ?: return EffectResult.success(state)
        state.getEntity(entityId)?.get<CardComponent>()
            ?: return EffectResult.success(state)

        // CR 701.60d — already suspected, so nothing happens at all.
        if (com.wingedsheep.engine.handlers.predicates.isSuspected(state, entityId)) {
            return EffectResult.success(state)
        }

        // "Can't become suspected" — read from the projection, because the prohibition is itself a
        // continuous effect (an Aura's static ability, in Airtight Alibi's case).
        if (!state.projectedState.canBecomeSuspected(entityId)) {
            return EffectResult.success(state)
        }

        var newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetSuspected,
            affectedEntities = setOf(entityId),
            duration = effect.duration,
            context = context
        )
        newState = newState.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword(Keyword.MENACE.name),
            affectedEntities = setOf(entityId),
            duration = effect.duration,
            context = context
        )
        newState = newState.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetCantBlock,
            affectedEntities = setOf(entityId),
            duration = effect.duration,
            context = context
        )

        // The menace half kept the same `KeywordGrantedEvent` it emitted when suspect routed
        // through `GrantKeywordExecutor`, so the client's "gained menace" animation and anything
        // watching for granted keywords behave exactly as before the collapse. The designation and
        // the can't-block restriction emit nothing, as they did before.
        val cardName = state.getEntity(entityId)?.get<CardComponent>()?.name ?: "Unknown"
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = listOf(
            KeywordGrantedEvent(
                targetId = entityId,
                targetName = cardName,
                keyword = Keyword.MENACE.name.lowercase().replace('_', ' '),
                sourceName = sourceName
            )
        )

        return EffectResult.success(newState, events)
    }
}
