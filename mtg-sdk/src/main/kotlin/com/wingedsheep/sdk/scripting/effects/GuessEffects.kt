package com.wingedsheep.sdk.scripting.effects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The kind a card can be categorised as for a land/nonland guess.
 */
@Serializable
enum class CardKind {
    LAND,
    NONLAND,
}

/**
 * "Choose land or nonland. An opponent guesses whether the top card of your library is the
 * chosen kind. Reveal that card. If they guessed right, [onGuessedRight]. Otherwise,
 * [onGuessedWrong]."
 *
 * A reusable opponent-guess primitive (Gollum, Scheming Guide). The controller (or whichever
 * [Chooser] is named) first picks a [CardKind]; then the guessing player ([guesser], the
 * opponent by default) guesses the kind of the *top card of the controller's library*; that
 * card is then revealed and its actual kind compared against the guess. A correct guess means
 * the guessed kind equals the top card's actual kind — note the controller's chosen kind only
 * frames the question ("is the top card the chosen kind?"), so a "right" guess is one where the
 * guesser's land/nonland call matches reality.
 *
 * Both branch effects resolve in the original effect context (source + targets preserved), so
 * `EffectTarget.Self` / `EffectTarget.ContextTarget` inside them refer to this ability's source.
 *
 * Edge cases:
 * - Empty library: there is no top card, so the guess can never be "right"; the [onGuessedWrong]
 *   branch runs (with no card revealed). This matches the rules — a player simply guesses about
 *   a card that isn't there.
 */
@Serializable
@SerialName("OpponentGuessesTopCardKindEffect")
data class OpponentGuessesTopCardKindEffect(
    val onGuessedRight: Effect,
    val onGuessedWrong: Effect,
    val chooser: Chooser = Chooser.Controller,
    val guesser: Chooser = Chooser.Opponent,
) : Effect {
    override val description: String =
        "Choose land or nonland. An opponent guesses whether the top card of your library is the " +
            "chosen kind. Reveal that card."
}
