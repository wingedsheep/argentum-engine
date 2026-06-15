package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardKind
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Continuation frames for [com.wingedsheep.sdk.scripting.effects.OpponentGuessesTopCardKindEffect].
 *
 * Two sequenced decisions:
 *  1. [ChooseGuessKindContinuation] — the chooser picks the framing land/nonland kind.
 *  2. [GuessTopCardKindContinuation] — the guesser guesses the top card's actual kind; on resume the
 *     card is revealed and the guess compared to reality, branching into the right/wrong effect.
 */

/**
 * Resume after the chooser picked the framing [CardKind] ("Choose land or nonland"). Stores the
 * chosen kind and presents the guess decision to the guesser.
 */
@Serializable
@SerialName("ChooseGuessKindContinuation")
data class ChooseGuessKindContinuation(
    override val decisionId: String,
    val controllerLibraryOwnerId: EntityId,
    val guesserId: EntityId,
    val onGuessedRight: Effect,
    val onGuessedWrong: Effect,
    val effectContext: EffectContext,
) : ContinuationFrame

/**
 * Resume after the guesser guessed the top card's [CardKind]. Reveals the top card of the
 * controller's library, compares its actual kind to the guess, and runs the matching branch effect
 * in the captured [effectContext].
 */
@Serializable
@SerialName("GuessTopCardKindContinuation")
data class GuessTopCardKindContinuation(
    override val decisionId: String,
    val controllerLibraryOwnerId: EntityId,
    val guesserId: EntityId,
    val onGuessedRight: Effect,
    val onGuessedWrong: Effect,
    val effectContext: EffectContext,
) : ContinuationFrame
