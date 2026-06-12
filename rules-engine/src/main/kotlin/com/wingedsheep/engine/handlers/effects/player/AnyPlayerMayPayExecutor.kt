package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.AnyPlayerMayPayEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for AnyPlayerMayPayEffect.
 *
 * Handles "any player may [cost]." with two branching outcomes:
 *  - As soon as any player pays, [AnyPlayerMayPayEffect.consequence] runs and no further players
 *    are asked ("any player may sacrifice…; if a player does, …" — Prowling Pangolin).
 *  - If no player pays, [AnyPlayerMayPayEffect.consequenceIfNonePaid] runs instead
 *    ("…unless any player pays N life" — Aether Rift).
 *
 * Players are asked in APNAP order; players who can't pay are skipped. Supported costs are
 * [PayCost.Sacrifice] (card selection) and [PayCost.PayLife] (yes/no). The surrounding pipeline's
 * stored collections are threaded into whichever consequence fires so it can reference cards
 * gathered earlier in the same resolution.
 */
class AnyPlayerMayPayExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val executeEffect: ((GameState, Effect, EffectContext) -> EffectResult)? = null
) : EffectExecutor<AnyPlayerMayPayEffect> {

    override val effectType: KClass<AnyPlayerMayPayEffect> = AnyPlayerMayPayEffect::class

    override fun execute(
        state: GameState,
        effect: AnyPlayerMayPayEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "No source for any player may pay effect")

        val sourceContainer = state.getEntity(sourceId)
            ?: return EffectResult.error(state, "Source entity not found")
        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Source has no card component")

        // Get players in APNAP order
        val activePlayer = state.activePlayerId
            ?: return EffectResult.error(state, "No active player")
        val playerOrder = listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }

        return askNextPlayer(state, effect, context, sourceId, sourceCard.name, playerOrder, 0)
    }

    /**
     * Ask the next player in APNAP order if they want to pay the cost.
     * Skips players who can't pay. If no one is left to ask, runs the "none paid" consequence.
     */
    private fun askNextPlayer(
        state: GameState,
        effect: AnyPlayerMayPayEffect,
        context: EffectContext,
        sourceId: EntityId,
        sourceName: String,
        playerOrder: List<EntityId>,
        currentIndex: Int
    ): EffectResult {
        // Find the next player who can pay
        var index = currentIndex
        while (index < playerOrder.size) {
            val playerId = playerOrder[index]
            if (canPlayerPay(state, playerId, effect.cost, sourceId)) {
                break
            }
            index++
        }

        // No more players can pay - run the "none paid" branch (e.g., reanimate the card).
        if (index >= playerOrder.size) {
            return runConsequence(
                state,
                effect.consequenceIfNonePaid,
                context.controllerId,
                sourceId,
                context.pipeline.storedCollections,
                context.triggeringEntityId,
                context.triggeringPlayerId
            )
        }

        val playerId = playerOrder[index]
        return when (val atom = (effect.cost as? PayCost.Atom)?.atom) {
            is CostAtom.Sacrifice -> askPlayerToSacrifice(
                state, effect, context, atom, sourceId, sourceName,
                playerId, playerOrder, index
            )
            is CostAtom.PayLife -> askPlayerToPayLife(
                state, effect, context, atom, sourceId, sourceName,
                playerId, playerOrder, index
            )
            else -> EffectResult.error(state, "Unsupported cost type for AnyPlayerMayPay: ${effect.cost::class.simpleName}")
        }
    }

    private fun canPlayerPay(
        state: GameState,
        playerId: EntityId,
        cost: PayCost,
        sourceId: EntityId
    ): Boolean {
        return when (val atom = (cost as? PayCost.Atom)?.atom) {
            is CostAtom.Sacrifice -> {
                val validPermanents = findValidPermanentsOnBattlefield(state, playerId, atom.filter, sourceId)
                validPermanents.size >= atom.count
            }
            // CR 119.4: a player may pay life only if their life total is at least the amount.
            is CostAtom.PayLife -> {
                val life = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
                life >= atom.amount
            }
            else -> false
        }
    }

    private fun askPlayerToSacrifice(
        state: GameState,
        effect: AnyPlayerMayPayEffect,
        context: EffectContext,
        cost: CostAtom.Sacrifice,
        sourceId: EntityId,
        sourceName: String,
        playerId: EntityId,
        playerOrder: List<EntityId>,
        currentIndex: Int
    ): EffectResult {
        val validPermanents = findValidPermanentsOnBattlefield(state, playerId, cost.filter, sourceId)

        val prompt = "You may sacrifice ${cost.count} ${cost.filter.description}s to cause $sourceName to be sacrificed, or skip"

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = playerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = validPermanents,
            minSelections = 0,
            maxSelections = cost.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true
        )

        val continuation = anyPlayerMayPayContinuation(
            effect, context,
            decisionId = decisionResult.pendingDecision!!.id,
            currentPlayerId = playerId,
            remainingPlayers = playerOrder.drop(currentIndex + 1),
            sourceId = sourceId,
            sourceName = sourceName,
            requiredCount = cost.count,
            filter = cost.filter
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    private fun askPlayerToPayLife(
        state: GameState,
        effect: AnyPlayerMayPayEffect,
        context: EffectContext,
        cost: CostAtom.PayLife,
        sourceId: EntityId,
        sourceName: String,
        playerId: EntityId,
        playerOrder: List<EntityId>,
        currentIndex: Int
    ): EffectResult {
        val decisionId = UUID.randomUUID().toString()
        val prompt = "Pay ${cost.amount} life to prevent $sourceName's effect?"

        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Pay ${cost.amount} life",
            noText = "Don't pay"
        )

        val continuation = anyPlayerMayPayContinuation(
            effect, context,
            decisionId = decisionId,
            currentPlayerId = playerId,
            remainingPlayers = playerOrder.drop(currentIndex + 1),
            sourceId = sourceId,
            sourceName = sourceName,
            requiredCount = cost.amount,
            filter = com.wingedsheep.sdk.scripting.GameObjectFilter.Any
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "YES_NO",
                    prompt = prompt
                )
            )
        )
    }

    private fun anyPlayerMayPayContinuation(
        effect: AnyPlayerMayPayEffect,
        context: EffectContext,
        decisionId: String,
        currentPlayerId: EntityId,
        remainingPlayers: List<EntityId>,
        sourceId: EntityId,
        sourceName: String,
        requiredCount: Int,
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter
    ): AnyPlayerMayPayContinuation = AnyPlayerMayPayContinuation(
        decisionId = decisionId,
        currentPlayerId = currentPlayerId,
        remainingPlayers = remainingPlayers,
        sourceId = sourceId,
        sourceName = sourceName,
        controllerId = context.controllerId,
        cost = effect.cost,
        consequence = effect.consequence,
        consequenceIfNonePaid = effect.consequenceIfNonePaid,
        requiredCount = requiredCount,
        filter = filter,
        storedCollections = context.pipeline.storedCollections,
        triggeringEntityId = context.triggeringEntityId,
        triggeringPlayerId = context.triggeringPlayerId
    )

    /**
     * Run one of the two consequence branches (may be null = nothing). Carries the pipeline's
     * stored collections so the effect can reference cards gathered earlier this resolution.
     */
    private fun runConsequence(
        state: GameState,
        consequence: Effect?,
        controllerId: EntityId,
        sourceId: EntityId,
        storedCollections: Map<String, List<EntityId>>,
        triggeringEntityId: EntityId? = null,
        triggeringPlayerId: EntityId? = null
    ): EffectResult {
        if (consequence == null) return EffectResult.success(state)
        val executor = executeEffect ?: return EffectResult.success(state)
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = controllerId,
            pipeline = PipelineState(storedCollections = storedCollections),
            triggeringEntityId = triggeringEntityId,
            triggeringPlayerId = triggeringPlayerId
        )
        return executor(state, consequence, context)
    }

    private fun findValidPermanentsOnBattlefield(
        state: GameState,
        playerId: EntityId,
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter,
        sourceId: EntityId
    ): List<EntityId> {
        return BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, filter.youControl(), PredicateContext(controllerId = playerId)
        )
    }
}
