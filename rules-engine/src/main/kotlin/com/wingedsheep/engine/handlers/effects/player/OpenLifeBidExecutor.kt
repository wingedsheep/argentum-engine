package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.OpenLifeBidContinuation
import com.wingedsheep.engine.core.OpenLifeBidStage
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.OpenLifeBidEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlin.reflect.KClass

/**
 * Executor for [OpenLifeBidEffect] (Mages' Contest).
 *
 * Resolves the other bidder from the effect's [OpenLifeBidEffect.participant] reference, then
 * hands stepping/resolution to [OpenLifeBidLogic] (shared with the resumer). The caster opens
 * with a bid of 1 and the participant is asked first. If the participant resolves to the caster
 * (or to nobody), the caster is the sole bidder and wins immediately at the opening bid.
 */
class OpenLifeBidExecutor(
    private val executeEffect: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<OpenLifeBidEffect> {

    override val effectType: KClass<OpenLifeBidEffect> = OpenLifeBidEffect::class

    override fun execute(
        state: GameState,
        effect: OpenLifeBidEffect,
        context: EffectContext
    ): EffectResult {
        val casterId = context.controllerId

        // The other bidder, resolved from the participant player reference (e.g. the controller
        // of the targeted spell for Mages' Contest). The caster is never their own opponent.
        val otherBidder = TargetResolutionUtils
            .resolvePlayerTargets(EffectTarget.PlayerRef(effect.participant), state, context)
            .firstOrNull { it != casterId }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // You open the bidding with a bid of 1.
        return EffectResult.from(
            if (otherBidder == null) {
                // No opponent in the bid — you're the only bidder and win at the opening bid.
                OpenLifeBidLogic.resolve(
                    state, casterId, highBidder = casterId, highBid = 1,
                    onWin = effect.onWin, targets = context.targets,
                    sourceId = context.sourceId, executeEffect = executeEffect
                )
            } else {
                OpenLifeBidLogic.advance(
                    state, casterId, highBidder = casterId, highBid = 1,
                    bidderToAsk = otherBidder, onWin = effect.onWin,
                    targets = context.targets, sourceId = context.sourceId, sourceName = sourceName,
                    executeEffect = executeEffect
                )
            }
        )
    }
}

/**
 * Shared stepping/resolution logic for the open life-bid auction, used by both
 * [OpenLifeBidExecutor] (which sets up the first decision) and the continuation resumer (which
 * drives subsequent decisions and resolution).
 *
 * Bids are capped at the bidding player's current life total — a player who cannot exceed the
 * high bid simply can't top it and the auction ends.
 */
object OpenLifeBidLogic {

    private val decisionHandler = DecisionHandler()

    private fun lifeOf(state: GameState, playerId: EntityId): Int =
        state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0

    /**
     * Ask [bidderToAsk] whether to top the current [highBid], or resolve the auction if they
     * cannot exceed it.
     */
    fun advance(
        state: GameState,
        casterId: EntityId,
        highBidder: EntityId,
        highBid: Int,
        bidderToAsk: EntityId,
        onWin: Effect,
        targets: List<ChosenTarget>,
        sourceId: EntityId?,
        sourceName: String?,
        executeEffect: (GameState, Effect, EffectContext) -> EffectResult
    ): ExecutionResult {
        // A player can only top if they can bid strictly more than the high bid.
        if (lifeOf(state, bidderToAsk) <= highBid) {
            return resolve(state, casterId, highBidder, highBid, onWin, targets, sourceId, executeEffect)
        }

        val decisionResult = decisionHandler.createYesNoDecision(
            state = state,
            playerId = bidderToAsk,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = "The high bid is $highBid life. Pay more life to top it?",
            yesText = "Top the bid",
            noText = "Pass"
        )

        val continuation = OpenLifeBidContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            casterId = casterId,
            highBidder = highBidder,
            highBid = highBid,
            bidderToAsk = bidderToAsk,
            stage = OpenLifeBidStage.AWAITING_TOP_DECISION,
            onWin = onWin,
            targets = targets,
            sourceId = sourceId,
            sourceName = sourceName
        )

        return ExecutionResult.paused(
            decisionResult.state.pushContinuation(continuation),
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Ask [OpenLifeBidContinuation.bidderToAsk] for the amount to top the high bid by.
     */
    fun askAmount(state: GameState, continuation: OpenLifeBidContinuation): ExecutionResult {
        val maxBid = lifeOf(state, continuation.bidderToAsk)
        val decisionResult = decisionHandler.createNumberDecision(
            state = state,
            playerId = continuation.bidderToAsk,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            prompt = "Bid more than ${continuation.highBid} life (up to $maxBid)",
            minValue = continuation.highBid + 1,
            maxValue = maxBid
        )

        val newContinuation = continuation.copy(
            decisionId = decisionResult.pendingDecision!!.id,
            stage = OpenLifeBidStage.AWAITING_BID_AMOUNT
        )

        return ExecutionResult.paused(
            decisionResult.state.pushContinuation(newContinuation),
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * End the auction: the high bidder loses [highBid] life, and if the caster is the high
     * bidder, [onWin] runs against the original [targets].
     */
    fun resolve(
        state: GameState,
        casterId: EntityId,
        highBidder: EntityId,
        highBid: Int,
        onWin: Effect,
        targets: List<ChosenTarget>,
        sourceId: EntityId?,
        executeEffect: (GameState, Effect, EffectContext) -> EffectResult
    ): ExecutionResult {
        val events = mutableListOf<GameEvent>()

        // The high bidder loses life equal to the high bid (routed through the life-loss
        // executor so prevention/replacement effects apply uniformly).
        val loseLifeContext = EffectContext(sourceId = sourceId, controllerId = highBidder)
        val lifeResult = executeEffect(
            state,
            LoseLifeEffect(DynamicAmount.Fixed(highBid), EffectTarget.PlayerRef(Player.You)),
            loseLifeContext
        )
        if (lifeResult.error != null) return lifeResult.toExecutionResult()
        var currentState = lifeResult.state
        events.addAll(lifeResult.events)

        // If you win the bidding, apply the payoff (counter the spell) against the targets.
        if (highBidder == casterId) {
            val winContext = EffectContext(
                sourceId = sourceId,
                controllerId = casterId,
                targets = targets
            )
            val winResult = executeEffect(currentState, onWin, winContext)
            currentState = winResult.state
            events.addAll(winResult.events)
            if (winResult.pendingDecision != null) {
                return ExecutionResult.paused(currentState, winResult.pendingDecision, events)
            }
            if (winResult.error != null) return winResult.toExecutionResult()
        }

        return ExecutionResult(currentState, events)
    }
}
