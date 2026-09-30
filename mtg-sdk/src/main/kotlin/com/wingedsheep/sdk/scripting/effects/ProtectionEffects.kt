package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Protection Effects
// =============================================================================

/**
 * Choose a color, then run [then] with the chosen color exposed on the
 * [com.wingedsheep.sdk.scripting.targets.EffectTarget] context. Atomic effects under
 * [then] (e.g., [GrantHexproofFromChosenColorEffect]) read the color from context
 * to apply per-color modifications.
 *
 * Use this combinator instead of inventing a new monolithic "choose color, then do
 * X+Y+Z" effect for every card. Compose with [CompositeEffect] when [then] is a
 * sequence of grants.
 */
@SerialName("ChooseColorThen")
@Serializable
data class ChooseColorThenEffect(
    val then: Effect,
    val prompt: String = "Choose a color",
    /**
     * How many distinct colors the player may pick. `1` (the default) is the ordinary
     * "choose a color". A larger value makes it "the color **or colors** of your choice" — any
     * nonempty set of up to [maxColors] colors (Quickchange; per its ruling colorless is not a
     * choice, so at least one color is always picked). The whole set reaches [then] as
     * `EffectContext.chosenColors`; `chosenColor` carries one of them for single-color atoms.
     */
    val maxColors: Int = 1
) : Effect {
    init {
        require(maxColors >= 1) { "ChooseColorThenEffect.maxColors must be at least 1" }
    }

    override val description: String =
        if (maxColors > 1) "Choose one or more colors. ${then.description}"
        else "Choose a color. ${then.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newThen = then.applyTextReplacement(replacer)
        return if (newThen !== then) copy(then = newThen) else this
    }
}

/**
 * Grant "hexproof from the chosen color" to a target. Must run inside a
 * [ChooseColorThenEffect] — the executor reads the chosen color from the
 * effect context. Resolves to a `HEXPROOF_FROM_<COLOR>` keyword grant.
 */
@SerialName("GrantHexproofFromChosenColor")
@Serializable
data class GrantHexproofFromChosenColorEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains hexproof from the chosen color")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Grant "protection from the chosen color" to a target. Must run inside a
 * [ChooseColorThenEffect] — the executor reads the chosen color from the
 * effect context. Resolves to a `PROTECTION_FROM_<COLOR>` keyword grant.
 */
@SerialName("GrantProtectionFromChosenColor")
@Serializable
data class GrantProtectionFromChosenColorEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains protection from the chosen color")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Grant "protection from the card type of your choice" to a target (CR 702.16).
 *
 * The executor presents a [com.wingedsheep.sdk.scripting] choose-option decision over the
 * meaningfully-protectable card types (Artifact, Creature, Enchantment, Instant, Land,
 * Planeswalker, Sorcery, Battle) and, on response, grants a floating
 * `PROTECTION_FROM_CARDTYPE_<TYPE>` keyword for [duration]. This is the card-type analogue of
 * [GrantProtectionFromChosenColorEffect] — used by Pippin, Guard of the Citadel.
 *
 * Unlike the chosen-color family this effect is self-contained (it owns its choice), because
 * the card-type option set is fixed and shared, so there is no general "choose card type, then
 * run X" combinator to compose under.
 *
 * @property target The creature to protect (defaults to the ability's first target).
 * @property duration How long the protection lasts (defaults to end of turn).
 */
@SerialName("GrantProtectionFromChosenCardType")
@Serializable
data class GrantProtectionFromChosenCardTypeEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains protection from the card type of your choice")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    companion object {
        /** Meaningfully-protectable card types presented as the fixed choice set (CR 205.2a). */
        val PROTECTABLE_CARD_TYPES: List<String> = listOf(
            "Artifact", "Creature", "Enchantment", "Instant",
            "Land", "Planeswalker", "Sorcery", "Battle"
        )
    }
}

/**
 * Grant "protection from colorless or from the color of your choice" to a target (CR 702.16).
 *
 * The colour-axis sibling of [GrantProtectionFromChosenCardTypeEffect], self-contained for the same
 * reason: the choice set is fixed — Colorless plus the five colors — and colorless is not a color
 * (CR 105.4), so the ordinary [ChooseColorThenEffect] can't offer it. The executor presents a
 * choose-option decision and, on response, grants a floating `PROTECTION_FROM_<COLOR>` or
 * `PROTECTION_FROM_COLORLESS` keyword for [duration]. Protection from colorless matches any source
 * with no colors — artifacts, Eldrazi, colorless spells and abilities of colorless sources — at
 * every protection read site (targeting, attaching, blocking, damage). Angelic Intervention.
 *
 * @property target The permanent to protect (defaults to the ability's first target).
 * @property duration How long the protection lasts (defaults to end of turn).
 */
@SerialName("GrantProtectionFromColorlessOrChosenColor")
@Serializable
data class GrantProtectionFromColorlessOrChosenColorEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains protection from colorless or from the color of your choice")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Grant a **player** protection from [scope] (CR 702.16) for [duration].
 *
 * The player-level counterpart of the creature protection statics — used by
 * The One Ring ("you gain protection from everything until your next turn").
 * The executor adds/merges a
 * [com.wingedsheep.engine.state.components.player.PlayerProtectionComponent] on the
 * target player; the targeting and damage systems consult it so the player can't be
 * targeted by, or dealt damage from, sources matching [scope].
 *
 * [scope] is any [ProtectionScope] — `Everything` (The One Ring), a single color,
 * `EachOpponent`, etc. Multiple grants stack (each appends to the player's scope list).
 *
 * @property target The player to protect (defaults to the controller).
 * @property scope The quality protected from.
 * @property duration How long the protection lasts; `UntilYourNextTurn` for The One Ring.
 */
@SerialName("GrantPlayerProtection")
@Serializable
data class GrantPlayerProtectionEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val scope: ProtectionScope = ProtectionScope.Everything,
    val duration: Duration = Duration.UntilYourNextTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains protection from ")
        append(scope.protectionDescription())
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/** Human-readable "from X" phrase for a [ProtectionScope], reused in effect descriptions. */
fun ProtectionScope.protectionDescription(): String = when (this) {
    is ProtectionScope.Color -> color.name.lowercase()
    is ProtectionScope.Colors -> colors.joinToString(" and ") { it.name.lowercase() }
    is ProtectionScope.NonColor -> "non" + color.name.lowercase()
    is ProtectionScope.CardType -> cardType.lowercase() + "s"
    is ProtectionScope.Subtype -> subtype + "s"
    is ProtectionScope.Supertype -> supertype.lowercase() + " permanents"
    ProtectionScope.Everything -> "everything"
    ProtectionScope.EachOpponent -> "each opponent"
}

/**
 * [target] gains, for [duration], **every protection ability** that some permanent in [group]
 * has — "creatures you control gain protection until end of turn if a creature you control has
 * protection" (Concerted Effort). Protection is parameterised (CR 702.16a: "protection from
 * [quality]"), so it can't be one fixed `Keyword` grant: the executor reads each [group] member's
 * projected protection abilities — from a colour, a card type, a subtype, a supertype, each
 * opponent — and grants every distinct one it finds. Per the Concerted Effort ruling, a creature
 * with protection from red and one with protection from green give every creature *both*.
 *
 * The set is read once, at resolution, and granted as ordinary keyword grants for [duration], so
 * the gained protections outlast the creature that supplied them. Fan it over a group with
 * `Effects.ForEachInGroup(group, Effects.GrantProtectionsSharedByGroup(group, IterationEntity))`.
 * An empty group, or one with no protection, grants nothing.
 */
@SerialName("GrantProtectionsSharedByGroup")
@Serializable
data class GrantProtectionsSharedByGroupEffect(
    val group: com.wingedsheep.sdk.scripting.filters.unified.GroupFilter,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains each protection ability that ")
        append(group.description.replaceFirstChar { it.lowercase() })
        append(" have")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newGroup = group.applyTextReplacement(replacer)
        return if (newGroup !== group) copy(group = newGroup) else this
    }
}
