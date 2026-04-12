package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.CounterUnlessPaysContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CounterCondition
import com.wingedsheep.sdk.scripting.effects.CounterDestination
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.CounterTarget
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import kotlin.reflect.KClass

/**
 * Unified executor for [CounterEffect].
 *
 * Handles all counter-spell and counter-ability variations by dispatching on
 * the sealed hierarchy parameters: [CounterTarget], [CounterTargetSource],
 * [CounterDestination], and [CounterCondition].
 */
class CounterEffectExecutor(
    private val amountEvaluator: DynamicAmountEvaluator,
    private val cardRegistry: CardRegistry
) : EffectExecutor<CounterEffect> {

    override val effectType: KClass<CounterEffect> = CounterEffect::class

    private val decisionHandler = DecisionHandler()
    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: CounterEffect,
        context: EffectContext
    ): EffectResult {
        // Step 1: Resolve the spell/ability entity ID
        val entityId = resolveCounterTarget(effect, context, state)
            ?: return if (effect.targetSource == CounterTargetSource.TriggeringEntity) {
                // Triggering entity no longer on stack — nothing to counter
                EffectResult.success(state)
            } else {
                EffectResult.error(state, "No valid target to counter")
            }

        // Step 2: Validate filter if present
        val filter = effect.filter
        if (filter != null) {
            val predicateContext = PredicateContext.fromEffectContext(context)
            if (!predicateEvaluator.matches(state, entityId, filter.baseFilter, predicateContext)) {
                return EffectResult.error(state, "Target does not match filter: ${filter.baseFilter.description}")
            }
        }

        // Step 3: Handle condition (unless pays) or perform counter directly
        return when (val condition = effect.condition) {
            is CounterCondition.Always -> performCounter(state, effect, entityId, context)
            is CounterCondition.UnlessPaysMana -> handleUnlessPaysMana(state, effect, entityId, condition.cost, context)
            is CounterCondition.UnlessPaysDynamic -> handleUnlessPaysDynamic(state, effect, entityId, condition, context)
        }
    }

    private fun resolveCounterTarget(effect: CounterEffect, context: EffectContext, state: GameState): EntityId? {
        return when (effect.targetSource) {
            CounterTargetSource.Chosen -> {
                val target = context.targets.firstOrNull()
                (target as? ChosenTarget.Spell)?.spellEntityId
            }
            CounterTargetSource.TriggeringEntity -> {
                val id = context.triggeringEntityId ?: return null
                if (!state.stack.contains(id)) null else id
            }
        }
    }

    private fun performCounter(
        state: GameState,
        effect: CounterEffect,
        entityId: EntityId,
        context: EffectContext
    ): EffectResult {
        val resolver = StackResolver(cardRegistry = cardRegistry)
        return EffectResult.from(when (effect.target) {
            CounterTarget.Ability -> resolver.counterAbility(state, entityId)
            CounterTarget.Spell -> when (val dest = effect.counterDestination) {
                CounterDestination.Graveyard -> resolver.counterSpell(state, entityId)
                is CounterDestination.Exile -> resolver.counterSpellToExile(
                    state, entityId, dest.grantFreeCast, context.controllerId
                )
            }
        })
    }

    private fun handleUnlessPaysMana(
        state: GameState,
        effect: CounterEffect,
        spellEntityId: EntityId,
        cost: ManaCost,
        context: EffectContext
    ): EffectResult {
        val payingPlayerId = getSpellCasterId(state, spellEntityId)
            ?: return EffectResult.error(state, "Spell not found on stack")

        val manaSolver = ManaSolver(cardRegistry)
        if (!manaSolver.canPay(state, payingPlayerId, cost)) {
            return performCounter(state, effect, spellEntityId, context)
        }

        return offerPayment(state, effect, spellEntityId, payingPlayerId, cost, context)
    }

    private fun handleUnlessPaysDynamic(
        state: GameState,
        effect: CounterEffect,
        spellEntityId: EntityId,
        condition: CounterCondition.UnlessPaysDynamic,
        context: EffectContext
    ): EffectResult {
        val payingPlayerId = getSpellCasterId(state, spellEntityId)
            ?: return EffectResult.error(state, "Spell not found on stack")

        val totalGenericCost = amountEvaluator.evaluate(state, condition.amount, context)

        if (totalGenericCost <= 0) {
            // Cost is 0 or negative — spell is not countered
            return EffectResult.success(state)
        }

        val manaCost = ManaCost(listOf(ManaSymbol.Generic(totalGenericCost)))

        val manaSolver = ManaSolver(cardRegistry)
        if (!manaSolver.canPay(state, payingPlayerId, manaCost)) {
            return performCounter(state, effect, spellEntityId, context)
        }

        return offerPayment(state, effect, spellEntityId, payingPlayerId, manaCost, context)
    }

    private fun getSpellCasterId(state: GameState, spellEntityId: EntityId): EntityId? {
        val spellEntity = state.getEntity(spellEntityId) ?: return null
        val spellComponent = spellEntity.get<SpellOnStackComponent>() ?: return null
        return spellComponent.casterId
    }

    private fun offerPayment(
        state: GameState,
        effect: CounterEffect,
        spellEntityId: EntityId,
        payingPlayerId: EntityId,
        manaCost: ManaCost,
        context: EffectContext
    ): EffectResult {
        val decisionResult = decisionHandler.createYesNoDecision(
            state = state,
            playerId = payingPlayerId,
            sourceId = context.sourceId,
            sourceName = "Counter unless pays",
            prompt = "Pay $manaCost to prevent your spell from being countered?",
            yesText = "Pay $manaCost",
            noText = "Don't pay"
        )

        val exileOnCounter = effect.counterDestination is CounterDestination.Exile

        val continuation = CounterUnlessPaysContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            payingPlayerId = payingPlayerId,
            spellEntityId = spellEntityId,
            manaCost = manaCost,
            sourceId = context.sourceId,
            sourceName = "Counter unless pays",
            exileOnCounter = exileOnCounter,
            controllerId = context.controllerId
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }
}
