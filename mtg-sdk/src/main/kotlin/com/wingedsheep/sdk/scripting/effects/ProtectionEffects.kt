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
    val prompt: String = "Choose a color"
) : Effect {
    override val description: String = "Choose a color. ${then.description}"

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
    is ProtectionScope.CardType -> cardType.lowercase() + "s"
    is ProtectionScope.Subtype -> subtype + "s"
    is ProtectionScope.Supertype -> supertype.lowercase() + " permanents"
    ProtectionScope.Everything -> "everything"
    ProtectionScope.EachOpponent -> "each opponent"
}
