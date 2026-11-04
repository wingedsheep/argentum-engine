package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.DamageUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.lkiSnapshotFor
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for DealDamageEffect.
 * Handles both fixed and dynamic damage amounts, and both single-target and
 * multi-player targets (e.g., PlayerRef(Player.Each), PlayerRef(Player.EachOpponent)).
 */
class DealDamageExecutor(
    private val zones: ZoneTransitionService,
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<DealDamageEffect> {

    override val effectType: KClass<DealDamageEffect> = DealDamageEffect::class

    override fun execute(
        state: GameState,
        effect: DealDamageEffect,
        context: EffectContext
    ): EffectResult {
        return executeSimultaneously(state, effect, listOf(context), effect, context)
    }

    /** Prepare every member against the same state, then replace and commit one damage event. */
    fun executeSimultaneously(
        state: GameState,
        effect: DealDamageEffect,
        contexts: List<EffectContext>,
        replayEffect: com.wingedsheep.sdk.scripting.effects.Effect,
        replayContext: EffectContext,
        aggregateExcess: Boolean = false
    ): EffectResult {
        val instances = contexts.flatMap { prepare(state, effect, it) }
        val result = if (instances.isEmpty()) EffectResult.success(state) else
            DamageReplacementChain.deal(zones, state, instances, replayEffect, replayContext, effect.excessToController)
        if (result.outcome is Outcome.Paused) return result
        val damage = result.events.filterIsInstance<DamageDealtEvent>()
        var numbers = result.updatedStoredNumbers
        effect.damageDealtVariable?.let { numbers = numbers + (it to damage.sumOf { event -> event.amount }) }
        effect.excessDamageVariable?.let { name ->
            val recipients = (if (aggregateExcess) instances else instances.take(1)).map { it.targetId }.toSet()
            numbers = numbers + (name to damage.filter { it.targetId in recipients }.sumOf { it.excessAmount })
        }
        return result.copy(updatedStoredNumbers = numbers)
    }

    private fun prepare(
        state: GameState,
        effect: DealDamageEffect,
        context: EffectContext
    ): List<DamageReplacementChain.Instance> {
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return emptyList()
        }

        // Use damageSource override if specified (e.g., EnchantedCreature for Lavamancer's Skill)
        val damageSourceTarget = effect.damageSource
        val sourceId = if (damageSourceTarget != null) {
            // The spell text named a specific source ("it deals damage" — Diplomatic Relations,
            // "enchanted creature deals damage" — Lavamancer's Skill). If that source can no
            // longer be resolved at resolution time (e.g. the FROM target died to a response
            // and was dropped by 608.2b validation), the whole damage instruction is skipped
            // per CR 608.2b — we don't fall back to the ability's source permanent. This is a
            // legal no-op fizzle, not an error: the surrounding effect (e.g. Composite) resolves.
            //
            // A named object reference that has merely left the battlefield is different: "it
            // deals that much damage" (Wrathful Red Dragon, Warstorm Surge) still has that object
            // deal the damage, using its last-known information (CR 608.2h), exactly as an
            // ability whose own source died still deals its damage.
            context.resolveTarget(damageSourceTarget, state)
                ?: departedObjectSource(damageSourceTarget, state, context)?.first
                ?: return emptyList()
        } else {
            context.sourceId
        }

        // The identity "a source of your choice" shields compare against: the object as it was
        // when it was chosen — for a departed source, its battlefield incarnation.
        val damageSourceRef = when (damageSourceTarget) {
            null -> context.objectReferences.origin ?: sourceId?.let(state::objectRef)
            else -> departedObjectSource(damageSourceTarget, state, context)?.second
                ?: sourceId?.let(state::objectRef)
        }

        // Resolve characteristics by object identity, never by a returned card's newer visit.
        val sourceReference = (damageSourceTarget ?: EffectTarget.Self) as? EffectTarget.SingleEntity
        val sourceSnapshot = sourceId?.let { id ->
            // A spell resolving on the stack is still its current object. Its cast-time
            // snapshot must not hide keywords or colors granted since it was cast.
            val departed = if (damageSourceRef != null) !state.isCurrentObject(damageSourceRef)
            else state.getEntity(id) == null || (id !in state.getBattlefield() &&
                state.getEntity(id)?.get<LastKnownPermanentComponent>() != null)
            if (!departed) null else {
                state.getEntity(id)?.get<LastKnownPermanentComponent>()?.snapshot
                    ?.takeIf { damageSourceRef == null || it.objectRef == damageSourceRef }
                    ?: sourceReference?.let { context.lkiSnapshotFor(it, id) }
                        ?.takeIf { damageSourceRef == null || it.objectRef == null || it.objectRef == damageSourceRef }
            }
        }

        val recipients = when (effect.target) {
            EffectTarget.EachDamagedBySourceThisGame -> resolveDamagedThisGame(state, sourceId, context)
            is EffectTarget.PlayerRef -> context.resolvePlayerTargets(effect.target, state)
            else -> listOfNotNull(context.resolveTarget(effect.target, state))
        }
        return recipients.map { DamageReplacementChain.Instance(sourceId, it, amount,
            cantBePrevented = effect.cantBePrevented, sourceRef = damageSourceRef, sourceSnapshot = sourceSnapshot) }
    }

    /**
     * The object a non-targeted [reference] ("it", "that creature", this permanent) named before it
     * left the battlefield, so it can still be the source of the damage by last-known information.
     * Null for anything else — a dropped target stays a no-op (CR 608.2b).
     */
    private fun departedObjectSource(
        reference: EffectTarget,
        state: GameState,
        context: EffectContext
    ): Pair<com.wingedsheep.sdk.model.EntityId, ObjectRef?>? {
        if (reference !is EffectTarget.SingleEntity) return null
        if (!TargetResolutionUtils.isLostObject(reference, context, state)) return null
        val entityId = TargetResolutionUtils.resolveEntity(reference, context, state) ?: return null
        val references = context.objectReferences
        val capturedRef = when (reference) {
            EffectTarget.Self -> references.source
            EffectTarget.TriggeringEntity -> references.triggering
            EffectTarget.IterationEntity -> references.iteration?.objectRef
            is EffectTarget.SpecificEntity -> references.bound.firstOrNull { it.entityId == entityId }?.objectRef
            else -> null
        }
        return entityId to capturedRef
    }

    /**
     * The recipients still eligible to be hit again: opponents of the source's controller, plus
     * planeswalkers still on the battlefield. A recorded player who has since left the game, and a
     * planeswalker that has since died, are dropped — the memory identifies them, it doesn't
     * resurrect them. Ordered deterministically by the recorded set's iteration order.
     */
    private fun resolveDamagedThisGame(
        state: GameState,
        sourceId: com.wingedsheep.sdk.model.EntityId?,
        context: EffectContext
    ): List<com.wingedsheep.sdk.model.EntityId> {
        val sourceEntity = sourceId?.let(state::getEntity) ?: return emptyList()
        val recorded = sourceEntity
            .get<com.wingedsheep.engine.state.components.battlefield.DealtDamageToThisGameComponent>()
            ?.recipientIds
            ?: return emptyList()
        val opponents = state.getOpponents(context.controllerId).toSet()
        val battlefield = state.getBattlefield().toSet()
        val projected = state.projectedState
        return recorded.filter { it in opponents || (it in battlefield && projected.isPlaneswalker(it)) }
    }
}
