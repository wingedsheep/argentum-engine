package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.selfNounToken
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Keyword and Ability Grant Effects
// =============================================================================

/**
 * Grant a keyword or ability flag to a target until end of turn.
 * "Target creature gains flying until end of turn."
 *
 * The [keyword] field stores the enum name (e.g., "FLYING", "DOESNT_UNTAP")
 * which the engine uses for string-based keyword checks in projected state.
 *
 * @property condition When non-null, the granted keyword is only *live* while this condition
 *   holds. This is not a gate on whether the grant happens — the grant always happens and lasts
 *   for [duration]; the condition rides along on the resulting continuous effect and is
 *   re-evaluated on every projection, exactly like the condition of a printed
 *   [com.wingedsheep.sdk.scripting.ConditionalStaticAbility]. It is the durational sibling of a
 *   printed "as long as …, this creature has …" clause, and the way to model a quoted conditional
 *   ability handed out by an animate effect: Restless Spire's "{U}{R}: … becomes a 2/1 … creature
 *   with 'During your turn, this creature has first strike.'" composes
 *   `BecomeCreature(...)` with `GrantKeyword(FIRST_STRIKE, Self, EndOfTurn, Conditions.IsYourTurn)`.
 *   "You" in the condition is the *source's* current controller under projection, so the clause
 *   correctly stops applying if another player gains control of the permanent mid-turn. Unlike the
 *   `Duration.While…` family this never latches off: the keyword comes back if the condition
 *   becomes true again within [duration].
 */
@SerialName("GrantKeyword")
@Serializable
data class GrantKeywordEffect(
    val keyword: String,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn,
    val condition: com.wingedsheep.sdk.scripting.conditions.Condition? = null
) : Effect {
    constructor(
        keyword: Keyword,
        target: EffectTarget,
        duration: Duration = Duration.EndOfTurn,
        condition: com.wingedsheep.sdk.scripting.conditions.Condition? = null
    ) : this(keyword.name, target, duration, condition)

    override val description: String = buildString {
        if (condition != null) append("${condition.description.replaceFirstChar { it.uppercase() }}, ")
        append("${target.description} gains ${keyword.lowercase().replace('_', ' ')}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Remove a keyword or ability flag from a target until end of turn.
 * "All other creatures lose flying until end of turn."
 *
 * The [keyword] field stores the enum name (e.g., "FLYING")
 * which the engine uses for string-based keyword checks in projected state.
 */
@SerialName("RemoveKeyword")
@Serializable
data class RemoveKeywordEffect(
    val keyword: String,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    constructor(keyword: Keyword, target: EffectTarget, duration: Duration = Duration.EndOfTurn) :
        this(keyword.name, target, duration)

    override val description: String = buildString {
        append("${target.description} loses ${keyword.lowercase().replace('_', ' ')}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Remove all abilities from a target creature until the specified duration.
 * "Target creature loses all abilities until end of turn."
 *
 * @property target The creature that loses all abilities
 * @property duration How long the effect lasts
 */
@SerialName("RemoveAllAbilities")
@Serializable
data class RemoveAllAbilitiesEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} loses all abilities")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Grant a triggered ability to a target until end of turn.
 * "Target creature gains 'When this creature deals combat damage to a player, ...'"
 *
 * @property ability The triggered ability to grant
 * @property target The creature to grant the ability to
 * @property duration How long the grant lasts
 */
@SerialName("GrantTriggeredAbility")
@Serializable
data class GrantTriggeredAbilityEffect(
    val ability: TriggeredAbility,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect, SelfReferentialDescription {
    override val descriptionTemplate: String = buildString {
        append("${target.selfNounToken} gains \"${ability.description}\"")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
    override val description: String get() = defaultResolvedDescription

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newAbility !== ability) copy(ability = newAbility) else this
    }
}

/**
 * Grant an activated ability to a target until end of turn.
 * "Target creature gains '{cost}: {effect}' until end of turn"
 *
 * Used for cards like Run Wild that grant activated abilities temporarily.
 *
 * @property ability The activated ability to grant
 * @property target The creature to grant the ability to
 * @property duration How long the grant lasts
 */
@SerialName("GrantActivatedAbility")
@Serializable
data class GrantActivatedAbilityEffect(
    val ability: ActivatedAbility,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect, SelfReferentialDescription {
    override val descriptionTemplate: String = buildString {
        append("${target.selfNounToken} gains \"${ability.description}\"")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
    override val description: String get() = defaultResolvedDescription

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newAbility !== ability) copy(ability = newAbility) else this
    }
}

/**
 * The permanent [target] names **gains all activated abilities of the object [donor] names**, for
 * [duration].
 *
 * The one-shot, resolution-time sibling of
 * [com.wingedsheep.sdk.scripting.GainActivatedAbilitiesOfPermanents] (a static that re-reads its
 * donor set every projection) and of [GrantActivatedAbilityEffect] (which grants one *authored*
 * ability). Here the donor is picked at resolution — usually a target — so the abilities can't be
 * written into the card:
 *  - Quicksilver Elemental: `{U}: This creature gains all activated abilities of target creature
 *    until end of turn.` → `GainAllActivatedAbilitiesOfEffect(donor = ContextTarget(0))`.
 *  - Grell Philosopher / Havengul Lich are the same shape with a different donor and receiver.
 *
 * **The set of abilities is snapshotted when this resolves**, per the Havengul Lich ruling
 * ("gains the activated abilities of the card *as it existed in the graveyard*"): the donor
 * changing, leaving the battlefield, or gaining abilities afterwards does not change what the
 * receiver has. That is the difference from the static sibling, and the reason this is an effect
 * rather than a `GrantStaticAbility` carrying one.
 *
 * Each gained ability is granted with the **receiver** as its source (CR 113.7), so `{T}`,
 * `SacrificeSelf` and "this creature" inside a copied ability bind to the permanent that gained it
 * — the printed reminder "(If any of the abilities use that creature's name, use this creature's
 * name instead.)".
 *
 * It grants only *activated* abilities — never triggered, static, or keyword abilities (unless the
 * keyword is itself modelled as an activated ability), and only those activatable from the
 * battlefield. Mana abilities **are** included: the printed wording is "all activated abilities",
 * with no "except mana abilities" clause.
 *
 * @property donor The object whose activated abilities are copied.
 * @property target The permanent that gains them (defaults to the source itself).
 * @property duration How long the gain lasts.
 */
@SerialName("GainAllActivatedAbilitiesOf")
@Serializable
data class GainAllActivatedAbilitiesOfEffect(
    val donor: EffectTarget,
    val target: EffectTarget = EffectTarget.Self,
    val duration: Duration = Duration.EndOfTurn
) : Effect, SelfReferentialDescription {
    override val descriptionTemplate: String = buildString {
        append("${target.selfNounToken} gains all activated abilities of ${donor.description}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
    override val description: String get() = defaultResolvedDescription
}

/**
 * Grant Harmonize (CR 702.180) to a target instant or sorcery card in a graveyard.
 * "Target instant or sorcery card in your graveyard gains harmonize until end of turn. Its
 * harmonize cost is equal to its mana cost." — Songcrafter Mage.
 *
 * Unlike the other ability-grant effects (which target battlefield creatures), this grants a
 * *graveyard-cast* keyword ability that the cast-from-graveyard enumerator and the
 * alternative-payment handler consult through the runtime granted-keyword record. The grant is
 * keyed to the card entity, so it survives the graveyard → stack move and drives the
 * exile-on-resolution clause for the spell cast this way.
 *
 * @property target The instant or sorcery card (in a graveyard) gaining harmonize
 * @property cost The harmonize cost. `null` (the default) means "equal to the card's mana cost"
 *   per Songcrafter Mage; a non-null value grants a fixed harmonize cost for any future card.
 * @property duration How long the grant lasts (until end of turn for Songcrafter Mage)
 */
@SerialName("GrantHarmonize")
@Serializable
data class GrantHarmonizeEffect(
    val target: EffectTarget,
    val cost: ManaCost? = null,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains harmonize")
        append(if (cost != null) " $cost" else " (its harmonize cost is equal to its mana cost)")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Grant Flashback (CR 702.34) to a target instant or sorcery card in a graveyard.
 * "Target instant or sorcery card in your graveyard gains flashback until end of turn. The
 * flashback cost is equal to its mana cost." — Archmage's Newt.
 *
 * The runtime sibling of printed [com.wingedsheep.sdk.scripting.KeywordAbility.Flashback], modelled
 * exactly like [GrantHarmonizeEffect]: it records a granted graveyard-cast keyword ability keyed to
 * the card entity, which the cast-from-graveyard enumerator, the cast handler, and the stack
 * resolver's exile-on-resolution clause consult through the runtime granted-keyword record. The
 * grant survives the graveyard → stack move so a spell cast this way still exiles on resolution.
 *
 * @property target The instant or sorcery card (in a graveyard) gaining flashback
 * @property cost The flashback cost. `null` (the default) means "equal to the card's mana cost"
 *   per Archmage's Newt; a non-null value grants a fixed flashback cost (e.g. `{0}` when the
 *   granting Mount is saddled).
 * @property duration How long the grant lasts (until end of turn for Archmage's Newt)
 */
@SerialName("GrantFlashback")
@Serializable
data class GrantFlashbackEffect(
    val target: EffectTarget,
    val cost: ManaCost? = null,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains flashback")
        append(if (cost != null) " $cost" else " (the flashback cost is equal to its mana cost)")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Grant Embalm (CR 702.128) to a target creature card in a graveyard.
 * "Target creature card in your graveyard gains embalm until end of turn. The embalm cost is equal
 * to its mana cost." — Cursecloth Wrappings.
 *
 * The runtime sibling of printed embalm ([com.wingedsheep.sdk.dsl.embalm]). Embalm is an ordinary
 * graveyard-*activated* ability, not an alternative way to cast, so unlike [GrantHarmonizeEffect] /
 * [GrantFlashbackEffect] this records a plain granted **activated** ability keyed to the card
 * entity — the same object [com.wingedsheep.sdk.dsl.embalmAbility] builds for a printed embalm
 * card — which the engine's zone-activated-ability enumerator surfaces while the card sits in the
 * graveyard. Nothing about the cast pipeline is involved.
 *
 * @property target The creature card (in a graveyard) gaining embalm
 * @property cost The embalm cost. `null` (the default) means "equal to the card's mana cost" per
 *   Cursecloth Wrappings; a non-null value grants a fixed embalm cost for any future card.
 * @property duration How long the grant lasts (until end of turn for Cursecloth Wrappings)
 */
@SerialName("GrantEmbalm")
@Serializable
data class GrantEmbalmEffect(
    val target: EffectTarget,
    val cost: ManaCost? = null,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains embalm")
        append(if (cost != null) " $cost" else " (the embalm cost is equal to its mana cost)")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Grant a static ability to a target until end of turn.
 * "Target creature gains 'This creature can't be blocked by more than one creature.'"
 *
 * The runtime sibling of a printed [com.wingedsheep.sdk.scripting.StaticAbility]. Unlike
 * keyword grants (which flow through projected keywords) and triggered/activated grants
 * (consulted by the trigger/ability resolvers), a granted static ability is recorded keyed
 * to the entity and read at the point of use — e.g. the combat blocker validation consults
 * granted [com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan] alongside the creature's
 * printed static abilities. Compose inside [com.wingedsheep.sdk.dsl.Effects.ForEachInGroup]
 * with [EffectTarget.Self] to grant it to each creature in a group (Full Steam Ahead).
 *
 * @property ability The static ability to grant
 * @property target The permanent to grant the ability to
 * @property duration How long the grant lasts
 */
@SerialName("GrantStaticAbility")
@Serializable
data class GrantStaticAbilityEffect(
    val ability: com.wingedsheep.sdk.scripting.StaticAbility,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect, SelfReferentialDescription {
    override val descriptionTemplate: String = buildString {
        append("${target.selfNounToken} gains \"${ability.description}\"")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
    override val description: String get() = defaultResolvedDescription

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newAbility !== ability) copy(ability = newAbility) else this
    }
}

/**
 * Grant a replacement effect to a target for a duration — the runtime sibling of a printed
 * [com.wingedsheep.sdk.scripting.ReplacementEffect]. Mirrors [GrantStaticAbilityEffect]: the
 * grant is recorded keyed to the entity ([com.wingedsheep.engine.state.GameState.grantedReplacementEffects])
 * and read at the point of use rather than projected through the layer system.
 *
 * Used for "this turn" replacement riders created by a resolving ability — e.g. Forgotten Cellar
 * ("if a card would be put into your graveyard from anywhere this turn, exile it instead"),
 * which grants a [com.wingedsheep.sdk.scripting.RedirectZoneChange] to the room for end of turn.
 * The zone-change redirect read site consults these alongside permanents' printed replacement
 * effects, so any card needing a durational redirect-to-exile/graveyard rider reuses this
 * instead of a one-off effect.
 *
 * @property replacement The replacement effect to grant
 * @property target The permanent the grant is anchored to (its controller owns the grant)
 * @property duration How long the grant lasts
 */
@SerialName("GrantReplacementEffect")
@Serializable
data class GrantReplacementEffectEffect(
    val replacement: com.wingedsheep.sdk.scripting.ReplacementEffect,
    val target: EffectTarget = EffectTarget.Self,
    val duration: Duration = Duration.EndOfTurn
) : Effect, SelfReferentialDescription {
    override val descriptionTemplate: String = buildString {
        append("${target.selfNounToken} gains \"${replacement.description}\"")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
    override val description: String get() = defaultResolvedDescription

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newReplacement = replacement.applyTextReplacement(replacer)
        return if (newReplacement !== replacement) copy(replacement = newReplacement) else this
    }
}

/**
 * Grant an activated ability to a group of creatures.
 * "Each creature you control gains '{B}: This creature gets +1/+1 until end of turn.'"
 *
 * Adds GrantedActivatedAbility entries for each matching creature.
 *
 * @property ability The activated ability to grant
 * @property filter Which creatures are affected
 * @property duration How long the grant lasts
 */
@SerialName("GrantActivatedAbilityToGroup")
@Serializable
data class GrantActivatedAbilityToGroupEffect(
    val ability: ActivatedAbility,
    val filter: GroupFilter = GroupFilter.AllCreaturesYouControl,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${filter.description} gain \"${ability.description}\"")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAbility !== ability)
            copy(filter = newFilter, ability = newAbility) else this
    }
}

/**
 * Grant stats and/or a keyword to the enchanted creature and all other creatures
 * that share a creature type with it.
 *
 * Used by the Crown cycle from Onslaught (Crown of Fury, Crown of Vigor, etc.)
 * which sacrifice themselves to buff creatures sharing a type with the enchanted creature.
 *
 * At resolution time, the executor:
 * 1. Finds the enchanted creature via AttachedToComponent on the source (aura)
 * 2. Gets the enchanted creature's creature subtypes
 * 3. Applies the effect to all creatures sharing at least one subtype
 *
 * @property powerModifier Power bonus (can be negative)
 * @property toughnessModifier Toughness bonus (can be negative)
 * @property keyword Optional keyword to grant
 * @property protectionColors Optional set of colors to grant protection from
 * @property duration How long the effect lasts
 */
@SerialName("GrantToEnchantedCreatureTypeGroup")
@Serializable
data class GrantToEnchantedCreatureTypeGroupEffect(
    val powerModifier: Int = 0,
    val toughnessModifier: Int = 0,
    val keyword: Keyword? = null,
    val protectionColors: Set<Color> = emptySet(),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("Enchanted creature and other creatures that share a creature type with it")
        if (powerModifier != 0 || toughnessModifier != 0) {
            val powerStr = if (powerModifier >= 0) "+$powerModifier" else "$powerModifier"
            val toughStr = if (toughnessModifier >= 0) "+$toughnessModifier" else "$toughnessModifier"
            append(" get $powerStr/$toughStr")
        }
        if (keyword != null) {
            if (powerModifier != 0 || toughnessModifier != 0) append(" and")
            append(" gain ${keyword.displayName.lowercase()}")
        }
        if (protectionColors.isNotEmpty()) {
            if (powerModifier != 0 || toughnessModifier != 0 || keyword != null) append(" and")
            append(" gain protection from ")
            append(protectionColors.joinToString(" and from ") { it.displayName.lowercase() })
        }
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}
