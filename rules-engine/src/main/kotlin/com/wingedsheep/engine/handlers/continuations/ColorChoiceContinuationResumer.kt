package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.mana.AdditionalManaOnSourceTapMirror
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.withCastChoice
import com.wingedsheep.sdk.scripting.ChoiceSlot

class ColorChoiceContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices,
    private val effectRunner: EffectContinuationRunner
) : ContinuationResumerModule {

    private val tappedForManaBonusResolver =
        com.wingedsheep.engine.handlers.effects.mana.TappedForManaBonusResolver(services.cardRegistry)

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
        if (state.getEntity(continuation.sourceId) == null) {
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
        // Record the chosen opponent durably on the source (spell or permanent) so
        // Player.ChosenOpponent reads it for the rest of the resolution and beyond.
        if (state.getEntity(continuation.sourceId) == null) {
            return checkForMore(state, emptyList())
        }
        val newState = state.updateEntity(continuation.sourceId) { container ->
            container.withCastChoice(ChoiceSlot.OPPONENT, ChoiceValue.EntityChoice(chosen))
        }
        return checkForMore(newState, emptyList())
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
        if (state.getEntity(continuation.sourceId) == null) {
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

        // CR 605 mirror bonus: if the just-chosen mana came from tapping a permanent for mana
        // (the continuation's source) and a battlefield `AdditionalManaOnSourceTap` mirror static
        // (color = null) applies to that source for this tapper, add one mana of the chosen type.
        // This is the any-color analogue of `ActivateAbilityHandler.resolveAdditionalManaOnSourceTap`
        // — that path runs only for fixed/non-pausing producers (Lavaleaper's basic lands); an
        // any-color producer (Roxanne's Meteorite, "{T}: Add one mana of any color") pauses for the
        // color choice and resumes here, so the mirror must fire after the choice is known.
        val mirrored = AdditionalManaOnSourceTapMirror.applyForResolvedTap(
            services, effectResult.state, continuation.sourceId, continuation.controllerId, response.color
        )
        if (mirrored.isPaused) return mirrored
        return checkForMore(mirrored.newState, effectResult.events.toList() + mirrored.events)
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
