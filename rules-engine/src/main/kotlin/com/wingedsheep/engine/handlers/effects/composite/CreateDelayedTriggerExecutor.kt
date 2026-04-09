package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.DestroyAllEquipmentOnTargetEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.effects.StoreResultEffect
import com.wingedsheep.sdk.scripting.effects.WarpExileEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for CreateDelayedTriggerEffect.
 *
 * Creates a delayed triggered ability that fires at a specific step.
 * Before storing the trigger, resolves any context-dependent target references
 * (e.g., ContextTarget(0)) to concrete SpecificEntity references so the
 * delayed trigger can fire correctly after the original execution context is gone.
 *
 * Used for Astral Slide-style exile-until-end-step patterns.
 */
class CreateDelayedTriggerExecutor : EffectExecutor<CreateDelayedTriggerEffect> {

    override val effectType: KClass<CreateDelayedTriggerEffect> = CreateDelayedTriggerEffect::class

    override fun execute(
        state: GameState,
        effect: CreateDelayedTriggerEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "CreateDelayedTrigger requires a source ID")

        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        // Bake in any context-dependent target references so the delayed trigger
        // has concrete entity IDs when it fires later.
        val resolvedEffect = resolveContextTargets(effect.effect, context)

        // For event-based delayed triggers, bake the watched target into a concrete
        // entity id so matching later is cheap and doesn't need the original context.
        val watchedEntityId = effect.watchedTarget?.let { resolveTarget(it, context) }

        val delayedTrigger = DelayedTriggeredAbility(
            id = UUID.randomUUID().toString(),
            effect = resolvedEffect,
            fireAtStep = effect.step,
            sourceId = sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            fireOnlyOnControllersTurn = effect.fireOnlyOnControllersTurn,
            trigger = effect.trigger,
            watchedEntityId = watchedEntityId,
            expiry = if (effect.trigger != null) effect.expiry else null
        )

        return ExecutionResult.success(state.addDelayedTrigger(delayedTrigger))
    }

    /**
     * Recursively substitute context-dependent target references with concrete SpecificEntity
     * references using the current execution context.
     *
     * This covers ContextTarget(n), Self, TriggeringEntity, and any other non-persistent
     * target types that won't be resolvable when the delayed trigger fires later.
     */
    private fun resolveContextTargets(effect: Effect, context: EffectContext): Effect {
        return when (effect) {
            is MoveToZoneEffect -> {
                val resolvedId = resolveTarget(effect.target, context)
                val resolvedController = effect.controllerOverride?.let { co ->
                    resolveTarget(co, context)?.let { EffectTarget.SpecificEntity(it) }
                }
                effect.copy(
                    target = if (resolvedId != null) EffectTarget.SpecificEntity(resolvedId) else effect.target,
                    controllerOverride = resolvedController ?: effect.controllerOverride
                )
            }
            is SacrificeTargetEffect -> {
                val resolvedId = resolveTarget(effect.target, context)
                if (resolvedId != null) effect.copy(target = EffectTarget.SpecificEntity(resolvedId)) else effect
            }
            is DestroyAllEquipmentOnTargetEffect -> {
                val resolvedId = resolveTarget(effect.target, context)
                if (resolvedId != null) effect.copy(target = EffectTarget.SpecificEntity(resolvedId)) else effect
            }
            is WarpExileEffect -> {
                val resolvedId = resolveTarget(effect.target, context)
                if (resolvedId != null) effect.copy(target = EffectTarget.SpecificEntity(resolvedId)) else effect
            }
            is AddCountersEffect -> {
                val resolvedId = resolveTarget(effect.target, context)
                if (resolvedId != null) effect.copy(target = EffectTarget.SpecificEntity(resolvedId)) else effect
            }
            is CompositeEffect -> effect.copy(
                effects = effect.effects.map { resolveContextTargets(it, context) }
            )
            is MayEffect -> {
                val inner = resolveContextTargets(effect.effect, context)
                if (inner !== effect.effect) effect.copy(effect = inner) else effect
            }
            is StoreResultEffect -> {
                val inner = resolveContextTargets(effect.effect, context)
                if (inner !== effect.effect) effect.copy(effect = inner) else effect
            }
            else -> effect
        }
    }
}
