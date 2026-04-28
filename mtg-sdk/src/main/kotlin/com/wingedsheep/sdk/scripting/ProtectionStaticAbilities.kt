package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Grants protection from a color to the target creature.
 * Used for auras like Crown of Awe: "Enchanted creature has protection from black and from red."
 *
 * @property color The color to grant protection from
 * @property target What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("GrantProtection")
@Serializable
data class GrantProtection(
    val color: Color,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "Grants protection from ${color.displayName.lowercase()}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Grants protection from the chosen color to a group of creatures.
 * Used for "As this enters, choose a color. [Group] have protection from the chosen color."
 * The chosen color is stored on the permanent via ChosenColorComponent and resolved dynamically.
 * Example: Ward Sliver (all Slivers have protection from the chosen color)
 *
 * @property filter The group of creatures that gain protection
 */
@SerialName("GrantProtectionFromChosenColorToGroup")
@Serializable
data class GrantProtectionFromChosenColorToGroup(
    val filter: GroupFilter = GroupFilter(GameObjectFilter.Companion.Creature.youControl())
) : StaticAbility {
    override val description: String = "Creatures of the chosen group have protection from the chosen color"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants each affected creature "hexproof from each of its colors" — i.e., for every color
 * the creature currently has (after Layer 5), it also has hexproof from that color.
 *
 * Used by Tam, Mindful First-Year ("Each other creature you control has hexproof from each of
 * its colors.") — the protection set varies per creature and changes if a creature's colors
 * change. Colorless is not a color, so a colorless creature gains no hexproof.
 *
 * The grant is applied in Layer 6 (ABILITY) after colors are finalized in Layer 5, so the
 * keyword set reflects the projected colors of each affected creature.
 *
 * @property filter The group of creatures that gain the dynamic hexproof
 */
@SerialName("GrantHexproofFromOwnColorsToGroup")
@Serializable
data class GrantHexproofFromOwnColorsToGroup(
    val filter: GroupFilter = GroupFilter(GameObjectFilter.Companion.Creature.youControl(), excludeSelf = true)
) : StaticAbility {
    override val description: String = "${filter.description} have hexproof from each of their colors"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Prevents a permanent from having counters put on it.
 * Used for Auras like Blossombind.
 */
@SerialName("CantReceiveCounters")
@Serializable
data class CantReceiveCounters(
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "${target.toString().lowercase()} can't have counters put on it"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * You have shroud. (You can't be the target of spells or abilities.)
 * Grants shroud to the permanent's controller (player-level shroud).
 * Used for True Believer.
 */
@SerialName("GrantShroudToController")
@Serializable
data object GrantShroudToController : StaticAbility {
    override val description: String = "You have shroud"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * You have hexproof. (You can't be the target of spells or abilities your opponents control.)
 * Grants hexproof to the permanent's controller (player-level hexproof).
 * Unlike shroud, the controller can still target themselves.
 * Used for Shalai, Voice of Plenty.
 */
@SerialName("GrantHexproofToController")
@Serializable
data object GrantHexproofToController : StaticAbility {
    override val description: String = "You have hexproof"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This permanent can't be the target of abilities your opponents control.
 * Unlike hexproof, this does NOT prevent targeting by spells — only by activated
 * and triggered abilities.
 * Used for Shanna, Sisay's Legacy.
 */
@SerialName("CantBeTargetedByOpponentAbilities")
@Serializable
data object CantBeTargetedByOpponentAbilities : StaticAbility {
    override val description: String = "Can't be the target of abilities your opponents control"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * You can't lose the game.
 * Grants the "can't lose the game" effect to the permanent's controller.
 * Used for Lich's Mastery, Platinum Angel, etc.
 *
 * When active, prevents all game loss conditions: life at 0 or less,
 * 10+ poison counters, drawing from empty library, and card effects.
 * Note: opponents can still WIN the game via effects that say so.
 */
@SerialName("GrantCantLoseGame")
@Serializable
data object GrantCantLoseGame : StaticAbility {
    override val description: String = "You can't lose the game"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}
