package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributeDamageContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.DamageUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for DividedDamageEffect.
 *
 * "Deal X damage divided as you choose among N targets"
 *
 * Per MTG rules the division is chosen as the spell is cast / the ability is activated (CR 601.2d),
 * not when it resolves, so this executor prefers the pre-supplied `context.damageDistribution` —
 * dealing each surviving target exactly its announced share and dropping the share of any target
 * that became illegal in the meantime. Only when no division was announced (a single target, or a
 * non-interactive controller) does it deal the whole total or ask for the division at resolution.
 */
class DividedDamageExecutor(
    private val decisionHandler: DecisionHandler,
    private val amountEvaluator: com.wingedsheep.engine.handlers.DynamicAmountEvaluator =
        com.wingedsheep.engine.handlers.DynamicAmountEvaluator()
) : EffectExecutor<DividedDamageEffect> {

    override val effectType: KClass<DividedDamageEffect> = DividedDamageEffect::class

    override fun execute(
        state: GameState,
        effect: DividedDamageEffect,
        context: EffectContext
    ): EffectResult {
        // Get the targets from context
        val targets = context.targets.map { it.toEntityId() }

        // The total is fixed unless a dynamicTotal is supplied, which is evaluated at resolution
        // (e.g. Ureni — "X is the number of lands you control").
        val total = effect.dynamicTotal?.let { amountEvaluator.evaluate(state, it, context) }
            ?: effect.totalDamage

        if (targets.isEmpty()) {
            // "Any number of target" forms can resolve with zero targets — nothing happens.
            return if (effect.dynamicTotal != null) EffectResult.success(state)
            else EffectResult.error(state, "No targets for divided damage")
        }

        val distribution = context.damageDistribution
        if (distribution != null) {
            // The division was locked in when the spell/ability was announced (CR 601.2d), so it is
            // honored verbatim — never re-divided at resolution. `context.targets` has already had
            // targets that became illegal dropped (CR 608.2b); their share is simply not dealt, and
            // the survivors keep exactly what they were assigned. This is why the assigned shares
            // can sum to less than [total] and must not be recomputed from the surviving count.
            val stillLegal = targets.toSet()
            val (readyState, pause) = OptionalDamageRedirect.beforeDealing(
                state,
                distribution
                    .filter { (targetId, amount) -> amount > 0 && targetId in stillLegal }
                    .map { (targetId, amount) -> OptionalDamageRedirect.Instance(context.sourceId, targetId, amount) },
                effect,
                context
            )
            if (pause != null) return pause

            var currentState = readyState
            val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

            for ((targetId, amount) in distribution) {
                if (amount <= 0 || targetId !in stillLegal) continue
                val result = dealDamageToTarget(currentState, targetId, amount, context.sourceId)
                if (!result.isSuccess) {
                    return result
                }
                currentState = result.newState
                events.addAll(result.events)
            }

            return EffectResult.success(currentState, events)
        }

        // No division was announced. A single target takes the whole total with nothing to divide;
        // otherwise fall back to asking for the division now (the path non-interactive controllers
        // and engine-direct actions take).
        if (targets.size == 1) {
            val (readyState, pause) = OptionalDamageRedirect.beforeDealing(
                state,
                listOf(OptionalDamageRedirect.Instance(context.sourceId, targets.first(), total)),
                effect,
                context
            )
            if (pause != null) return pause
            return dealDamageToTarget(readyState, targets.first(), total, context.sourceId)
        }
        return createDistributionDecision(state, effect, context, targets, total)
    }

    /**
     * Legacy behavior: Create a DistributeDecision for backwards compatibility.
     * This should only be used if damageDistribution is not provided in context.
     */
    private fun createDistributionDecision(
        state: GameState,
        effect: DividedDamageEffect,
        context: EffectContext,
        targets: List<com.wingedsheep.sdk.model.EntityId>,
        total: Int
    ): EffectResult {
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: "Effect"

        val decisionId = UUID.randomUUID().toString()
        val decision = DistributeDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Divide $total damage among ${targets.size} targets",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            totalAmount = total,
            targets = targets,
            minPerTarget = 1 // Per MTG rules, must assign at least 1 damage to each target
        )

        // Push continuation so we know how to resume
        val continuation = DistributeDamageContinuation(
            decisionId = decisionId,
            sourceId = context.sourceId,
            controllerId = context.controllerId,
            targets = targets
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)

        val events = listOf(
            DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = context.controllerId,
                decisionType = "DISTRIBUTE",
                prompt = decision.prompt
            )
        )

        return EffectResult.paused(newState, decision, events)
    }
}
