package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Permanent State Transformation Effects
// (animation, morph, equipment, exile-on-leave)
// =============================================================================

/**
 * Target land becomes an X/Y creature until end of turn. It's still a land.
 * Used for Kamahl, Fist of Krosa: "{G}: Target land becomes a 1/1 creature until end of turn. It's still a land."
 *
 * Creates two floating effects:
 * 1. Layer.TYPE + AddType("Creature") - adds the Creature type
 * 2. Layer.POWER_TOUGHNESS + Sublayer.SET_VALUES + SetPowerToughness - sets base P/T
 *
 * @property target The land to animate
 * @property power The base power to set
 * @property toughness The base toughness to set
 * @property duration How long the effect lasts
 */
@SerialName("AnimateLand")
@Serializable
data class AnimateLandEffect(
    val target: EffectTarget,
    val power: Int = 1,
    val toughness: Int = 1,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} becomes a $power/$toughness creature")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
        append(". It's still a land.")
    }
}

/**
 * Target permanent becomes a creature with specified characteristics until end of turn.
 * More general than AnimateLandEffect — can also remove types (e.g., Planeswalker),
 * grant keywords, set subtypes, and change color.
 *
 * Used for Sarkhan, the Dragonspeaker's +1: "becomes a legendary 4/4 red Dragon creature
 * with flying, indestructible, and haste."
 *
 * Creates floating effects across multiple layers:
 * - Layer.TYPE: AddType("CREATURE"), RemoveType for each removeType, SetCreatureSubtypes
 * - Layer.COLOR: ChangeColor if colors specified
 * - Layer.ABILITY: GrantKeyword for each keyword
 * - Layer.POWER_TOUGHNESS + Sublayer.SET_VALUES: SetPowerToughness
 *
 * @property target The permanent to animate
 * @property power The base power to set
 * @property toughness The base toughness to set
 * @property keywords Keywords to grant (e.g., flying, indestructible, haste)
 * @property creatureTypes Creature subtypes to set (e.g., "Dragon")
 * @property removeTypes Types to remove (e.g., "PLANESWALKER")
 * @property colors Colors to set (null = keep existing)
 * @property duration How long the effect lasts
 */
@SerialName("BecomeCreature")
@Serializable
data class BecomeCreatureEffect(
    val target: EffectTarget = EffectTarget.Self,
    val power: Int,
    val toughness: Int,
    val keywords: Set<Keyword> = emptySet(),
    val creatureTypes: Set<String> = emptySet(),
    val removeTypes: Set<String> = emptySet(),
    val colors: Set<String>? = null,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} becomes a $power/$toughness creature")
        if (creatureTypes.isNotEmpty()) append(" ${creatureTypes.joinToString("/")}")
        if (keywords.isNotEmpty()) append(" with ${keywords.joinToString(", ") { it.name.lowercase() }}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Target permanent becomes saddled until end of turn (CR 702.171b). This is the resolving
 * effect of a Saddle ability: it stamps a transient "saddled" marker on the permanent (the
 * engine's `SaddledComponent`), which Mount payoffs read via `Conditions.SourceIsSaddled` /
 * `StatePredicate.IsSaddled`. The marker is engine state, not a copiable value, and is cleared
 * at end-of-turn cleanup or when the permanent leaves the battlefield.
 *
 * Defaults to [EffectTarget.Self] because a Saddle ability always saddles its own source.
 *
 * @property target The permanent to mark as saddled
 */
@SerialName("BecomeSaddled")
@Serializable
data class BecomeSaddledEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "${target.description} becomes saddled until end of turn"
}

/**
 * Turn target creature face down.
 * "Turn target creature with a morph ability face down."
 * Used for Backslide and similar effects.
 */
@SerialName("TurnFaceDown")
@Serializable
data class TurnFaceDownEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Turn ${target.description} face down"
}

/**
 * Turn target face-down creature face up.
 * "Turn target face-down creature an opponent controls face up."
 * Used for Break Open and similar effects.
 */
@SerialName("TurnFaceUp")
@Serializable
data class TurnFaceUpEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Turn ${target.description} face up"
}

/**
 * Attach this equipment to a target creature.
 * Detaches from the currently equipped creature (if any) before attaching to the new one.
 * "Attach to target creature you control."
 *
 * @property target The creature to attach to
 */
@SerialName("AttachEquipment")
@Serializable
data class AttachEquipmentEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Attach this equipment to ${target.description}"
}

/**
 * Attach a targeted Equipment to a targeted creature.
 * Unlike [AttachEquipmentEffect] which uses the source as the equipment,
 * this effect uses two explicit targets — one for the Equipment, one for the creature.
 * Used for Blacksmith's Talent Level 2: "attach target Equipment you control to up to one
 * target creature you control."
 *
 * @property equipmentTarget The Equipment to attach (e.g., ContextTarget(0))
 * @property creatureTarget The creature to attach it to (e.g., ContextTarget(1))
 */
@SerialName("AttachTargetEquipmentToCreature")
@Serializable
data class AttachTargetEquipmentToCreatureEffect(
    val equipmentTarget: EffectTarget = EffectTarget.ContextTarget(0),
    val creatureTarget: EffectTarget = EffectTarget.ContextTarget(1)
) : Effect {
    override val description: String = "Attach ${equipmentTarget.description} to ${creatureTarget.description}"
}

/**
 * Put a targeted Aura or Equipment card onto the battlefield **attached to a permanent the
 * effect's controller chooses** at resolution. The card is the [target] (e.g. a targeted
 * Aura/Equipment in a graveyard); the host is chosen as the effect resolves and is therefore
 * NOT a target — only the host filter constrains it ([hostFilter], default "a creature you
 * control"). Models "Return target Aura or Equipment card from your graveyard to the
 * battlefield attached to a creature you control" (One Last Job, Brass Squire-shaped attaches).
 *
 * Unlike the [com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect] aura auto-attach
 * (Rule 303.4f), which is Aura-only and uses the Aura's own enchant target, this effect
 * works for both Auras and Equipment and lets the card restrict the host. Per the One Last
 * Job ruling, the Aura/Equipment must be legally attachable to the chosen host:
 *  - If a legal host exists, the controller chooses one and the card enters attached to it.
 *  - If no legal host exists, an Equipment enters the battlefield unattached, while an Aura
 *    can't enter (it stays in its current zone — Rule 303.4g).
 *
 * @property target The Aura or Equipment card to put onto the battlefield (e.g. a graveyard target).
 * @property hostFilter Which permanents are eligible hosts (default: a creature you control).
 */
@SerialName("PutOntoBattlefieldAttachedToChosen")
@Serializable
data class PutOntoBattlefieldAttachedToChosenEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val hostFilter: GameObjectFilter = GameObjectFilter.Creature.youControl()
) : Effect {
    override val description: String =
        "Put ${target.description} onto the battlefield attached to ${hostFilter.description}"
}

/**
 * Mark a permanent so that if it would leave the battlefield, it is exiled instead.
 * Used by Kheru Lich Lord, Whip of Erebos, Sneak Attack, and similar reanimation effects.
 *
 * @property target The permanent to mark
 */
@SerialName("GrantExileOnLeave")
@Serializable
data class GrantExileOnLeaveEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "If ${target.description} would leave the battlefield, exile it instead of putting it anywhere else"
}

// =============================================================================
// Ability Resolution Tracking
// =============================================================================

/**
 * Increments the ability resolution count on the source permanent.
 * Used for cards that track "the Nth time this ability has resolved this turn"
 * (e.g., Harvestrite Host).
 */
@SerialName("IncrementAbilityResolutionCount")
@Serializable
data object IncrementAbilityResolutionCountEffect : Effect {
    override val description: String = "Track ability resolution"
}

// =============================================================================
// Explore
// =============================================================================

/**
 * Target creature explores.
 *
 * "Reveal the top card of your library. If it's a land card, put it into your hand.
 * Otherwise, put a +1/+1 counter on this creature, then put the card back on top of
 * your library or put it into your graveyard."
 *
 * The exploring player is the controller of the effect (the Map token's controller).
 * The exploring creature is [target].
 *
 * @property target The creature that is exploring.
 */
@SerialName("Explore")
@Serializable
data class ExploreEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "${target.description} explores"
}

