package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Player Effects
// =============================================================================

/**
 * Target player skips their combat phases during their next turn.
 * Used for cards like False Peace.
 */
@SerialName("SkipCombatPhases")
@Serializable
data class SkipCombatPhasesEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetPlayer)
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} skips all combat phases of their next turn"
}

/**
 * Target player's creatures and lands don't untap during their next untap step.
 * Used for cards like Exhaustion.
 */
@SerialName("SkipUntap")
@Serializable
data class SkipUntapEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent),
    val affectsCreatures: Boolean = true,
    val affectsLands: Boolean = true
) : Effect {
    override val description: String = buildString {
        val affectedTypes = listOfNotNull(
            if (affectsCreatures) "Creatures" else null,
            if (affectsLands) "lands" else null
        ).joinToString(" and ")
        append("$affectedTypes ${target.description} controls don't untap during their next untap step")
    }
}

/**
 * You may play additional lands this turn.
 * Used for Summer Bloom: "You may play up to three additional lands this turn."
 *
 * @param count The number of additional lands you may play
 */
@SerialName("PlayAdditionalLands")
@Serializable
data class PlayAdditionalLandsEffect(
    val count: Int
) : Effect {
    override val description: String = "You may play up to $count additional land${if (count != 1) "s" else ""} this turn"
}

/**
 * Add an additional combat phase followed by an additional main phase after the current main phase.
 * Used for Aggravated Assault: "{3}{R}{R}: Untap all creatures you control. After this main phase,
 * there is an additional combat phase followed by an additional main phase."
 */
@SerialName("AddCombatPhase")
@Serializable
data object AddCombatPhaseEffect : Effect {
    override val description: String =
        "After this main phase, there is an additional combat phase followed by an additional main phase"
}

/**
 * Take an extra turn after this one, with a consequence at end of turn.
 * Used for Last Chance: "Take an extra turn after this one. At the beginning of that turn's end step, you lose the game."
 *
 * @param loseAtEndStep If true, you lose the game at the beginning of that turn's end step
 */
@SerialName("TakeExtraTurn")
@Serializable
data class TakeExtraTurnEffect(
    val loseAtEndStep: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Take an extra turn after this one")
        if (loseAtEndStep) {
            append(". At the beginning of that turn's end step, you lose the game")
        }
    }
}

/**
 * Prevent the controller from playing lands for the rest of this turn.
 * Sets the player's remaining land drops to 0.
 * Used for cards like Rock Jockey.
 */
@SerialName("PreventLandPlaysThisTurn")
@Serializable
data object PreventLandPlaysThisTurnEffect : Effect {
    override val description: String = "You can't play lands this turn"
}

/**
 * Create a global triggered ability that lasts until end of turn.
 * Used for spells like False Cure that create delayed triggered abilities
 * not attached to any specific permanent.
 *
 * "Until end of turn, whenever [trigger], [effect]."
 *
 * @property ability The triggered ability to create
 */
@SerialName("CreateGlobalTriggeredAbilityUntilEndOfTurn")
@Serializable
data class CreateGlobalTriggeredAbilityUntilEndOfTurnEffect(
    val ability: TriggeredAbility
) : Effect {
    override val description: String =
        "Until end of turn, ${ability.description.replaceFirstChar { it.lowercase() }}"
}

/**
 * Grant shroud to a target entity for the specified duration.
 * Works for players, creatures, and planeswalkers.
 *
 * Used for cards like Gilded Light: "You gain shroud until end of turn."
 *
 * - For player targets: adds PlayerShroudComponent with appropriate removal timing
 * - For permanent targets: creates a floating effect granting the Shroud keyword
 *
 * @param target The entity to grant shroud to (player, creature, or planeswalker)
 * @param duration How long the shroud lasts
 */
@SerialName("GrantShroud")
@Serializable
data class GrantShroudEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} gains shroud ${duration.description}"
}
