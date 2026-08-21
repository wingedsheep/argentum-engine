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

/**
 * Resume after the guesser answered a
 * [com.wingedsheep.sdk.scripting.effects.PlayerGuessesConditionEffect] (Liar's Pendulum). On resume
 * the condition is evaluated for the first time and the answer scored, publishing 1 or 0 under
 * [storeGuessedRightAs].
 *
 * The whole [effectContext] is carried rather than rebuilt, because the condition is usually written
 * against something an earlier step chose — Liar's Pendulum's guess is about a card *name* held in
 * `chosenValues`, and a fresh context would lose it and silently score every guess as wrong.
 *
 * @property condition The proposition, still unevaluated at the moment this frame is pushed.
 * @property storeGuessedRightAs Pipeline number written as 1 (guessed right) or 0 (guessed wrong).
 */
@Serializable
@SerialName("GuessConditionContinuation")
data class GuessConditionContinuation(
    override val decisionId: String,
    val guesserId: EntityId,
    val condition: com.wingedsheep.sdk.scripting.conditions.Condition,
    val storeGuessedRightAs: String,
    val effectContext: EffectContext,
) : ContinuationFrame
