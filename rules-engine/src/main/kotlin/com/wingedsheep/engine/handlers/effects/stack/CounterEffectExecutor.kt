package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.CounterEachUnlessPaysContinuation
import com.wingedsheep.engine.core.CounterUnlessPaysContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.SpellCounterer
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CounterCondition
import com.wingedsheep.sdk.scripting.effects.CounterDestination
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.CounterTarget
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
    private val cardRegistry: CardRegistry,
    private val counterer: SpellCounterer
) : EffectExecutor<CounterEffect> {
    private val predicateEvaluator = amountEvaluator.predicates

    override val effectType: KClass<CounterEffect> = CounterEffect::class

    private val decisionHandler = DecisionHandler()
    override fun execute(
        state: GameState,
        effect: CounterEffect,
        context: EffectContext
    ): EffectResult {
        val targetSource = effect.targetSource
        if (targetSource is CounterTargetSource.EachOnStack) {
            return executeEachOnStack(state, effect, targetSource, context)
        }

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
            if (!predicateEvaluator.matches(state, state.projectedState, entityId, filter.baseFilter, predicateContext)) {
                return EffectResult.error(state, "Target does not match filter: ${filter.baseFilter.description}")
            }
        }

        // Step 3: Handle condition (unless pays) or perform counter directly
        return when (val condition = effect.condition) {
            is CounterCondition.Always -> performCounter(state, effect, entityId, context)
            is CounterCondition.UnlessPaysMana ->
                handleUnlessPaysMana(state, effect, entityId, condition.cost, condition.onPaid, context)
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
            is CounterTargetSource.EachOnStack -> error("EachOnStack is resolved by executeEachOnStack")
        }
    }

    /**
     * "For each spell and ability your opponents control, counter it [unless its controller pays N]"
     * (Whirlwind Denial). Untargeted: every matching object on the stack at resolution, asked
     * about grouped by controller in APNAP order (CR 101.4), then every unpaid one countered at
     * the same time by [CounterEachUnlessPays].
     */
    private fun executeEachOnStack(
        state: GameState,
        effect: CounterEffect,
        source: CounterTargetSource.EachOnStack,
        context: EffectContext
    ): EffectResult {
        val controllers = context.resolvePlayerTargets(EffectTarget.PlayerRef(source.controller), state).toSet()
        val predicateContext = PredicateContext.fromEffectContext(context)
        val filter = effect.filter
        val apnap = state.apnapOrder
        val objects = state.stack.asReversed().filter { id ->
            id != context.sourceId &&
                TargetResolutionUtils.stackObjectController(state, id) in controllers &&
                when (effect.target) {
                    CounterTarget.Spell -> state.isSpellOnStack(id)
                    CounterTarget.Ability -> !state.isSpellOnStack(id)
                    CounterTarget.SpellOrAbility -> true
                } &&
                (filter == null || predicateEvaluator.matches(
                    state, state.projectedState, id, filter.baseFilter, predicateContext
                ))
        }.sortedBy { id ->
            // Stable sort: each controller's objects stay top-of-stack first.
            apnap.indexOf(TargetResolutionUtils.stackObjectController(state, id)).let { if (it < 0) Int.MAX_VALUE else it }
        }

        val manaCost = when (val condition = effect.condition) {
            // Rejected by CounterEffect's init: unconditional is CounterAllOnStackEffect.
            CounterCondition.Always -> return EffectResult.error(state, "EachOnStack needs an unless-pays condition")
            is CounterCondition.UnlessPaysMana -> condition.cost
            is CounterCondition.UnlessPaysDynamic -> {
                val amount = amountEvaluator.evaluate(state, condition.amount, context)
                // A cost of 0 or less is always "paid": nothing is countered.
                if (amount <= 0) return EffectResult.success(state)
                ManaCost(listOf(ManaSymbol.Generic(amount)))
            }
        }
        val controllerId = context.controllerId
        val frame = CounterEachUnlessPaysContinuation(
            remaining = objects,
            unpaid = emptyList(),
            manaCost = manaCost,
            target = effect.target,
            counterDestination = effect.counterDestination,
            controllerId = controllerId,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        )
        return EffectResult.from(CounterEachUnlessPays(counterer, ManaSolver(cardRegistry, predicateEvaluator)).step(state, frame))
    }

    private fun performCounter(
        state: GameState,
        effect: CounterEffect,
        entityId: EntityId,
        context: EffectContext
    ): EffectResult {
        return EffectResult.from(
            counterStackObject(
                state, counterer, effect.target, effect.counterDestination,
                entityId, context.controllerId, context.sourceId
            )
        )
    }

    private fun handleUnlessPaysMana(
        state: GameState,
        effect: CounterEffect,
        spellEntityId: EntityId,
        cost: ManaCost,
        onPaid: com.wingedsheep.sdk.scripting.effects.Effect?,
        context: EffectContext
    ): EffectResult {
        val payingPlayerId = TargetResolutionUtils.stackObjectController(state, spellEntityId)
            ?: return EffectResult.error(state, "Spell or ability not found on stack")

        val manaSolver = ManaSolver(cardRegistry, predicateEvaluator)
        if (!manaSolver.canPay(state, payingPlayerId, cost)) {
            return performCounter(state, effect, spellEntityId, context)
        }

        return offerPayment(state, effect, spellEntityId, payingPlayerId, cost, onPaid, context)
    }

    private fun handleUnlessPaysDynamic(
        state: GameState,
        effect: CounterEffect,
        spellEntityId: EntityId,
        condition: CounterCondition.UnlessPaysDynamic,
        context: EffectContext
    ): EffectResult {
        val payingPlayerId = TargetResolutionUtils.stackObjectController(state, spellEntityId)
            ?: return EffectResult.error(state, "Spell or ability not found on stack")

        val totalGenericCost = amountEvaluator.evaluate(state, condition.amount, context)

        if (totalGenericCost <= 0) {
            // Cost is 0 or negative — spell is not countered.
            // The "If they do, …" rider also doesn't fire: nothing was paid.
            return EffectResult.success(state)
        }

        val manaCost = ManaCost(listOf(ManaSymbol.Generic(totalGenericCost)))

        val manaSolver = ManaSolver(cardRegistry, predicateEvaluator)
        if (!manaSolver.canPay(state, payingPlayerId, manaCost)) {
            return performCounter(state, effect, spellEntityId, context)
        }

        return offerPayment(state, effect, spellEntityId, payingPlayerId, manaCost, condition.onPaid, context)
    }

    private fun offerPayment(
        state: GameState,
        effect: CounterEffect,
        spellEntityId: EntityId,
        payingPlayerId: EntityId,
        manaCost: ManaCost,
        onPaid: com.wingedsheep.sdk.scripting.effects.Effect?,
        context: EffectContext
    ): EffectResult {
        // Name the card asking (Spell Stutter, Mana Leak, …) — the prompt is about *its* demand.
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val continuation = CounterUnlessPaysContinuation(
            resolvingTriggeredAbility = context.resolvingTriggeredAbility,
            payingPlayerId = payingPlayerId,
            spellEntityId = spellEntityId,
            manaCost = manaCost,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            counterDestination = effect.counterDestination,
            controllerId = context.controllerId,
            onPaid = onPaid
        )

        val decisionResult = decisionHandler.createYesNoDecision(
            state = state,
            playerId = payingPlayerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Pay $manaCost to prevent your ${if (state.isSpellOnStack(spellEntityId)) "spell" else "ability"} from being countered?",
            yesText = "Pay $manaCost",
            noText = "Don't pay",
            answer = continuation
        )

        return EffectResult.propagatePause(
            decisionResult.state,
            decisionResult.events
        )
    }
}
