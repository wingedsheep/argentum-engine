package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.PayLifeEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * One executor for the [GatedEffect] frame — the unified replacement for the bespoke
 * may / optional-cost executors. It owns the canonical resolution order for every
 * decision-driven [Gate]:
 *
 *  1. Resolve the decision-maker (defaults to the ability's controller).
 *  2. [Gate.MayPay] only: skip the prompt entirely when the cost is unaffordable, falling
 *     through to [GatedEffect.otherwise] — asking yes/no for an unpayable cost is a UX trap
 *     that can also silently drop pieces of a composite cost.
 *  3. Pause with a [YesNoDecision]. The [GatedEffectContinuation] carries the *already-locked*
 *     targets in its [EffectContext], so a targeted [GatedEffect.then] resolves against the
 *     trigger-time target (CR 603.3d) instead of re-choosing one at resolution.
 *
 * The "yes" branch is consumed by `EffectAndTriggerContinuationResumer.resumeGatedEffect`;
 * the gate kind decides *how* — run [GatedEffect.then] directly ([Gate.MayDecide]) or pay the
 * cost first ([Gate.MayPay]).
 */
class GatedEffectExecutor(
    private val cardRegistry: CardRegistry,
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<GatedEffect> {

    override val effectType: KClass<GatedEffect> = GatedEffect::class

    private val manaSolver = ManaSolver(cardRegistry)
    private val conditionEvaluator = ConditionEvaluator()

    override fun execute(
        state: GameState,
        effect: GatedEffect,
        context: EffectContext
    ): EffectResult {
        val gate = effect.gate

        // Gate.WhenCondition: a synchronous state test, not a decision — no prompt, no pause.
        // Evaluate through the same ConditionEvaluationContext used everywhere else (so it reads
        // identically at resolution and under projection) and run `then` / `otherwise` directly.
        if (gate is Gate.WhenCondition) {
            val otherwise = effect.otherwise
            return when {
                conditionEvaluator.evaluate(state, gate.condition, context) ->
                    effectExecutor(state, effect.then, context)
                otherwise != null -> effectExecutor(state, otherwise, context)
                else -> EffectResult.success(state)
            }
        }

        // Gate.MayDecide: two cases where the former MayEffect skipped the prompt entirely.
        if (gate is Gate.MayDecide) {
            // Source must still be in its required zone (e.g. a dies-trigger "may" whose source
            // has since left) — otherwise the may-action is impossible, so skip silently.
            if (gate.sourceRequiredZone != null && context.sourceId != null) {
                val inRequiredZone = state.zones.any { (zoneKey, entities) ->
                    zoneKey.zoneType == gate.sourceRequiredZone && context.sourceId in entities
                }
                if (!inRequiredZone) return EffectResult.success(state)
            }
            // A ChooseActionEffect payoff with no feasible choice — don't ask the may question at all.
            val then = effect.then
            if (then is ChooseActionEffect &&
                then.choices.none { checkFeasibility(state, context.controllerId, it.feasibilityCheck) }
            ) {
                return EffectResult.success(state)
            }
        }

        val playerId = effect.decisionMaker
            ?.let { TargetResolutionUtils.resolvePlayerTarget(it, context, state) }
            ?: context.controllerId

        // Gate.MayPay: don't offer an impossible "yes" — fall straight through to `otherwise`.
        if (gate is Gate.MayPay && !canAfford(state, playerId, gate.cost)) {
            return effect.otherwise
                ?.let { effectExecutor(state, it, context) }
                ?: EffectResult.success(state)
        }

        // An optional *mana* payment (the lowered MayPayManaEffect shape) keeps its bespoke UX —
        // a "Pay {cost}?" yes/no that, on "yes", routes through the mana-source-selection
        // continuations rather than the generic auto-tapping cost composite. See [OptionalManaPayment].
        effect.asOptionalManaPayment()?.let { mana ->
            return executeOptionalManaPayment(state, playerId, mana.cost, mana.then, context)
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val hint = when (gate) {
            is Gate.MayDecide -> gate.hint ?: effect.hint
            is Gate.MayPay -> effect.hint
            is Gate.WhenCondition -> effect.hint // unreachable: handled by the synchronous branch above
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = effect.description,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION,
                triggeringEntityId = context.triggeringEntityId,
                inlineOnTrigger = (gate as? Gate.MayDecide)?.inlineOnTrigger ?: false
            ),
            yesText = "Yes",
            noText = "No",
            hint = hint
        )

        val continuation = GatedEffectContinuation(
            decisionId = decisionId,
            gate = gate,
            then = effect.then,
            otherwise = effect.otherwise,
            effectContext = context
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
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * The optional-mana-payment yes/no — formerly `MayPayManaExecutor`. Affordability is already
     * checked by the [canAfford] pre-pass above, so this only builds the "Pay {cost}?" prompt and a
     * [MayPayManaContinuation]; the existing mana-payment resumer then either spends floating mana or
     * pauses for manual mana-source selection before running [then].
     */
    private fun executeOptionalManaPayment(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        then: Effect,
        context: EffectContext
    ): EffectResult {
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay $manaCost?",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION,
                triggeringEntityId = context.triggeringEntityId
            ),
            yesText = "Pay $manaCost",
            noText = "Don't pay"
        )

        val continuation = MayPayManaContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceName = sourceName,
            manaCost = manaCost,
            effect = then,
            effectContext = context
        )

        val stateWithContinuation = state.withPendingDecision(decision).pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Whether [playerId] can pay [cost] right now. Mirrors the former
     * `OptionalCostEffectExecutor`: recognizes the payment primitives that appear in a
     * [Gate.MayPay] cost slot ([PayManaCostEffect], [PayLifeEffect], and a [CompositeEffect]
     * composing them). Unknown shapes fail open (assumed payable) so exotic cost pipelines
     * still prompt and abort later via the resumer's `stopOnError` composite.
     */
    private fun canAfford(state: GameState, playerId: EntityId, cost: Effect): Boolean =
        when (cost) {
            is PayManaCostEffect -> manaSolver.canPay(state, playerId, cost.cost)
            is PayLifeEffect -> {
                val life = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
                life >= cost.amount
            }
            is CompositeEffect -> cost.effects.all { canAfford(state, playerId, it) }
            else -> true
        }
}
