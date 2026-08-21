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

/**
 * "[guesser] guesses whether [condition] is true." The answer is compared to reality and `1` or `0`
 * is stored under [storeGuessedRightAs], leaving the payoff for a right or a wrong guess to the card
 * rather than baking it in here.
 *
 * The open sibling of [OpponentGuessesTopCardKindEffect]. That one is a *closed* guess: it owns its
 * proposition (land or nonland), owns the reveal, and owns both branches. This one owns none of them —
 * the proposition is any resolution-time [Condition] and the outcome is a pipeline number. That is
 * what lets a card put its own steps *between* the guess and the payoff, which a branch-carrying
 * effect cannot: Liar's Pendulum's "You may reveal your hand. If you do and your opponent guessed
 * wrong, draw a card" has an optional reveal in the middle, and folding it into the branches would
 * duplicate the reveal on both sides and make the two prompts distinguishable.
 *
 * The guess is a real guess: [condition] is evaluated only *after* the answer is in, and its truth is
 * never shown to the guesser. This effect reveals nothing on its own — a card that wants the truth
 * made public spells the reveal out itself.
 *
 * `1` for right and `0` for wrong makes both readings direct: gate on `EQ 1` for "guessed right" and
 * `EQ 0` for "guessed wrong". A consumer that runs without a guess having happened reads 0 — i.e.
 * "guessed wrong" — so order the guess before its consumers and never lean on the default.
 *
 * @property condition The proposition guessed about, evaluated at resolution in this effect's context.
 * @property prompt The question put to the guesser. `{name}` is substituted when [promptNameVariable] is set.
 * @property storeGuessedRightAs Pipeline number written as 1 (right) or 0 (wrong).
 * @property guesser Who guesses; the printed "target opponent" is [Chooser.TargetPlayer].
 * @property promptNameVariable When set, `{name}` in [prompt] is replaced by the card name stored in
 *   `chosenValues` under this key, so a guess about a named card can put that name in the question.
 */
@Serializable
@SerialName("PlayerGuessesConditionEffect")
data class PlayerGuessesConditionEffect(
    val condition: com.wingedsheep.sdk.scripting.conditions.Condition,
    val prompt: String,
    val storeGuessedRightAs: String = "guessedRight",
    val guesser: Chooser = Chooser.Opponent,
    val promptNameVariable: String? = null,
) : Effect {
    override val description: String = "A player guesses whether ${condition.description}"
}
