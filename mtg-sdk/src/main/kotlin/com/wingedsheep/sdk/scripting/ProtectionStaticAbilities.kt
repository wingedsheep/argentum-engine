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
 * @property filter What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("GrantProtection")
@Serializable
data class GrantProtection(
    val color: Color,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "${filter.description} have protection from ${color.displayName.lowercase()}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
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
 * Grants each affected creature "hexproof from monocolored" — they can't be the targets of
 * monocolored (exactly one color, CR 105.2) spells or abilities opponents control. Colorless
 * and multicolored sources are unaffected.
 *
 * Used by Dragonfire Blade ("Equipped creature ... has hexproof from monocolored."). The
 * default filter applies it to the attached creature; pass a wider [GroupFilter] for cards that
 * blanket a group.
 *
 * @property filter The group of creatures that gain the hexproof
 */
@SerialName("GrantHexproofFromMonocoloredToGroup")
@Serializable
data class GrantHexproofFromMonocoloredToGroup(
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "${filter.description} have hexproof from monocolored"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants each affected creature protection from the colors of permanents the source's
 * controller currently controls. The protection set is board-derived and re-evaluated at
 * projection (after Layer 5), so it tracks the controller's permanents in real time; a
 * colorless permanent contributes no color.
 *
 * Used by Pledge of Loyalty ("Enchanted creature has protection from the colors of permanents
 * you control."). The "you" is the controller of the permanent holding this ability; the
 * default filter applies it to the enchanted creature.
 *
 * @property filter The group of creatures that gain the dynamic protection
 */
@SerialName("GrantProtectionFromControlledColors")
@Serializable
data class GrantProtectionFromControlledColors(
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String =
        "${filter.description} have protection from the colors of permanents you control"
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
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "${filter.description} can't have counters put on it"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
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
}

/**
 * Creatures matching [filter] can be the targets of spells and abilities as though
 * they didn't have hexproof.
 *
 * Used for Nowhere to Run: "Creatures your opponents control can be the targets of
 * spells and abilities as though they didn't have hexproof."
 *
 * The filter is evaluated with the controller of this permanent as the "you" context,
 * so [GroupFilter.AllCreaturesOpponentsControl] means "creatures opponents of the
 * controller of this permanent control" — which is what "your opponents" means.
 *
 * Does NOT suppress shroud (shroud still prevents targeting by all players).
 */
@SerialName("SuppressHexproofForGroup")
@Serializable
data class SuppressHexproofForGroup(
    val filter: GroupFilter = GroupFilter.AllCreaturesOpponentsControl
) : StaticAbility {
    override val description: String = "${filter.description} can be targeted as though they didn't have hexproof"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Ward abilities of creatures matching [filter] don't trigger.
 *
 * Used for Nowhere to Run: "Ward abilities of those creatures don't trigger."
 *
 * The filter is evaluated with the controller of this permanent as the "you" context.
 * Suppresses both intrinsic ward (from the creature's own keywords) and granted ward
 * (from GrantWard static abilities) for affected creatures.
 */
@SerialName("SuppressWardForGroup")
@Serializable
data class SuppressWardForGroup(
    val filter: GroupFilter = GroupFilter.AllCreaturesOpponentsControl
) : StaticAbility {
    override val description: String = "Ward abilities of ${filter.description} don't trigger"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}
