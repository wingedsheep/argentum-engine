package com.wingedsheep.engine.handlers.effects.chain

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.ChainCopyEffect
import com.wingedsheep.sdk.scripting.effects.CopyRecipient
import com.wingedsheep.sdk.scripting.effects.Effect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Unified executor for all Chain of X effects.
 *
 * Resolves the copy recipient, delegates the primary action to the effect
 * executor registry, then offers the chain copy to the recipient.
 */
class ChainCopyExecutor(
    private val targetFinder: TargetFinder = TargetFinder(),
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<ChainCopyEffect> {

    override val effectType: KClass<ChainCopyEffect> = ChainCopyEffect::class

    override fun execute(
        state: GameState,
        effect: ChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        // Step 1: Determine who gets the copy offer BEFORE executing the action
        val recipientPlayerId = resolveRecipient(state, effect, context)
            ?: return ExecutionResult.success(state)

        // Step 2: Pre-push after-action continuation (sits below any inner continuations)
        val afterActionContinuation = ChainCopyAfterActionContinuation(
            decisionId = "chain-after-action-${UUID.randomUUID()}",
            effect = effect,
            recipientPlayerId = recipientPlayerId,
            sourceId = context.sourceId
        )
        val stateWithContinuation = state.pushContinuation(afterActionContinuation)

        // Step 3: Execute the inner action via the registry
        val actionResult = effectExecutor(stateWithContinuation, effect.action, context)

        if (actionResult.pendingDecision != null) {
            // Inner action paused (e.g., discard card selection).
            // The inner continuation sits on top, our after-action continuation below.
            // After the inner decisions resolve, the auto-resumer will fire offerChainCopy.
            return actionResult
        }

        // Step 4: Inner action completed (success or error) — pop the unused after-action continuation
        val (_, stateAfterPop) = actionResult.state.popContinuation()

        if (!actionResult.isSuccess) {
            return actionResult.copy(state = stateAfterPop)
        }

        // Step 5: Inner action succeeded immediately — offer the chain copy
        return offerChainCopy(
            stateAfterPop, actionResult.events.toMutableList(),
            recipientPlayerId, effect, context
        )
    }

    /**
     * Determine who gets offered the copy based on the effect's [CopyRecipient]
     * and the resolved target.
     */
    private fun resolveRecipient(
        state: GameState,
        effect: ChainCopyEffect,
        context: EffectContext
    ): EntityId? {
        return when (effect.copyRecipient) {
            CopyRecipient.TARGET_CONTROLLER -> {
                val targetId = context.resolveTarget(effect.target)
                    ?: return null
                val projected = state.projectedState
                projected.getController(targetId)
                    ?: state.getEntity(targetId)?.get<CardComponent>()?.ownerId
            }
            CopyRecipient.TARGET_PLAYER -> {
                context.resolvePlayerTarget(effect.target)
            }
            CopyRecipient.AFFECTED_PLAYER -> {
                val targetId = context.resolveTarget(effect.target, state)
                    ?: return null
                resolveAffectedPlayer(state, targetId)
            }
        }
    }

    private fun resolveAffectedPlayer(state: GameState, targetId: EntityId): EntityId? {
        val entity = state.getEntity(targetId) ?: return null
        val controller = entity.get<ControllerComponent>()
        return controller?.playerId ?: targetId
    }

    // =========================================================================
    // Chain copy offer logic
    // =========================================================================

    fun offerChainCopy(
        state: GameState,
        events: MutableList<GameEvent>,
        recipientPlayerId: EntityId,
        effect: ChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        // Check cost prerequisites
        if (!canPayCopyCost(state, recipientPlayerId, effect.copyCost)) {
            return ExecutionResult.success(state, events)
        }

        // Check if there are valid targets for a potential copy
        val legalTargets = targetFinder.findLegalTargets(
            state, effect.copyTargetRequirement, recipientPlayerId, context.sourceId
        )
        if (legalTargets.isEmpty()) {
            return ExecutionResult.success(state, events)
        }

        // Build the yes/no decision
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: effect.spellName

        val copyCost = effect.copyCost
        val prompt = if (copyCost == null) {
            "Copy ${effect.spellName} and choose a new target?"
        } else {
            "${copyCost.description.replaceFirstChar { it.uppercase() }} to copy ${effect.spellName} and choose a new target?"
        }

        val (yesText, noText) = if (copyCost == null) {
            "Copy" to "Decline"
        } else {
            copyCost.description.replaceFirstChar { it.uppercase() } to "Decline"
        }

        val decision = YesNoDecision(
            id = decisionId,
            playerId = recipientPlayerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = yesText,
            noText = noText
        )

        val continuation = ChainCopyDecisionContinuation(
            decisionId = decisionId,
            effect = effect,
            copyControllerId = recipientPlayerId,
            sourceId = context.sourceId
        )

        val newState = state.withPendingDecision(decision).pushContinuation(continuation)

        return ExecutionResult.paused(
            newState,
            decision,
            events + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = recipientPlayerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    companion object {
        /**
         * Check if the recipient can pay the copy cost.
         */
        fun canPayCopyCost(state: GameState, playerId: EntityId, cost: PayCost?): Boolean {
            if (cost == null) return true
            return when (cost) {
                is PayCost.Sacrifice -> {
                    findMatchingPermanents(state, playerId, cost.filter).size >= cost.count
                }
                is PayCost.Discard -> {
                    val handZone = ZoneKey(playerId, Zone.HAND)
                    val hand = state.getZone(handZone)
                    hand.size >= cost.count
                }
                else -> true
            }
        }

        fun findMatchingPermanents(
            state: GameState,
            controllerId: EntityId,
            filter: GameObjectFilter
        ): List<EntityId> {
            return BattlefieldFilterUtils.findMatchingOnBattlefield(
                state, filter.youControl(), PredicateContext(controllerId = controllerId)
            )
        }
    }
}
