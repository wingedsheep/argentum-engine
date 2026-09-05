package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.mana.ManaAbilityResolutionPipeline
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.ChoiceSlot

class ColorChoiceContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices,
    private val effectRunner: EffectContinuationRunner
) : ContinuationResumerModule {

    private val tappedForManaBonusResolver =
        com.wingedsheep.engine.handlers.effects.mana.TappedForManaBonusResolver(services.cardRegistry)

    /** Shared with `ActivateAbilityHandler` so a paused mana ability finishes the same way. */
    private val manaPipeline = ManaAbilityResolutionPipeline(
        cardRegistry = services.cardRegistry,
        conditionEvaluator = services.conditionEvaluator,
        effectExecutorRegistry = services.effectExecutorRegistry,
        predicateEvaluator = services.predicateEvaluator,
    )

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ChooseColorThenContinuation::class, ::resumeChooseColorThen),
        resumer(ChooseNumberThenContinuation::class, ::resumeChooseNumberThen),
        resumer(ChooseNumberForSourceContinuation::class, ::resumeChooseNumberForSource),
        resumer(ChooseOpponentForSourceContinuation::class, ::resumeChooseOpponentForSource),
        resumer(ChooseCardTypeForSourceContinuation::class, ::resumeChooseCardTypeForSource),
        resumer(ChooseOpponentDeciderContinuation::class, ::resumeChooseOpponentDecider),
        resumer(ChooseManaColorContinuation::class, ::resumeChooseManaColor),
        resumer(ChooseColorForTargetContinuation::class, ::resumeChooseColorForTarget),
        resumer(ChooseAnyColorTapBonusContinuation::class, ::resumeChooseAnyColorTapBonus)
    )

    fun resumeChooseAnyColorTapBonus(
        state: GameState,
        continuation: ChooseAnyColorTapBonusContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult = tappedForManaBonusResolver.resume(state, continuation, response, checkForMore)

    fun resumeChooseColorThen(
        state: GameState,
        continuation: ChooseColorThenContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is ColorChosenResponse) {
            return ExecutionResult.error(state, "Expected color choice response for ChooseColorThen effect")
        }

        val contextWithColor = continuation.baseContext.copy(chosenColor = response.color)
        val effectResult = effectRunner.executeRemainingEffects(
            state,
            listOf(continuation.then),
            contextWithColor
        )

        if (effectResult.isPaused) return effectResult.toExecutionResult()
        return checkForMore(effectResult.state, effectResult.events.toList())
    }

    fun resumeChooseNumberThen(
        state: GameState,
        continuation: ChooseNumberThenContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number choice response for ChooseNumberThen effect")
        }

        // Stamp the chosen number as X so atomic effects/filters (manaValueEqualsX) read it.
        val contextWithNumber = continuation.baseContext.copy(xValue = response.number)
        val effectResult = effectRunner.executeRemainingEffects(
            state,
            listOf(continuation.then),
            contextWithNumber
        )

        if (effectResult.isPaused) return effectResult.toExecutionResult()
        return checkForMore(effectResult.state, effectResult.events.toList())
    }

    fun resumeChooseNumberForSource(
        state: GameState,
        continuation: ChooseNumberForSourceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number choice response for ChooseNumberForSource effect")
        }
        // Record the chosen number durably on the source permanent (replacing any prior value
        // for the slot), so the source's characteristic-defining ability reads the latest choice.
        if (state.getEntity(continuation.sourceId) == null ||
            !continuation.objectReferences.isCurrent(continuation.objectReferences.source, state)) {
            return checkForMore(state, emptyList())
        }
        val newState = state.updateEntity(continuation.sourceId) { container ->
            container.withCastChoice(continuation.slot, ChoiceValue.NumberChoice(response.number))
        }
        return checkForMore(newState, emptyList())
    }

    fun resumeChooseOpponentForSource(
        state: GameState,
        continuation: ChooseOpponentForSourceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for ChooseOpponentForSource effect")
        }
        val chosen = continuation.opponentIds.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Opponent choice index ${response.optionIndex} out of range")
        val newState = if (state.getEntity(continuation.sourceId) != null &&
            continuation.objectReferences.isCurrent(continuation.objectReferences.source, state)) {
            state.updateEntity(continuation.sourceId) { container ->
                container.withCastChoice(ChoiceSlot.OPPONENT, ChoiceValue.EntityChoice(chosen))
            }
        } else state
        val withChoice = exposeCollectionsToNextFrame(newState, mapOf(
            com.wingedsheep.engine.handlers.RESOLUTION_CHOSEN_OPPONENT to listOf(chosen),
        ))
        return checkForMore(withChoice, emptyList())
    }

    fun resumeChooseCardTypeForSource(
        state: GameState,
        continuation: ChooseCardTypeForSourceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for ChooseCardTypeForSource effect")
        }
        val chosen = continuation.cardTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Card type choice index ${response.optionIndex} out of range")
        // Record the chosen card type durably on the source permanent so
        // CardPredicate.CardTypeEqualsChosenComponent reads it at cost-calculation / projection time.
        if (state.getEntity(continuation.sourceId) == null ||
            !continuation.objectReferences.isCurrent(continuation.objectReferences.source, state)) {
            return checkForMore(state, emptyList())
        }
        val newState = state.updateEntity(continuation.sourceId) { container ->
            container.withCastChoice(continuation.slot, ChoiceValue.TextChoice(chosen))
        }
        return checkForMore(newState, emptyList())
    }

    /**
     * The controller picked which opponent makes a `Chooser.Opponent` decision (CR 601.7a /
     * 602.3a and the matching resolution-time rulings). Stamp the pick onto the context and
     * re-run the effect, which now resolves its chooser to that opponent and pauses with its
     * own decision.
     */
    fun resumeChooseOpponentDecider(
        state: GameState,
        continuation: ChooseOpponentDeciderContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for the opponent-decider pick")
        }
        val chosen = continuation.opponentIds.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Opponent choice index ${response.optionIndex} out of range")

        val contextWithDecider = continuation.baseContext.copy(opponentDeciderId = chosen)
        val effectResult = effectRunner.executeRemainingEffects(
            state,
            listOf(continuation.effect),
            contextWithDecider
        )

        if (effectResult.isPaused) return effectResult.toExecutionResult()
        // The re-run finished without pausing (nothing left to decide). Publish whatever it
        // stored so the composite's remaining steps still see it.
        val published = exposeCollectionsToNextFrame(effectResult.state, effectResult.updatedCollections)
        return checkForMore(published, effectResult.events.toList())
    }

    fun resumeChooseManaColor(
        state: GameState,
        continuation: ChooseManaColorContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is ColorChosenResponse) {
            return ExecutionResult.error(state, "Expected color choice response for AddManaOfChoice effect")
        }

        val contextWithColor = continuation.baseContext.copy(manaColorChoice = response.color)
        val effectResult = effectRunner.executeRemainingEffects(
            state,
            listOf(continuation.effect),
            contextWithColor
        )

        if (effectResult.isPaused) return effectResult.toExecutionResult()

        // The mana ability itself is now done, but only its *effect* ran — `ActivateAbilityHandler`
        // returned at the pause, before the rest of the tap pipeline. Everything downstream of the
        // produced mana (Damping Sphere, Elvish Guidance, Badgermole Cub / Roxanne / Overabundance,
        // the land-tapped event, Fertile Ground) therefore has to run here instead, through the
        // same [ManaAbilityResolutionPipeline] the synchronous path uses. The two are mutually
        // exclusive — the inline pass runs only when the effect did not pause — so nothing is
        // applied twice.
        val sourceId = continuation.sourceId
            ?: return checkForMore(effectResult.state, effectResult.events.toList())
        val tapperId = continuation.controllerId
        val sourceCard = effectResult.state.getEntity(sourceId)?.get<CardComponent>()

        var events = effectResult.events.toList()
        var producedMana = events.filterIsInstance<ManaAddedEvent>().lastOrNull { it.sourceId == sourceId }

        // Damping Sphere: a land tapped for two or more mana produces {C} instead. `state` is the
        // pool as it stood before the chosen mana was added, which is the delta this reads.
        val dampening = manaPipeline.applyLandManaDampening(state, effectResult.state, sourceCard, tapperId)
        if (dampening.dampened) {
            val replacement = ManaAddedEvent(
                playerId = tapperId,
                sourceId = sourceId,
                sourceName = sourceCard?.name ?: continuation.sourceName,
                colorless = 1
            )
            events = events.filterNot { it === producedMana } + replacement
            producedMana = replacement
        }

        val finished = manaPipeline.finishTapBonuses(
            dampening.state, sourceId, sourceCard, tapperId, producedMana, events
        )
        if (finished.isPaused) return finished
        return checkForMore(finished.newState, finished.events.toList())
    }

    fun resumeChooseColorForTarget(
        state: GameState,
        continuation: ChooseColorForTargetContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is ColorChosenResponse) {
            return ExecutionResult.error(state, "Expected color choice response")
        }

        val targetId = continuation.targetEntityId
        if (!state.getBattlefield().contains(targetId) || state.getEntity(targetId) == null) {
            return checkForMore(state, emptyList())
        }

        val newState = state.updateEntity(targetId) { container ->
            container.withCastChoice(ChoiceSlot.COLOR, ChoiceValue.ColorChoice(response.color))
        }

        return checkForMore(newState, emptyList())
    }
}
