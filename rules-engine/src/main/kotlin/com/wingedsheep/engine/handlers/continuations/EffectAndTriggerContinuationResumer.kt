package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.engine.handlers.effects.composite.asMayDecide
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Gate
import java.util.UUID

/**
 * Handles core effect and trigger resumption:
 * - EffectContinuation (composite effect pipelines)
 * - TriggeredAbilityContinuation (target selection for triggered abilities)
 * - ResolveSpellContinuation (no-op marker)
 * - MayAbilityContinuation (yes/no for may effects)
 * - MayTriggerContinuation (yes/no for may triggers with targets)
 */
class EffectAndTriggerContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices,
    private val effectRunner: EffectContinuationRunner
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(EffectContinuation::class, ::resumeEffect),
        resumer(TriggeredAbilityContinuation::class, ::resumeTriggeredAbility),
        resumer(TriggerDamageDistributionContinuation::class, ::resumeTriggerDamageDistribution),
        resumer(ResolveSpellContinuation::class) { state, _, _, _ ->
            ExecutionResult.success(state)
        },
        resumer(MayAbilityContinuation::class, ::resumeMayAbility),
        resumer(GatedEffectContinuation::class, ::resumeGatedEffect),
        resumer(MayRevealCardFromHandContinuation::class, ::resumeMayRevealCardFromHand),
        resumer(BeholdContinuation::class, ::resumeBehold),
        resumer(MayTriggerContinuation::class, ::resumeMayTrigger),
        resumer(ReflexiveTriggerResolveContinuation::class, ::resumeReflexiveTriggerResolve)
    )

    private fun resumeEffect(
        state: GameState,
        continuation: EffectContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val effectResult = effectRunner.executeRemainingEffects(state, continuation.remainingEffects, continuation.effectContext)
        if (effectResult.isPaused) return effectResult.toExecutionResult()
        return checkForMore(effectResult.state, effectResult.events.toList())
    }

    private fun resumeTriggeredAbility(
        state: GameState,
        continuation: TriggeredAbilityContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected target selection response for triggered ability")
        }

        val selectedTargets = response.selectedTargets.flatMap { (_, targetIds) ->
            targetIds.map { entityId -> entityIdToChosenTarget(state, entityId) }
        }

        if (selectedTargets.isEmpty()) {
            if (continuation.elseEffect != null) {
                val elseComponent = TriggeredAbilityOnStackComponent(
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName,
                    controllerId = continuation.controllerId,
                    effect = continuation.elseEffect,
                    description = continuation.description,
                    triggerDamageAmount = continuation.triggerDamageAmount,
                    triggeringEntityId = continuation.triggeringEntityId,
                    triggeringPlayerId = continuation.triggeringPlayerId,
                    triggerCounterCount = continuation.triggerCounterCount,
                    triggerTotalCounterCount = continuation.triggerTotalCounterCount,
                    triggerLastKnownCounters = continuation.triggerLastKnownCounters,
                    triggerLastKnownDamageDealtByPlayers = continuation.triggerLastKnownDamageDealtByPlayers,
                    triggerScryCount = continuation.triggerScryCount,
                    triggerExcessDamageAmount = continuation.triggerExcessDamageAmount
                )
                val stackResult = services.stackResolver.putTriggeredAbility(state, elseComponent, emptyList())
                if (!stackResult.isSuccess) return stackResult
                return checkForMore(stackResult.newState, stackResult.events.toList())
            }
            return checkForMore(state, emptyList())
        }

        // Check if this is a DividedDamageEffect with multiple targets — need distribution.
        // A dynamicTotal (e.g. Ureni — "X = lands you control") is evaluated now, as the ability
        // goes on the stack, so the player divides the correct amount among the chosen targets.
        val effect = continuation.effect
        if (effect is DividedDamageEffect && selectedTargets.size > 1) {
            val total = effect.dynamicTotal?.let {
                com.wingedsheep.engine.handlers.DynamicAmountEvaluator().evaluate(
                    state,
                    it,
                    com.wingedsheep.engine.handlers.EffectContext(
                        sourceId = continuation.sourceId,
                        controllerId = continuation.controllerId,
                        opponentId = state.getOpponent(continuation.controllerId)
                    )
                )
            } ?: effect.totalDamage
            return createTriggerDamageDistributionDecision(
                state, continuation, selectedTargets, total, checkForMore
            )
        }

        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            triggerDamageAmount = continuation.triggerDamageAmount,
            triggeringEntityId = continuation.triggeringEntityId,
            triggeringPlayerId = continuation.triggeringPlayerId,
            triggerCounterCount = continuation.triggerCounterCount,
            triggerTotalCounterCount = continuation.triggerTotalCounterCount,
            triggerLastKnownCounters = continuation.triggerLastKnownCounters,
            triggerLastKnownDamageDealtByPlayers = continuation.triggerLastKnownDamageDealtByPlayers,
            triggerModesChosenCount = continuation.triggerModesChosenCount,
            enchantedCreatureLastKnownPower = continuation.enchantedCreatureLastKnownPower,
            triggerScryCount = continuation.triggerScryCount,
            triggerExcessDamageAmount = continuation.triggerExcessDamageAmount
        )

        val stackResult = services.stackResolver.putTriggeredAbility(
            state, abilityComponent, selectedTargets, continuation.targetRequirements
        )

        if (!stackResult.isSuccess) {
            return stackResult
        }

        return checkForMore(stackResult.newState, stackResult.events.toList())
    }

    /**
     * After targets are selected for a triggered ability with DividedDamageEffect,
     * pause to ask how to distribute damage among the chosen targets.
     */
    private fun createTriggerDamageDistributionDecision(
        state: GameState,
        continuation: TriggeredAbilityContinuation,
        selectedTargets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
        totalDamage: Int,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val sourceName = continuation.sourceId.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: continuation.sourceName

        val targetEntityIds = selectedTargets.map { target ->
            when (target) {
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player -> target.playerId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> target.entityId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Card -> target.cardId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> target.spellEntityId
            }
        }
        val decisionId = UUID.randomUUID().toString()
        val decision = DistributeDecision(
            id = decisionId,
            playerId = continuation.controllerId,
            prompt = "Divide $totalDamage damage among ${selectedTargets.size} targets",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.CASTING
            ),
            totalAmount = totalDamage,
            targets = targetEntityIds,
            minPerTarget = 1
        )

        val distributionContinuation = TriggerDamageDistributionContinuation(
            decisionId = decisionId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            triggerDamageAmount = continuation.triggerDamageAmount,
            triggeringEntityId = continuation.triggeringEntityId,
            triggeringPlayerId = continuation.triggeringPlayerId,
            triggerCounterCount = continuation.triggerCounterCount,
            triggerTotalCounterCount = continuation.triggerTotalCounterCount,
            triggerLastKnownCounters = continuation.triggerLastKnownCounters,
            triggerLastKnownDamageDealtByPlayers = continuation.triggerLastKnownDamageDealtByPlayers,
            selectedTargets = selectedTargets,
            targetRequirements = continuation.targetRequirements,
            totalDamage = totalDamage
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(distributionContinuation)

        val events = listOf(
            DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = continuation.controllerId,
                decisionType = "DISTRIBUTE",
                prompt = decision.prompt
            )
        )

        return ExecutionResult.paused(newState, decision, events)
    }

    /**
     * Resume after player distributes damage for a triggered ability's DividedDamageEffect.
     * Put the ability on the stack with the distribution locked in.
     */
    private fun resumeTriggerDamageDistribution(
        state: GameState,
        continuation: TriggerDamageDistributionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is DistributionResponse) {
            return ExecutionResult.error(state, "Expected distribution response for triggered ability damage")
        }

        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            triggerDamageAmount = continuation.triggerDamageAmount,
            triggeringEntityId = continuation.triggeringEntityId,
            triggeringPlayerId = continuation.triggeringPlayerId,
            triggerCounterCount = continuation.triggerCounterCount,
            triggerTotalCounterCount = continuation.triggerTotalCounterCount,
            triggerLastKnownCounters = continuation.triggerLastKnownCounters,
            triggerLastKnownDamageDealtByPlayers = continuation.triggerLastKnownDamageDealtByPlayers,
            damageDistribution = response.distribution
        )

        val stackResult = services.stackResolver.putTriggeredAbility(
            state, abilityComponent, continuation.selectedTargets, continuation.targetRequirements
        )

        if (!stackResult.isSuccess) {
            return stackResult
        }

        return checkForMore(stackResult.newState, stackResult.events.toList())
    }

    private fun resumeMayTrigger(
        state: GameState,
        continuation: MayTriggerContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may trigger")
        }

        if (!response.choice) {
            return checkForMore(state, emptyList())
        }

        val trigger = continuation.trigger
        val innerEffect = trigger.ability.effect.asMayDecide()?.then
            ?: return ExecutionResult.error(state, "May trigger continuation resumed on a non-may effect")

        val unwrappedAbility = trigger.ability.copy(effect = innerEffect)
        val unwrappedTrigger = trigger.copy(ability = unwrappedAbility)

        val result = services.triggerProcessor.processTargetedTrigger(state, unwrappedTrigger, continuation.targetRequirement)

        if (result.isPaused) {
            return result
        }

        if (!result.isSuccess) {
            return result
        }

        return checkForMore(result.newState, result.events.toList())
    }

    private fun resumeMayAbility(
        state: GameState,
        continuation: MayAbilityContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may ability")
        }

        val context = continuation.effectContext
        val effectToExecute = if (response.choice) {
            continuation.effectIfYes
        } else {
            continuation.effectIfNo
        }

        if (effectToExecute == null) {
            return checkForMore(state, emptyList())
        }

        val result = services.effectExecutorRegistry.execute(state, effectToExecute, context).toExecutionResult()

        if (result.isPaused) {
            return result
        }

        return checkForMore(result.state, result.events.toList())
    }

    /**
     * Resume a [GatedEffect] after its gate's yes/no decision. The canonical unwind:
     * on "yes", run [GatedEffectContinuation.then] — for [Gate.MayPay], pay the cost first
     * (a `stopOnError` composite so an unpayable cost aborts the payoff, mirroring the former
     * OptionalCost behavior); on "no", run [GatedEffectContinuation.otherwise]. The locked
     * targets travel in the continuation's [EffectContext], so a targeted `then` resolves
     * against its trigger-time target.
     */
    private fun resumeGatedEffect(
        state: GameState,
        continuation: GatedEffectContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for gated effect")
        }

        val effectToExecute: Effect? = if (response.choice) {
            when (val gate = continuation.gate) {
                is Gate.MayDecide -> continuation.then
                is Gate.MayPay ->
                    CompositeEffect(listOf(gate.cost, continuation.then), stopOnError = true)
                // WhenCondition resolves synchronously in the executor and never pushes this
                // continuation, so this branch is unreachable — present only for exhaustiveness.
                is Gate.WhenCondition -> continuation.then
            }
        } else {
            continuation.otherwise
        }

        if (effectToExecute == null) {
            return checkForMore(state, emptyList())
        }

        val result = services.effectExecutorRegistry
            .execute(state, effectToExecute, continuation.effectContext)
            .toExecutionResult()

        if (result.isPaused) {
            return result
        }

        return checkForMore(result.state, result.events.toList())
    }

    private fun resumeMayRevealCardFromHand(
        state: GameState,
        continuation: MayRevealCardFromHandContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for may-reveal-from-hand")
        }

        val chosenCardId = response.selectedCards.firstOrNull()

        if (chosenCardId == null) {
            // Player declined to reveal — fall through to the "otherwise" branch.
            val otherwise = continuation.otherwise
                ?: return checkForMore(state, emptyList())
            val result = services.effectExecutorRegistry
                .execute(state, otherwise, continuation.effectContext)
                .toExecutionResult()
            return if (result.isPaused) result
            else checkForMore(result.state, result.events.toList())
        }

        // Player picked a card — emit the public reveal. The reveal itself is the
        // entire payoff of the MayReveal atom; any rider effect lives in `otherwise`.
        val (revealedState, revealEvent) = com.wingedsheep.engine.handlers.effects.composite
            .MayRevealCardFromHandEffectExecutor.emitReveal(
                state, continuation.revealerId, chosenCardId, continuation.sourceName,
            )
        return checkForMore(revealedState, listOf(revealEvent))
    }

    private fun resumeBehold(
        state: GameState,
        continuation: BeholdContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for behold")
        }

        val chosenId = response.selectedCards.firstOrNull()
        if (chosenId == null) {
            // Player declined to behold — the "if you do" payoff doesn't run.
            return checkForMore(state, emptyList())
        }

        // If the beheld object was a card in hand, reveal it publicly. Battlefield permanents
        // are chosen, not revealed.
        var currentState = state
        val events = mutableListOf<GameEvent>()
        if (chosenId in continuation.handOptionIds) {
            val (revealedState, revealEvent) = com.wingedsheep.engine.handlers.effects.composite
                .MayRevealCardFromHandEffectExecutor.emitReveal(
                    currentState, continuation.beholderId, chosenId, continuation.sourceName,
                )
            currentState = revealedState
            events += revealEvent
        }

        val ifBeheld = continuation.ifBeheld
            ?: return checkForMore(currentState, events)

        val result = services.effectExecutorRegistry
            .execute(currentState, ifBeheld, continuation.effectContext)
            .toExecutionResult()
        if (result.isPaused) return result
        return checkForMore(result.state, events + result.events.toList())
    }

    private fun resumeReflexiveTriggerResolve(
        state: GameState,
        continuation: ReflexiveTriggerResolveContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected target selection response for reflexive trigger")
        }

        val selectedTargets = response.selectedTargets.flatMap { (_, targetIds) ->
            targetIds.map { entityId -> entityIdToChosenTarget(state, entityId) }
        }

        if (selectedTargets.isEmpty()) {
            // Player declined targets (optional) or no valid targets selected
            return checkForMore(state, emptyList())
        }

        // Execute the reflexive effect with the chosen targets
        val context = continuation.effectContext.copy(
            targets = selectedTargets,
            pipeline = continuation.effectContext.pipeline.copy(
                namedTargets = com.wingedsheep.engine.handlers.EffectContext.buildNamedTargets(
                    continuation.reflexiveTargetRequirements, selectedTargets
                )
            )
        )

        val result = services.effectExecutorRegistry.execute(state, continuation.reflexiveEffect, context).toExecutionResult()

        if (result.isPaused) {
            return result
        }

        return checkForMore(result.state, result.events.toList())
    }
}
