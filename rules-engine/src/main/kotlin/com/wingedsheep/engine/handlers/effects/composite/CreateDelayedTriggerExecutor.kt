package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.effects.DealDamagePerEntityInZoneEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.DestroyAllEquipmentOnTargetEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.effects.WarpExileEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount
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

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()

    override fun execute(
        state: GameState,
        effect: CreateDelayedTriggerEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "CreateDelayedTrigger requires a source ID")

        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        // Bake in any context-dependent target references so the delayed trigger
        // has concrete entity IDs when it fires later.
        val resolvedEffect = resolveContextTargets(effect.effect, context, state)

        // Bake any chosen-value references in the trigger's filter into concrete predicates.
        // Long List of the Ents (LTR) needs "when you cast a creature spell of the type just
        // noted, that creature enters with +1/+1" — the noted type lives in `chosenValues` of
        // the pipeline running this chapter, and that EffectContext is gone by the time the
        // trigger fires. Substitute the chosen value at creation time so the trigger spec is
        // self-contained.
        val resolvedTrigger = effect.trigger?.let { bakeChosenValuesIntoTrigger(it, context) }

        // For event-based delayed triggers, bake the watched target into a concrete
        // entity id so matching later is cheap and doesn't need the original context.
        val watchedEntityId = effect.watchedTarget?.let { context.resolveTarget(it) }

        // Recipient-scoped delayed triggers ("…deals combat damage to *that player* this turn"):
        // resolve the chosen recipient (e.g. ContextTarget(0) for the targeted opponent) into a
        // concrete entity id now, while the originating context still knows who it is.
        val watchedRecipientId = effect.watchedRecipient?.let {
            context.resolvePlayerTarget(it) ?: context.resolveTarget(it)
        }

        // For step-based delayed triggers that restrict to a specific player's turn (e.g.
        // Nafs Asp's "at the beginning of their next draw step"): resolve the player target
        // now, while the trigger context still knows who it is, and bake the entity id in.
        // resolvePlayerTarget covers PlayerRef shapes; the generic resolveTarget fallback
        // covers pre-baked SpecificEntity/TriggeringEntity ids. Either way, the resolved id
        // must point at a player — anything else (e.g. SpecificEntity(creatureId)) would
        // never match state.activePlayerId and the trigger would silently never fire, so we
        // fail loudly at scheduling time instead.
        val fireOnPlayerId = effect.fireOnPlayer?.let { target ->
            val resolved = context.resolvePlayerTarget(target) ?: context.resolveTarget(target)
                ?: return EffectResult.error(state, "CreateDelayedTrigger fireOnPlayer did not resolve: $target")
            if (resolved !in state.turnOrder) {
                return EffectResult.error(
                    state,
                    "CreateDelayedTrigger fireOnPlayer resolved to non-player entity $resolved (from $target)"
                )
            }
            resolved
        }

        // The earliest turn this delayed trigger may fire, derived from effect.timing:
        //  - NEXT_END_STEP ("at the beginning of your next end step"): fires at the next
        //    upcoming end step on the controller's turn. If we're still before the end step
        //    on the controller's current turn, that end step qualifies — don't skip to the
        //    following turn. Only bump notBeforeTurn when the current turn's end step has
        //    already begun (or passed), i.e. we're in END or CLEANUP on the controller's turn.
        //  - NEXT_TURN ("on your next turn") is stricter: the current turn never qualifies,
        //    regardless of step. Combine with fireOnPlayer = PlayerRef(You) to land on the
        //    controller's upcoming turn.
        //  - CURRENT_TURN_OR_LATER: no turn floor.
        val notBeforeTurn = when (effect.timing) {
            DelayedTriggerTiming.NEXT_TURN -> state.turnNumber + 1
            DelayedTriggerTiming.NEXT_END_STEP -> {
                val onControllersTurn = context.controllerId == state.activePlayerId
                val endStepAlreadyStarted = state.step == Step.END || state.step == Step.CLEANUP
                if (onControllersTurn && endStepAlreadyStarted) state.turnNumber + 1 else null
            }
            DelayedTriggerTiming.CURRENT_TURN_OR_LATER -> null
        }

        val delayedTrigger = DelayedTriggeredAbility(
            id = UUID.randomUUID().toString(),
            effect = resolvedEffect,
            fireAtStep = effect.step,
            sourceId = sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            trigger = resolvedTrigger,
            watchedEntityId = watchedEntityId,
            watchedRecipientId = watchedRecipientId,
            expiry = if (effect.trigger != null) effect.expiry else null,
            fireOnce = effect.trigger != null && effect.fireOnce,
            notBeforeTurn = notBeforeTurn,
            targetRequirement = effect.targetRequirement,
            fireOnPlayerId = fireOnPlayerId
        )

        return EffectResult.success(state.addDelayedTrigger(delayedTrigger))
    }

    /**
     * Substitute any chosen-value references in the trigger spec's filter with the concrete
     * values from the current [EffectContext.pipeline.chosenValues]. After the delayed trigger
     * is created, the originating EffectContext is gone — the trigger spec needs to be
     * self-contained so the matcher can evaluate it on its own.
     *
     * Currently handles [CardPredicate.HasSubtypeFromVariable] inside a
     * [EventPattern.SpellCastEvent]'s `spellFilter` — that's the predicate Long List of the
     * Ents's chapter trigger needs. Extend this helper as further variable-based predicates
     * appear in trigger filters (e.g., `HasSubtypeInStoredList`, `NameEqualsChosen` inside a
     * cast trigger).
     */
    private fun bakeChosenValuesIntoTrigger(trigger: TriggerSpec, context: EffectContext): TriggerSpec {
        val chosen = context.pipeline.chosenValues
        if (chosen.isEmpty()) return trigger
        val event = trigger.event
        if (event !is EventPattern.SpellCastEvent) return trigger
        val filter = event.spellFilter
        val newPredicates = filter.cardPredicates.map { predicate ->
            if (predicate is CardPredicate.HasSubtypeFromVariable) {
                val value = chosen[predicate.variableName] ?: return@map predicate
                CardPredicate.HasSubtype(Subtype(value))
            } else {
                predicate
            }
        }
        if (newPredicates == filter.cardPredicates) return trigger
        val newFilter = filter.copy(cardPredicates = newPredicates)
        return trigger.copy(event = event.copy(spellFilter = newFilter))
    }

    /**
     * Capture a [DynamicAmount] that reads from the *current* context (a target spell/permanent,
     * the mana spent to cast it, etc.) into a [DynamicAmount.Fixed] literal, so the delayed
     * trigger's effect carries the value forward to a later step when the originating context —
     * and any referenced object — no longer exists. Already-fixed amounts are returned unchanged.
     */
    private fun snapshotAmount(
        amount: DynamicAmount,
        context: EffectContext,
        state: GameState
    ): DynamicAmount {
        if (amount is DynamicAmount.Fixed) return amount
        val value = dynamicAmountEvaluator.evaluate(state, amount, context)
        return DynamicAmount.Fixed(value)
    }

    /**
     * Recursively substitute context-dependent target references with concrete SpecificEntity
     * references using the current execution context.
     *
     * This covers ContextTarget(n), Self, TriggeringEntity, and any other non-persistent
     * target types that won't be resolvable when the delayed trigger fires later.
     */
    private fun resolveContextTargets(effect: Effect, context: EffectContext, state: GameState): Effect {
        return when (effect) {
            is MoveToZoneEffect -> {
                val resolvedId = context.resolveTarget(effect.target)
                val resolvedController = effect.controllerOverride?.let { co ->
                    context.resolveTarget(co)?.let { EffectTarget.SpecificEntity(it) }
                }
                effect.copy(
                    target = if (resolvedId != null) EffectTarget.SpecificEntity(resolvedId) else effect.target,
                    controllerOverride = resolvedController ?: effect.controllerOverride
                )
            }
            is SacrificeTargetEffect -> {
                val resolvedId = context.resolveTarget(effect.target)
                if (resolvedId != null) effect.copy(target = EffectTarget.SpecificEntity(resolvedId)) else effect
            }
            is DestroyAllEquipmentOnTargetEffect -> {
                val resolvedId = context.resolveTarget(effect.target)
                if (resolvedId != null) effect.copy(target = EffectTarget.SpecificEntity(resolvedId)) else effect
            }
            is WarpExileEffect -> {
                val resolvedId = context.resolveTarget(effect.target)
                if (resolvedId != null) effect.copy(target = EffectTarget.SpecificEntity(resolvedId)) else effect
            }
            is AddCountersEffect -> {
                val resolvedId = context.resolveTarget(effect.target)
                if (resolvedId != null) effect.copy(target = EffectTarget.SpecificEntity(resolvedId)) else effect
            }
            // A delayed trigger that adds mana "equal to" a value read from a context entity
            // (e.g. Mana Sculpt: "{C} equal to the amount of mana spent to cast that spell, at the
            // beginning of your next main phase") must capture that value NOW — the referenced
            // spell/permanent is gone by the time the trigger fires, so a lazy read would yield 0.
            // Snapshot the amount against the current context into a Fixed literal.
            is AddManaEffect -> {
                val snapshot = snapshotAmount(effect.amount, context, state)
                if (snapshot !== effect.amount) effect.copy(amount = snapshot) else effect
            }
            is AddColorlessManaEffect -> {
                val snapshot = snapshotAmount(effect.amount, context, state)
                if (snapshot !== effect.amount) effect.copy(amount = snapshot) else effect
            }
            is DealDamagePerEntityInZoneEffect -> {
                // Resolve collection name to concrete entity IDs from the pipeline
                val resolvedIds = effect.collectionName?.let { name ->
                    context.pipeline.storedCollections[name]
                } ?: effect.entityIds
                val resolvedSource = effect.damageSource?.let { ds ->
                    context.resolveTarget(ds)?.let { EffectTarget.SpecificEntity(it) }
                }
                effect.copy(
                    entityIds = resolvedIds,
                    collectionName = null,
                    damageSource = resolvedSource ?: effect.damageSource
                )
            }
            is CompositeEffect -> effect.copy(
                effects = effect.effects.map { resolveContextTargets(it, context, state) }
            )
            is GatedEffect -> {
                // Former MayEffect shape: resolve ContextTargets inside the optional `then` payoff,
                // exactly as MayEffect did. Other gate shapes (MayPay / WhenCondition) were never
                // resolved here, so leave them untouched.
                if (effect.gate is Gate.MayDecide && effect.otherwise == null) {
                    val inner = resolveContextTargets(effect.then, context, state)
                    if (inner !== effect.then) effect.copy(then = inner) else effect
                } else {
                    effect
                }
            }
            else -> effect
        }
    }
}
