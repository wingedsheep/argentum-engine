package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The value each participant commits privately before all choices are revealed together.
 * Numeric choices generalize the former SecretBidEffect. Collection choices share its private
 * commitment/reveal interaction; SelectFromCollectionEffect alone selects for one chooser and
 * neither aggregates ballots nor keeps earlier participants' choices secret.
 */
@Serializable
sealed interface SecretChoice {
    /**
     * A bounded integer choice. Highest/lowest/tied branches run per qualifying participant with
     * controller = that participant and X = their number. [outcomeMinValue] limits the values
     * included in ranking, retaining the old bid behavior of excluding zero by default.
     * [tiedEffect] runs for every ranked participant iff all ranked values are equal.
     * Matched/unmatched branches compare ALL participants' numbers, including unranked values,
     * and run once in the original effect context. Empty participant groups run neither branch.
     */
    @SerialName("SecretNumberChoice")
    @Serializable
    data class Number(
        val minValue: Int = 0,
        val maxValue: DynamicAmount = DynamicAmount.Fixed(99),
        val outcomeMinValue: Int = 1,
        val highestEffect: Effect? = null,
        val lowestEffect: Effect? = null,
        val tiedEffect: Effect? = null,
        val matchedEffect: Effect? = null,
        val unmatchedEffect: Effect? = null,
    ) : SecretChoice

    /**
     * Each participant chooses one entity from [from], or abstains when [optional] is true.
     * [storeMostChosen] receives each distinct entity with the greatest positive ballot count,
     * including every tied winner; it is empty when everyone abstains or the pool is empty.
     * This is a choice, not targeting. The outer follow-up consumes the resulting collection
     * under the original context, permitting simultaneous movement of all winning entities.
     */
    @SerialName("SecretEntityChoice")
    @Serializable
    data class Entity(
        val from: String,
        val optional: Boolean = true,
        val storeMostChosen: String,
    ) : SecretChoice
}

/**
 * Collect private choices in APNAP order, reveal them simultaneously, then execute outcomes.
 * Replaces SecretBidEffect rather than introducing a parallel private-choice interaction.
 * ChooseNumberThenEffect selects a single public number and cannot compare private commitments;
 * SelectFromCollectionEffect selects for one player and cannot tally multiple secret ballots.
 * Participant references are unioned, deduplicated, and ordered by APNAP; all branches and [then]
 * retain the original targets, stored collections, and object references.
 */
@SerialName("SecretChoice")
@Serializable
data class SecretChoiceEffect(
    val participants: List<Player> = listOf(Player.Each),
    val choice: SecretChoice,
    val then: Effect? = null,
) : Effect {
    override val description: String = buildString {
        val players = participants.joinToString(" and ") { it.description }
        when (val spec = choice) {
            is SecretChoice.Number -> {
                append("$players secretly choose a number. Then those numbers are revealed.")
                spec.highestEffect?.let { append(" Each player with the highest number ${it.description}.") }
                spec.lowestEffect?.let { append(" Each player with the lowest number ${it.description}.") }
                spec.tiedEffect?.let { append(" If there is a tie, each tied player ${it.description}.") }
                spec.matchedEffect?.let { append(" If the numbers match, ${it.description}.") }
                spec.unmatchedEffect?.let { append(" Otherwise, ${it.description}.") }
            }
            is SecretChoice.Entity -> {
                val count = if (spec.optional) "up to one" else "one"
                append("$players secretly choose $count object. Then those choices are revealed.")
            }
        }
        then?.let { append(" ${it.description}") }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = copy(
        choice = when (val spec = choice) {
            is SecretChoice.Number -> spec.copy(
                maxValue = spec.maxValue.applyTextReplacement(replacer),
                highestEffect = spec.highestEffect?.applyTextReplacement(replacer),
                lowestEffect = spec.lowestEffect?.applyTextReplacement(replacer),
                tiedEffect = spec.tiedEffect?.applyTextReplacement(replacer),
                matchedEffect = spec.matchedEffect?.applyTextReplacement(replacer),
                unmatchedEffect = spec.unmatchedEffect?.applyTextReplacement(replacer),
            )
            is SecretChoice.Entity -> spec
        },
        then = then?.applyTextReplacement(replacer),
    )
}
