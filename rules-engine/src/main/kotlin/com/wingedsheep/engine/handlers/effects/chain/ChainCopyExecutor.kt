package com.wingedsheep.engine.handlers.effects.chain

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.ChainCopyEffect
import com.wingedsheep.sdk.scripting.effects.CopyRecipient
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlin.reflect.KClass

/**
 * Unified executor for all Chain of X effects.
 *
 * Resolves the copy recipient, delegates the primary action to the effect
 * executor registry, then offers the chain copy to the recipient.
 */
class ChainCopyExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val targetFinder: TargetFinder,
    private val predicateEvaluator: PredicateEvaluator,
    private val manaSolver: ManaSolver
) : EffectExecutor<ChainCopyEffect> {

    override val effectType: KClass<ChainCopyEffect> = ChainCopyEffect::class

    override fun execute(
        state: GameState,
        effect: ChainCopyEffect,
        context: EffectContext
    ): EffectResult {
        // Step 1: Determine who gets the copy offer BEFORE executing the action
        val recipientPlayerId = resolveRecipient(state, effect, context)
            ?: return EffectResult.success(state)

        // Step 2: Pre-push after-action continuation (sits below any inner continuations)

        val afterActionContinuation = ChainCopyAfterActionContinuation(
            effect = effect,
            recipientPlayerId = recipientPlayerId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences
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

        if (actionResult.outcome !is Outcome.Done) {
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
                context.resolvePlayerTarget(effect.target, state)
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
    ): EffectResult {
        // Check cost prerequisites
        if (!canPayCopyCost(state, recipientPlayerId, effect.copyCost, predicateEvaluator = predicateEvaluator, manaSolver = manaSolver)) {
            return EffectResult.success(state, events)
        }

        // Check if there are valid targets for a potential copy
        val legalTargets = targetFinder.findLegalTargets(
            state, effect.copyTargetRequirement, recipientPlayerId, context.sourceId
        )
        if (legalTargets.isEmpty()) {
            return EffectResult.success(state, events)
        }

        // Build the yes/no decision
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: "this spell"

        val copyCost = effect.copyCostText?.replaceFirstChar { it.uppercase() }
        val prompt = if (copyCost == null) {
            "Copy $sourceName and choose a new target?"
        } else {
            "$copyCost to copy $sourceName and choose a new target?"
        }

        val (yesText, noText) = (copyCost ?: "Copy") to "Decline"

        val decision = { decisionId: String -> YesNoDecision(
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
        ) }

        val continuation = ChainCopyDecisionContinuation(
            effect = effect,
            copyControllerId = recipientPlayerId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation, events = events + emptyList()))
    }

    companion object {
        /**
         * Check if the recipient can pay the copy cost.
         */
        fun canPayCopyCost(
            state: GameState,
            playerId: EntityId,
            cost: PayCost?,
            predicateEvaluator: PredicateEvaluator,
            manaSolver: ManaSolver
        ): Boolean {
            if (cost == null) return true
            return when (val atom = (cost as? PayCost.Atom)?.atom) {
                // The broad "payable at all" check: the payment window lets the payer activate any
                // mana ability (CR 605.3a), not just the ones auto-pay would tap.
                is CostAtom.Mana -> manaSolver.canPay(state, playerId, atom.cost)
                is CostAtom.Sacrifice -> {
                    findMatchingPermanents(state, playerId, atom.filter, predicateEvaluator = predicateEvaluator).size >= atom.count
                }
                is CostAtom.Discard -> {
                    val handZone = ZoneKey(playerId, Zone.HAND)
                    val hand = state.getZone(handZone)
                    hand.size >= atom.count
                }
                else -> true
            }
        }

        fun findMatchingPermanents(
            state: GameState,
            controllerId: EntityId,
            filter: GameObjectFilter,
            predicateEvaluator: PredicateEvaluator
        ): List<EntityId> {
            return BattlefieldFilterUtils.findMatchingOnBattlefield(
                state, filter.youControl(), PredicateContext(controllerId = controllerId),
                predicateEvaluator = predicateEvaluator
            )
        }
    }
}
