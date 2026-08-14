package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.CollectEvidenceContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.CollectEvidenceEffect
import kotlin.reflect.KClass

/**
 * Executor for [CollectEvidenceEffect] — collect evidence N at resolution time (CR 701.59).
 *
 * Delegates every rules decision to [CollectEvidenceResolver], the one implementation shared with
 * the three *cost* contexts, so the effect form can't drift from them: same reachability gate, same
 * legality rule for a selection, same exile, same
 * [com.wingedsheep.engine.core.EvidenceCollectedEvent].
 *
 * The only thing this adds is the interactive step — *which* cards. It pauses for a card-selection
 * decision carrying a `minTotalManaValue` floor (the sum gate, since collect evidence constrains no
 * count) and finishes in [com.wingedsheep.engine.handlers.continuations.CollectEvidenceContinuationResumer].
 *
 * **This effect performs no yes/no of its own.** Whether to collect is the outer "may" —
 * `ReflexiveTriggerEffect`'s prompt for Sample Collector, a `GatedEffect` for Izoni. That outer gate
 * consults `ReflexiveTriggerEffectExecutor.isActionFeasible`, which asks
 * [CollectEvidenceResolver.canCollect], so CR 701.59b holds by construction: a player who can't
 * reach N is never offered the choice. Reaching this executor with an unreachable threshold means
 * the effect ran unguarded, so it no-ops rather than exiling a partial, illegal payment.
 */
class CollectEvidenceExecutor(
    private val decisionHandler: DecisionHandler
) : EffectExecutor<CollectEvidenceEffect> {

    override val effectType: KClass<CollectEvidenceEffect> = CollectEvidenceEffect::class

    override fun execute(
        state: GameState,
        effect: CollectEvidenceEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = TargetResolutionUtils.resolvePlayerRef(effect.player, context, state)
            ?: return EffectResult.error(state, "CollectEvidence: could not resolve collecting player")

        val candidates = CollectEvidenceResolver.candidates(state, playerId)
        // CR 701.59b — defense in depth. The "may" that leads here is only offered when this holds.
        if (!candidates.canReach(effect.amount)) {
            return EffectResult.success(state, emptyList())
        }

        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: "Collect evidence"

        // When the whole graveyard is exactly what's needed there is nothing to choose, so skip the
        // round-trip. Any surplus means a real choice exists (which cards to keep), so prompt.
        if (candidates.totalManaValue == effect.amount) {
            return applyCollection(state, playerId, effect.amount, candidates.cards, sourceName)
        }

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Collect evidence ${effect.amount}: exile cards with total mana value " +
                "${effect.amount} or greater from your graveyard",
            options = candidates.cards,
            minSelections = 1,
            maxSelections = candidates.cards.size,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            minTotalManaValue = effect.amount
        )

        val continuation = CollectEvidenceContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = playerId,
            amount = effect.amount,
            sourceName = sourceName,
        )

        return EffectResult.paused(
            decisionResult.state.pushContinuation(continuation),
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    private fun applyCollection(
        state: GameState,
        playerId: com.wingedsheep.sdk.model.EntityId,
        amount: Int,
        cards: List<com.wingedsheep.sdk.model.EntityId>,
        sourceName: String,
    ): EffectResult =
        when (val result = CollectEvidenceResolver.collect(state, playerId, amount, cards, sourceName)) {
            is CollectEvidenceResolver.Result.Success ->
                EffectResult.success(result.state, result.events)
            is CollectEvidenceResolver.Result.Failure ->
                EffectResult.error(state, "CollectEvidence: ${result.reason}")
        }
}
