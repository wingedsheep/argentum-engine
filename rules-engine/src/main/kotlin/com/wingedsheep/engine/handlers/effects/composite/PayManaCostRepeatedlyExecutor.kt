package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PayManaCostRepeatedlyContinuation
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.PayManaCostRepeatedlyEffect
import kotlin.reflect.KClass

/**
 * Executor for [PayManaCostRepeatedlyEffect] — "pay {1} up to three times" / "…any number of
 * times", the repeatable optional payment whose payoff scales with the repetition count.
 *
 * Two steps, one prompt:
 *  1. [affordableRepetitions] computes how many repetitions the payer can actually make, testing
 *     `cost * n` color-aware through the same auto-tap predicate the payment itself uses
 *     ([canAutoPayManaCost]) rather than dividing total mana by the mana value — `{G}` three times
 *     needs three *green*, and three Mountains can't pay it.
 *  2. The payer names a number in `1..cap`; the whole `cost * n` is then paid in one auto-tapped
 *     payment ([payManaCostFromPool]) and `n` is written into the pipeline under
 *     [PayManaCostRepeatedlyEffect.storeCountAs].
 *
 * The floor is 1, not 0: declining belongs to the wrapper (`ReflexiveTriggerEffect(optional =
 * true)`, `Gate.MayPay`), not to this effect, so the two questions stay one "do you want to?" and
 * one "how many?" instead of a redundant pair of decline paths. Consistently, a payer who cannot
 * afford even one repetition gets a *failure*, which is what stops a CR 603.12 reflexive trigger
 * from firing on a payment that never happened — the same contract `PayExactCountersEffect` has,
 * and `ReflexiveTriggerEffectExecutor.isActionFeasible` scores it up front so the may-question is
 * never raised in that case.
 *
 * A cap of exactly 1 has no question to ask (the payer has already consented, and 1 is the only
 * legal count), so it pays synchronously without a prompt.
 */
class PayManaCostRepeatedlyExecutor(
    private val cardRegistry: CardRegistry,
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<PayManaCostRepeatedlyEffect> {

    override val effectType: KClass<PayManaCostRepeatedlyEffect> = PayManaCostRepeatedlyEffect::class

    override fun execute(
        state: GameState,
        effect: PayManaCostRepeatedlyEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = context.controllerId
        val cap = affordableRepetitions(state, playerId, effect.cost, effect.maxTimes, cardRegistry, predicateEvaluator = predicateEvaluator)

        if (cap <= 0) {
            return EffectResult.error(state, "Cannot pay ${effect.cost} even once")
        }

        if (cap == 1) {
            val paid = payManaCostFromPool(state, playerId, effect.cost, cardRegistry, predicateEvaluator = predicateEvaluator)
            if (paid.error != null) return paid
            return paid.copy(updatedStoredNumbers = paid.updatedStoredNumbers + (effect.storeCountAs to 1))
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val continuation = PayManaCostRepeatedlyContinuation(
            playerId = playerId,
            cost = effect.cost,
            maxTimes = cap,
            storeCountAs = effect.storeCountAs
        )

        val decisionResult = decisionHandler.createNumberDecision(
            state = state,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "How many times do you want to pay ${effect.cost}? (1-$cap)",
            minValue = 1,
            maxValue = cap,
            phase = DecisionPhase.RESOLUTION,
            answer = continuation
        )
        val decision = decisionResult.pendingDecision
            ?: return EffectResult.error(state, "Failed to create repeat-count decision")

        return EffectResult.propagatePause(
            decisionResult.state,
            decisionResult.events
        )
    }

    companion object {
        /**
         * How many times [player] could pay [cost] back to back right now, capped by [maxTimes].
         *
         * Walks `cost * n` upward through [canAutoPayManaCost], which is what makes the answer
         * color-aware; the walk is bounded by [maxTimes] when set and otherwise by the payer's
         * total available mana (no repetition of a non-empty cost can be free, so that bound can
         * never cut a reachable count short).
         *
         * The probe is [canAutoPayManaCost] and **not** [ManaSolver.canPay] on purpose: the payment
         * is [payManaCostFromPool], which auto-taps and so refuses to spend "extras" (sacrificing a
         * Treasure, Springleaf Drum). Capping with `canPay` would count those and offer a count the
         * payment then fails on. The two must agree, so both go through the same predicate.
         *
         * A free cost (`{0}`) is payable any number of times, so an uncapped effect just reports
         * the mana-count bound — a degenerate shape that should carry an explicit [maxTimes].
         */
        fun affordableRepetitions(
            state: GameState,
            player: EntityId,
            cost: ManaCost,
            maxTimes: Int?,
            cardRegistry: CardRegistry,
            predicateEvaluator: PredicateEvaluator
        ): Int {
            val solver = ManaSolver(cardRegistry, predicateEvaluator = predicateEvaluator)
            // Hoisted once and threaded through every probe: nothing about the battlefield changes
            // between them, and the walk is O(bound) solver runs otherwise.
            val sources = solver.findAvailableManaSources(state, player)
            val bound = maxTimes ?: solver.getAvailableManaCount(state, player, sources)
            if (bound <= 0) return 0
            var paid = 0
            while (paid < bound &&
                canAutoPayManaCost(state, player, cost * (paid + 1), cardRegistry, sources, predicateEvaluator = predicateEvaluator)
            ) {
                paid++
            }
            return paid
        }
    }
}
