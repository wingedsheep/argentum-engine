package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.DamageUtils.dealDamageToTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for DealDamageEffect.
 * Handles both fixed and dynamic damage amounts, and both single-target and
 * multi-player targets (e.g., PlayerRef(Player.Each), PlayerRef(Player.EachOpponent)).
 */
class DealDamageExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<DealDamageEffect> {

    override val effectType: KClass<DealDamageEffect> = DealDamageEffect::class

    override fun execute(
        state: GameState,
        effect: DealDamageEffect,
        context: EffectContext
    ): EffectResult {
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return EffectResult.success(state)
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
            context.resolveTarget(damageSourceTarget, state)
                ?: return EffectResult.success(state)
        } else {
            context.sourceId
        }

        // "Each opponent and planeswalker it has dealt damage to this game" (The Fallen): a set
        // that mixes players and permanents, read off the damage source's accumulated memory.
        // Empty is a legal no-op, not an error — a Fallen that has damaged nobody yet does nothing.
        if (effect.target is EffectTarget.EachDamagedBySourceThisGame) {
            val recipients = resolveDamagedThisGame(state, sourceId, context)
            val (readyState, pause) = OptionalDamageRedirect.beforeDealing(
                state,
                recipients.map { OptionalDamageRedirect.Instance(sourceId, it, amount) },
                effect,
                context
            )
            if (pause != null) return pause
            var newState = readyState
            val events = mutableListOf<EngineGameEvent>()
            for (recipientId in recipients) {
                val result = dealDamageToTarget(newState, recipientId, amount, sourceId, effect.cantBePrevented)
                newState = result.newState
                events.addAll(result.events)
            }
            return EffectResult.success(newState, events)
        }

        // For PlayerRef targets, resolve to potentially multiple players
        if (effect.target is EffectTarget.PlayerRef) {
            val playerIds = context.resolvePlayerTargets(effect.target, state)
            if (playerIds.isEmpty()) {
                return EffectResult.error(state, "No valid target for damage")
            }

            val (readyState, pause) = OptionalDamageRedirect.beforeDealing(
                state,
                playerIds.map { OptionalDamageRedirect.Instance(sourceId, it, amount) },
                effect,
                context
            )
            if (pause != null) return pause
            var newState = readyState
            val events = mutableListOf<EngineGameEvent>()
            for (playerId in playerIds) {
                val result = dealDamageToTarget(newState, playerId, amount, sourceId, effect.cantBePrevented)
                newState = result.newState
                events.addAll(result.events)
            }
            return EffectResult.success(newState, events)
        }

        // Single target resolution
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for damage")

        // "You may have that damage dealt to you instead" (Blood of the Martyr) — ask before dealing.
        val (readyState, pause) = OptionalDamageRedirect.beforeDealing(
            state,
            listOf(OptionalDamageRedirect.Instance(sourceId, targetId, amount)),
            effect,
            context
        )
        if (pause != null) return pause

        return dealDamageToTarget(
            readyState, targetId, amount, sourceId, effect.cantBePrevented,
            excessToController = effect.excessToController
        )
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
