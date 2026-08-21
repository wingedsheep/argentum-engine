package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ChooseEvidenceAmountContinuation
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.CollectEvidenceChosenAmountEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [CollectEvidenceChosenAmountEffect] — "collect evidence **X**", where X is a number
 * the player picks as the effect resolves (Incinerator of the Guilty).
 *
 * Two hops, and the order matters:
 *
 *  1. **Pick X**, from a [ChooseNumberDecision] bounded at the graveyard's total mana value. That
 *     ceiling *is* CR 701.59b, moved from the payment to the choice: an X the player couldn't then
 *     reach is never offered rather than being offered and failing. The floor is 0 — collecting
 *     evidence 0 exiles nothing but still counts as collecting (2024-02-02 ruling), which is also
 *     why this effect is always feasible and any enclosing "may" is always shown.
 *  2. **Pick the cards**, handed off to the ordinary [CollectEvidenceContinuation] path in
 *     [com.wingedsheep.engine.handlers.continuations.ChooseEvidenceAmountContinuationResumer] so
 *     the exile itself still runs through [CollectEvidenceResolver] — one reachability gate, one
 *     legality rule, one [com.wingedsheep.engine.core.EvidenceCollectedEvent], shared with every
 *     cost context.
 *
 * Deriving X from whichever cards the player exiled would be the tempting one-hop shortcut and is
 * wrong: over-exiling is legal (CR 701.59a), so it would silently raise X above what the player
 * chose.
 *
 * An empty graveyard skips the prompt entirely — X can only be 0 — and stores 0.
 */
class CollectEvidenceChosenAmountExecutor : EffectExecutor<CollectEvidenceChosenAmountEffect> {

    override val effectType: KClass<CollectEvidenceChosenAmountEffect> =
        CollectEvidenceChosenAmountEffect::class

    override fun execute(
        state: GameState,
        effect: CollectEvidenceChosenAmountEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = TargetResolutionUtils.resolvePlayerRef(effect.player, context, state)
            ?: return EffectResult.error(
                state,
                "CollectEvidenceChosenAmount: could not resolve collecting player"
            )

        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: "Collect evidence"

        val maxAmount = CollectEvidenceResolver.candidates(state, playerId).totalManaValue
        if (maxAmount <= 0) {
            // Nothing to choose between: X can only be 0, which exiles nothing. Still a
            // collection, so hand 0 downstream rather than failing.
            return EffectResult(
                state,
                updatedStoredNumbers = mapOf(effect.storeAmountAs to 0),
            )
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseNumberDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Collect evidence X — choose X (0-$maxAmount)",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            minValue = 0,
            maxValue = maxAmount
        )

        val continuation = ChooseEvidenceAmountContinuation(
            decisionId = decisionId,
            playerId = playerId,
            storeAmountAs = effect.storeAmountAs,
            sourceName = sourceName,
        )

        return EffectResult.paused(
            state.withPendingDecision(decision).pushContinuation(continuation),
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "CHOOSE_NUMBER",
                    prompt = decision.prompt
                )
            )
        )
    }
}
