package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DamageAndChainCopyEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for DamageAndChainCopyEffect.
 *
 * Deals damage to any target, then that player or that permanent's controller
 * may discard a card. If they do, they may copy the spell and choose a new target.
 * Used for Chain of Plasma.
 */
class DamageAndChainCopyExecutor(
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<DamageAndChainCopyEffect> {

    override val effectType: KClass<DamageAndChainCopyEffect> = DamageAndChainCopyEffect::class

    override fun execute(
        state: GameState,
        effect: DamageAndChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context, state)
            ?: return ExecutionResult.success(state)

        // Determine "that player" — if the target is a player, it's them;
        // if a permanent, it's the controller.
        val affectedPlayerId = resolveAffectedPlayer(state, targetId)
            ?: return ExecutionResult.success(state)

        // Deal the damage
        val damageResult = dealDamageToTarget(state, targetId, effect.amount, context.sourceId)
        if (!damageResult.isSuccess) return damageResult

        var newState = damageResult.state
        val events = damageResult.events.toMutableList()

        // Check if affected player has cards in hand to discard
        val handZone = ZoneKey(affectedPlayerId, Zone.HAND)
        val hand = newState.getZone(handZone)
        if (hand.isEmpty()) {
            return ExecutionResult.success(newState, events)
        }

        // Check if there are valid targets for a potential copy
        val requirement = AnyTarget()
        val legalTargets = targetFinder.findLegalTargets(
            newState, requirement, affectedPlayerId, context.sourceId
        )
        if (legalTargets.isEmpty()) {
            return ExecutionResult.success(newState, events)
        }

        // Offer the affected player the option to discard a card to copy
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: effect.spellName

        val decision = YesNoDecision(
            id = decisionId,
            playerId = affectedPlayerId,
            prompt = "Discard a card to copy ${effect.spellName}?",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Discard and Copy",
            noText = "Decline"
        )

        val continuation = DamageChainCopyDecisionContinuation(
            decisionId = decisionId,
            affectedPlayerId = affectedPlayerId,
            amount = effect.amount,
            spellName = effect.spellName,
            sourceId = context.sourceId
        )

        newState = newState.withPendingDecision(decision).pushContinuation(continuation)

        return ExecutionResult.paused(
            newState,
            decision,
            events + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = affectedPlayerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Determine "that player" — if the target is a player entity, return it directly.
     * If it's a permanent, return its controller.
     */
    private fun resolveAffectedPlayer(state: GameState, targetId: EntityId): EntityId? {
        val entity = state.getEntity(targetId) ?: return null
        // If it's a player entity (has no ControllerComponent but IS a player), return it
        return if (entity.get<ControllerComponent>() != null) {
            // It's a permanent — return its controller
            entity.get<ControllerComponent>()!!.playerId
        } else {
            // It's a player entity
            targetId
        }
    }
}
