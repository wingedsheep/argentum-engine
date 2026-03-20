package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Take an extra turn after this one, with a consequence at end of turn.
 * Used for Last Chance: "Take an extra turn after this one. At the beginning of that turn's end step, you lose the game."
 *
 * @param loseAtEndStep If true, you lose the game at the beginning of that turn's end step
 * @param target The player who takes the extra turn. Defaults to the controller.
 */
@SerialName("TakeExtraTurn")
@Serializable
data class TakeExtraTurnEffect(
    val loseAtEndStep: Boolean = false,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = buildString {
        append("Take an extra turn after this one")
        if (loseAtEndStep) {
            append(". At the beginning of that turn's end step, you lose the game")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newAbility !== ability) copy(ability = newAbility) else this
    }
}

/**
 * Create a global triggered ability that lasts permanently.
 * Used for effects that create recurring triggers from non-permanent sources
 * (e.g., Dimensional Breach creating an upkeep trigger from a sorcery).
 *
 * "Whenever [trigger], [effect]." (permanent duration)
 *
 * @property ability The triggered ability to create
 * @property descriptionOverride Optional override for the auto-generated description
 */
@SerialName("CreatePermanentGlobalTriggeredAbility")
@Serializable
data class CreatePermanentGlobalTriggeredAbilityEffect(
    val ability: TriggeredAbility,
    val descriptionOverride: String? = null
) : Effect {
    override val description: String = descriptionOverride ?: ability.description

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newAbility !== ability) copy(ability = newAbility) else this
    }
}

/**
 * Target player skips their next turn.
 * Used for cards like Lethal Vapors: "You skip your next turn."
 *
 * @param target The player who skips their next turn (default: the controller/activating player)
 */
@SerialName("SkipNextTurn")
@Serializable
data class SkipNextTurnEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} skips their next turn"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Target player can't cast spells for the specified duration.
 * Used for cards like Xantid Swarm: "Whenever this creature attacks, defending player can't cast spells this turn."
 *
 * @param target The player who can't cast spells
 * @param duration How long the restriction lasts (default: EndOfTurn)
 */
@SerialName("CantCastSpells")
@Serializable
data class CantCastSpellsEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.Opponent),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} can't cast spells ${duration.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Target player loses the game.
 * Used for cards like Phage the Untouchable: "that player loses the game."
 *
 * @param target The player who loses the game
 * @param message Optional message describing why they lost (shown in game-over screen)
 */
@SerialName("LoseGame")
@Serializable
data class LoseGameEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val message: String? = null
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} loses the game"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Grants hexproof to a target entity for the specified duration.
 * "You gain hexproof until end of turn."
 *
 * Used for cards like Dawn's Truce: "You and permanents you control gain hexproof until end of turn."
 *
 * - For player targets: adds PlayerHexproofComponent with appropriate removal timing
 * - For permanent targets: creates a floating effect granting the Hexproof keyword
 *
 * @param target The entity to grant hexproof to (player, creature, or planeswalker)
 * @param duration How long the hexproof lasts
 */
@SerialName("GrantHexproof")
@Serializable
data class GrantHexproofEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "${target.description.replaceFirstChar { it.uppercase() }} gains hexproof ${duration.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Grant a flat damage bonus to a player's sources for the specified duration.
 * When a source matching the filter that the player controls would deal damage,
 * it deals that much damage plus the bonus amount instead.
 *
 * Used for cards like The Flame of Keld (Chapter III): "If a red source you control
 * would deal damage to a permanent or player this turn, it deals that much damage plus 2 instead."
 *
 * @param bonusAmount The flat damage bonus to add
 * @param sourceFilter Filter for which sources get the bonus (e.g., SourceFilter.HasColor(Color.RED))
 * @param target The player who gets the damage bonus (default: controller)
 * @param duration How long the bonus lasts (default: EndOfTurn)
 */
@SerialName("GrantDamageBonus")
@Serializable
data class GrantDamageBonusEffect(
    val bonusAmount: Int,
    val sourceFilter: SourceFilter = SourceFilter.Any,
    val target: EffectTarget = EffectTarget.Controller,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("If ${sourceFilter.description} ${target.description} controls would deal damage to a permanent or player")
        append(", it deals that much damage plus $bonusAmount instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
