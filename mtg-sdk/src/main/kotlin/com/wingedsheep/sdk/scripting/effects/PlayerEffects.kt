package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

// =============================================================================
// Player Effects
// =============================================================================

/**
 * Target player skips their combat phases during their next turn.
 * Used for cards like False Peace.
 */
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
@Serializable
data class PlayAdditionalLandsEffect(
    val count: Int
) : Effect {
    override val description: String = "You may play up to $count additional land${if (count != 1) "s" else ""} this turn"
}

/**
 * Take an extra turn after this one, with a consequence at end of turn.
 * Used for Last Chance: "Take an extra turn after this one. At the beginning of that turn's end step, you lose the game."
 *
 * @param loseAtEndStep If true, you lose the game at the beginning of that turn's end step
 */
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
