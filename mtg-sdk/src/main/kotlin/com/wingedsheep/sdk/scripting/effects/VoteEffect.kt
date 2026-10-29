package com.wingedsheep.sdk.scripting.effects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Vote (CR 701.38): "Starting with you, each player votes for [choices]". On resolution each player
 * still in the game, starting with the controller and proceeding in turn order (CR 701.38a), picks
 * exactly one of [choices] — nobody may abstain — and every vote is announced as it is cast, so a
 * later voter knows the earlier votes. [voteCounts] maps pipeline number names to the choice they
 * tally: once everyone has voted, each name holds how many votes that choice received (0 when
 * nobody picked it), for later steps to read through `DynamicAmount.VariableReference`.
 *
 * This is the word vote — choices that are "words with no rules meaning that are each connected to
 * a different effect" (CR 701.38b). Council's dilemma spends every tally (Selvala's Stampede: one
 * creature per *wild* vote, one permanent per *free* vote); will of the council compares them with
 * `Conditions.CompareAmounts`. Voting for objects (Council's Judgment), extra votes, and who voted
 * for what (Expropriate) are not modelled here. A secret ballot is the other vote shape
 * ([SecretChoiceEffect], Vault 11: Voter's Dilemma); a future vote trigger or extra-vote effect has
 * to cover both.
 *
 * Closest types: [SecretChoiceEffect] collects *secret* ballots in APNAP order and reveals them at
 * once, where this vote is public and cast one player at a time starting with you; [ChooseOptionEffect] has one chooser and stores a
 * single string, not a tally per choice. [RollDiceEffect] publishes per-outcome tallies the same
 * way, which is the shape this follows.
 */
@Serializable
@SerialName("Vote")
data class VoteEffect(
    val choices: List<String>,
    val voteCounts: Map<String, String> = emptyMap(),
) : Effect {
    init {
        require(choices.size >= 2) { "A vote needs at least two choices" }
        require(choices.none { it.isBlank() } && choices.toSet().size == choices.size) { "Vote choices must be distinct words" }
        require(voteCounts.values.all { it in choices }) { "A vote count must name one of the choices" }
    }

    override val description: String = "Starting with you, each player votes for " + when (choices.size) {
        2 -> "${choices[0]} or ${choices[1]}"
        else -> choices.dropLast(1).joinToString(", ") + ", or " + choices.last()
    }
}
