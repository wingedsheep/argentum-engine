package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.ChooseGuessKindContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.GuessTopCardKindContinuation
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardKind
import com.wingedsheep.sdk.scripting.effects.Effect
import java.util.UUID

/**
 * Resumes the two-step opponent-guess flow for
 * [com.wingedsheep.sdk.scripting.effects.OpponentGuessesTopCardKindEffect].
 *
 *  1. [ChooseGuessKindContinuation] — the chooser picked land/nonland; now ask the guesser to guess.
 *  2. [GuessTopCardKindContinuation] — the guesser guessed; reveal the top card, compare the guess to
 *     the card's actual kind, and run the matching branch effect.
 *
 * A correct guess is one where the guesser's land/nonland call matches the actual top card. An empty
 * library has no top card, so the guess is never right and the "wrong" branch runs.
 */
class GuessContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    private val effectRunner: EffectContinuationRunner by lazy {
        EffectContinuationRunner(services.effectExecutorRegistry)
    }

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ChooseGuessKindContinuation::class, ::resumeChooseKind),
        resumer(GuessTopCardKindContinuation::class, ::resumeGuess),
    )

    /** Step 1 resume: the chooser picked land/nonland. Present the guess to the guesser. */
    private fun resumeChooseKind(
        state: GameState,
        continuation: ChooseGuessKindContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice for guess-kind framing choice")
        }
        // The framing choice (index 0 = Land, 1 = Nonland) only frames the prompt; it does not change
        // which guess counts as correct. We carry the prompt forward but compare the guess to reality.

        val sourceName = continuation.effectContext.sourceId?.let {
            state.getEntity(it)?.get<CardComponent>()?.name
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = continuation.guesserId,
            prompt = "Guess whether the top card of the library is land or nonland",
            context = DecisionContext(
                sourceId = continuation.effectContext.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = listOf("Land", "Nonland")
        )

        val nextContinuation = GuessTopCardKindContinuation(
            decisionId = decisionId,
            controllerLibraryOwnerId = continuation.controllerLibraryOwnerId,
            guesserId = continuation.guesserId,
            onGuessedRight = continuation.onGuessedRight,
            onGuessedWrong = continuation.onGuessedWrong,
            effectContext = continuation.effectContext
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(nextContinuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.guesserId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    /** Step 2 resume: the guesser guessed. Reveal the top card and branch on correctness. */
    private fun resumeGuess(
        state: GameState,
        continuation: GuessTopCardKindContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice for top-card guess")
        }

        val guessedKind = when (response.optionIndex) {
            0 -> CardKind.LAND
            1 -> CardKind.NONLAND
            else -> return ExecutionResult.error(state, "Invalid guess index: ${response.optionIndex}")
        }

        val libraryOwnerId = continuation.controllerLibraryOwnerId
        val topCardId = state.getZone(ZoneKey(libraryOwnerId, Zone.LIBRARY)).firstOrNull()

        val events = mutableListOf<GameEvent>()
        var newState = state

        // Reveal the top card to everyone (if any). Empty library => no card revealed.
        val actualKind: CardKind? = if (topCardId != null) {
            val card = state.getEntity(topCardId)?.get<CardComponent>()
            newState = LibraryRevealUtils.markRevealed(newState, listOf(topCardId), newState.turnOrder.toSet())
            events.add(
                CardsRevealedEvent(
                    revealingPlayerId = libraryOwnerId,
                    cardIds = listOf(topCardId),
                    cardNames = listOf(card?.name ?: "Unknown"),
                    imageUris = listOf(card?.imageUri),
                    source = continuation.effectContext.sourceId?.let {
                        state.getEntity(it)?.get<CardComponent>()?.name
                    }
                )
            )
            if (card?.typeLine?.isLand == true) CardKind.LAND else CardKind.NONLAND
        } else {
            null
        }

        // A correct guess requires a top card whose actual kind matches the guess.
        val guessedRight = actualKind != null && actualKind == guessedKind
        val branch: Effect = if (guessedRight) continuation.onGuessedRight else continuation.onGuessedWrong

        val result = effectRunner.executeRemainingEffects(
            newState,
            listOf(branch),
            continuation.effectContext
        )

        if (result.isPaused) {
            return ExecutionResult.paused(result.state, result.pendingDecision!!, events + result.events)
        }
        return checkForMore(result.state, events + result.events.toList())
    }
}
