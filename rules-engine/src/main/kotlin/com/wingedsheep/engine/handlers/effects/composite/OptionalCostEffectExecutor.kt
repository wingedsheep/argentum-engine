package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.PayLifeEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for OptionalCostEffect.
 * Handles "You may [cost]. If you do, [ifPaid]. Otherwise, [ifNotPaid]." effects.
 *
 * Presents a yes/no choice. If yes, executes [cost] then [ifPaid].
 * If no, executes [ifNotPaid] (if present).
 *
 * Used for Gift mechanics and similar optional cost patterns.
 */
class OptionalCostEffectExecutor(
    private val cardRegistry: CardRegistry,
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<OptionalCostEffect> {

    override val effectType: KClass<OptionalCostEffect> = OptionalCostEffect::class

    private val manaSolver = ManaSolver(cardRegistry)

    override fun execute(
        state: GameState,
        effect: OptionalCostEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = context.controllerId

        // If the player can't actually pay the cost, skip the prompt entirely
        // and fall through to ifNotPaid (or no-op). Asking yes/no for an
        // unpayable cost is a UX trap that can also silently skip pieces of
        // a composite cost (e.g. paying life when mana can't be paid).
        if (!canAfford(state, playerId, effect.cost)) {
            return effect.ifNotPaid
                ?.let { effectExecutor(state, it, context) }
                ?: EffectResult.success(state)
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
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
                triggeringEntityId = context.triggeringEntityId
            ),
            yesText = "Yes",
            noText = "No"
        )

        // If yes: execute cost then ifPaid (stopOnError ensures cost failure aborts the payoff)
        val effectIfYes = CompositeEffect(listOf(effect.cost, effect.ifPaid), stopOnError = true)

        val continuation = MayAbilityContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceName = sourceName,
            effectIfYes = effectIfYes,
            effectIfNo = effect.ifNotPaid,
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
     * Checks whether [playerId] can pay the given cost effect right now.
     *
     * Recognizes the payment primitives that appear inside an [OptionalCostEffect]'s
     * cost slot: [PayManaCostEffect], [PayLifeEffect], and a [CompositeEffect]
     * composing them. Unknown effect shapes are assumed payable — failing open
     * preserves existing behavior for exotic cost pipelines and still lets the
     * executor abort later via `stopOnError`.
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
