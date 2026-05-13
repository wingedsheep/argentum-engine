package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Turn/Phase Conditions
// =============================================================================

/**
 * Condition: "If it's your turn"
 */
@SerialName("IsYourTurn")
@Serializable
data object IsYourTurn : Condition {
    override val description: String = "if it's your turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If it's not your turn"
 */
@SerialName("IsNotYourTurn")
@Serializable
data object IsNotYourTurn : Condition {
    override val description: String = "if it's not your turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If the current phase matches any of the listed phases"
 * When `yoursOnly = true` (default), also requires that it's the controller's turn —
 * i.e. "your main phase" means it's both your turn AND the main phase.
 * Used for cards like Dose of Dawnglow ("if it isn't your main phase").
 */
@SerialName("IsInPhase")
@Serializable
data class IsInPhase(
    val phases: List<Phase>,
    val yoursOnly: Boolean = true
) : Condition {
    override val description: String = buildString {
        append("if it's ")
        if (yoursOnly) append("your ")
        append(phases.joinToString(" or ") { it.displayName.removeSuffix(" Phase").lowercase() })
        if (phases.any { !it.isMainPhase } || phases.size > 1) append(" phase")
    }
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

// =============================================================================
// Combat Conditions
// =============================================================================

/**
 * Condition: "If you've been attacked this step"
 * Used for cards like Defiant Stand and Harsh Justice that can only be cast
 * during the declare attackers step if you've been attacked.
 */
@SerialName("YouWereAttackedThisStep")
@Serializable
data object YouWereAttackedThisStep : Condition {
    override val description: String = "if you've been attacked this step"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If you attacked with [atLeast] or more creatures matching [filter] this turn".
 * Counts every creature you declared as an attacker this turn whose current state
 * (per the projected state) matches the filter.
 *
 * Used for cards like Deepway Navigator: "as long as you attacked with three or more
 * Merfolk this turn".
 */
@SerialName("YouAttackedWithCreaturesThisTurn")
@Serializable
data class YouAttackedWithCreaturesThisTurn(
    val filter: GameObjectFilter,
    val atLeast: Int
) : Condition {
    override val description: String = "if you attacked with $atLeast or more ${DynamicAmount.pluralize(filter.description)} this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Condition: "If you've cast [atLeast] or more spells matching [filter] this turn".
 * Counts the controller's `CastSpellRecord`s captured at cast time, so every spell
 * cast counts even if it was countered, fizzled, or is still on the stack.
 *
 * Used for cards like Brightspear Zealot ("as long as you've cast two or more
 * spells this turn") and Illvoi Infiltrator ("if you've cast two or more spells
 * this turn"). Pass `GameObjectFilter.Any` for the unfiltered "any spell" form.
 */
@SerialName("YouCastSpellsThisTurn")
@Serializable
data class YouCastSpellsThisTurn(
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val atLeast: Int
) : Condition {
    override val description: String =
        if (filter == GameObjectFilter.Any)
            "if you've cast $atLeast or more spells this turn"
        else
            "if you've cast $atLeast or more ${DynamicAmount.pluralize(filter.description)} spells this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Condition: "if this is the first spell matching the given filter cast by you this turn"
 * Checks the per-player spell record tracker. The condition is true if exactly one spell
 * matching the filter has been cast (just this spell). Used for Alania, Divergent Storm.
 */
@SerialName("IsFirstSpellOfTypeCastThisTurn")
@Serializable
data class IsFirstSpellOfTypeCastThisTurn(
    val spellFilter: GameObjectFilter
) : Condition {
    override val description: String = "if it's the first ${spellFilter.description} spell you've cast this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

// =============================================================================
// Ability Resolution Conditions
// =============================================================================

/**
 * Condition: "if this is the Nth time this ability has resolved this turn"
 * Checks the AbilityResolutionCountThisTurnComponent on the source entity.
 * Used for cards like Harvestrite Host.
 */
@SerialName("SourceAbilityResolvedNTimesThisTurn")
@Serializable
data class SourceAbilityResolvedNTimesThisTurn(val count: Int) : Condition {
    override val description: String = "if this is the ${ordinal(count)} time this ability has resolved this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this

    private fun ordinal(n: Int): String = when (n) {
        1 -> "first"
        2 -> "second"
        3 -> "third"
        else -> "${n}th"
    }
}

// =============================================================================
// Void (Edge of Eternities ability word)
// =============================================================================

/**
 * Condition: "if a nonland permanent left the battlefield this turn or
 * a spell was warped this turn".
 *
 * Backs the Void ability word from Edge of Eternities. The condition is global — it
 * is satisfied by any player's nonland permanent leaving the battlefield (tokens
 * count, lands do not) or any spell that was cast for its warp cost this turn,
 * even if that spell was countered.
 */
@SerialName("Void")
@Serializable
data object VoidCondition : Condition {
    override val description: String =
        "if a nonland permanent left the battlefield this turn or a spell was warped this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

// =============================================================================
// Stack Conditions
// =============================================================================

/**
 * Condition: "If an opponent has cast a spell (it's on the stack)"
 * Used for Portal counterspells like Mystic Denial that can only be cast
 * in response to an opponent's spell.
 */
@SerialName("OpponentSpellOnStack")
@Serializable
data object OpponentSpellOnStack : Condition {
    override val description: String = "if an opponent has cast a spell"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

// =============================================================================
// Death Conditions
// =============================================================================

/**
 * Intervening-if condition (Rule 603.4): "if a creature died this turn".
 * True when the controlling player's CreaturesDiedThisTurnComponent has count > 0.
 * Evaluated both at trigger time and at resolution per Rule 603.4.
 */
@SerialName("CreatureDiedThisTurn")
@Serializable
data object CreatureDiedThisTurnCondition : Condition {
    override val description: String = "if a creature died this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

