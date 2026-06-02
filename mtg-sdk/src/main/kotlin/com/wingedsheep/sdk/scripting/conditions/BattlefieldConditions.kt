package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Battlefield Conditions (non-generic — require special evaluation logic)
// =============================================================================

/**
 * Condition: "If a player controls more [subtype] creatures than each other player"
 * Used by Thoughtbound Primoc and similar Onslaught "tribal war" cards.
 * Returns true only if exactly one player has strictly more than all others.
 */
@SerialName("APlayerControlsMostOfSubtype")
@Serializable
data class APlayerControlsMostOfSubtype(val subtype: Subtype) : Condition {
    override val description: String = "if a player controls more ${subtype.value}s than each other player"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val new = replacer.replaceSubtype(subtype)
        return if (new == subtype) this else APlayerControlsMostOfSubtype(new)
    }
}

/**
 * Condition: "If you control more creatures of the chosen type than each other player"
 * Used by Peer Pressure-style effects where a creature type is chosen via
 * ChooseOptionEffect and stored in EffectContext.chosenValues[chosenValueKey].
 * Returns true if the controller has strictly more creatures of that type than
 * each other player.
 */
@SerialName("YouControlMostOfChosenType")
@Serializable
data class YouControlMostOfChosenType(val chosenValueKey: String = "chosenCreatureType") : Condition {
    override val description: String = "if you control more creatures of the chosen type than each other player"
}

/**
 * Condition: "If enchanted creature is a [subtype]"
 * Used by auras like Lavamancer's Skill that have different effects based on
 * the creature type of the enchanted creature.
 */
@SerialName("EnchantedCreatureHasSubtype")
@Serializable
data class EnchantedCreatureHasSubtype(val subtype: Subtype) : Condition {
    override val description: String = "if enchanted creature is a ${subtype.value}"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val new = replacer.replaceSubtype(subtype)
        return if (new == subtype) this else EnchantedCreatureHasSubtype(new)
    }
}

/**
 * Condition: "If enchanted creature is legendary"
 * Used by auras whose continuous effects apply only while the enchanted creature
 * has the legendary supertype.
 */
@SerialName("EnchantedCreatureIsLegendary")
@Serializable
data object EnchantedCreatureIsLegendary : Condition {
    override val description: String = "if enchanted creature is legendary"
}

/**
 * Condition: "if enchanted permanent matches [filter]".
 *
 * Resolves the source Aura's `AttachedToComponent` and checks the attached permanent
 * against a [GameObjectFilter] in projected state. The general-purpose counterpart to
 * [EnchantedCreatureHasSubtype] / [EnchantedCreatureIsLegendary]: works for color, type,
 * or any other filterable property. Used by Essence Leak ("as long as enchanted permanent
 * is red or green"). The attachment may be any permanent, not just a creature.
 */
@SerialName("EnchantedPermanentMatches")
@Serializable
data class EnchantedPermanentMatches(val filter: GameObjectFilter) : Condition {
    override val description: String = "if enchanted permanent is ${filter.description}"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Condition: "if it was historic" (legendary, artifact, or Saga).
 * Checks the triggering entity's card definition for the historic quality.
 * Used for Curator's Ward's "if it was historic" intervening-if condition.
 */
@SerialName("TriggeringEntityWasHistoric")
@Serializable
data object TriggeringEntityWasHistoric : Condition {
    override val description: String = "if it was historic"
}

/**
 * Condition: "if it entered or was cast from a graveyard".
 * True when the triggering entity has either EnteredFromGraveyardComponent (reanimated
 * directly from graveyard → battlefield) or CastFromGraveyardComponent (spell was cast
 * from graveyard). Used by Twilight Diviner.
 */
@SerialName("TriggeringEntityEnteredOrWasCastFromGraveyard")
@Serializable
data object TriggeringEntityEnteredOrWasCastFromGraveyard : Condition {
    override val description: String = "if it entered or was cast from a graveyard"
}

/**
 * Condition: "if it wasn't put onto the battlefield with this ability".
 * True when the triggering entity does NOT carry an `EnteredViaAbilityComponent`
 * pointing to this trigger's source permanent. Used to break ETB-trigger loops on
 * cards like Kodama of the East Tree, where the trigger's own pay-off (putting a
 * card from hand onto the battlefield) would otherwise re-fire the trigger.
 */
@SerialName("TriggeringEntityWasNotPutByThisSource")
@Serializable
data object TriggeringEntityWasNotPutByThisSource : Condition {
    override val description: String = "if it wasn't put onto the battlefield with this ability"
}

/**
 * Condition: "if it had a -1/-1 counter on it" (intervening-if for dies/leaves triggers).
 * Reads the last-known -1/-1 counter count captured on the dying entity at the moment
 * it left the battlefield (Rule 603.10 / 603.6c). Used by cards like Retched Wretch
 * whose dies trigger only fires when the creature died with -1/-1 counters on it.
 */
@SerialName("TriggeringEntityHadMinusOneMinusOneCounter")
@Serializable
data object TriggeringEntityHadMinusOneMinusOneCounter : Condition {
    override val description: String = "if it had a -1/-1 counter on it"
}

/**
 * Condition: "with a single target" — true iff the triggering spell or ability
 * has exactly one target chosen. Reads the triggering entity's TargetsComponent.
 * Used by cards like Spinerock Tyrant whose trigger fires only when you cast an
 * instant or sorcery spell with a single target.
 */
@SerialName("TriggeringSpellHasSingleTarget")
@Serializable
data object TriggeringSpellHasSingleTarget : Condition {
    override val description: String = "with a single target"
}

/**
 * Condition: "if it's a/an [filter] spell" — true iff the spell that triggered this ability
 * matches [filter]. Reads the triggering entity (the spell of a "whenever you cast a spell"
 * trigger) by its static card characteristics, so it stays correct even after the spell has
 * left the stack. Resolution-only.
 *
 * General intervening-if guard for "whenever you cast a spell, if it's a/an X ..." cards. Compose
 * with [PlayerCastSpellsThisTurn] for "first X spell this turn" payoffs — see
 * `Conditions.YouCastFirstSpellOfTypeThisTurn`, which ANDs this with
 * `Not(PlayerCastSpellsThisTurn(filter, atLeast = 2))` so the count machinery isn't duplicated.
 */
@SerialName("TriggeringSpellMatchesFilter")
@Serializable
data class TriggeringSpellMatchesFilter(
    val filter: GameObjectFilter
) : Condition {
    override val description: String = "if it's ${filter.description.ifEmpty { "a matching" }} spell"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Condition: "if [target] is [filter]"
 * Checks whether a context target matches a GameObjectFilter.
 * Used for cards like Blessing of Belzenlok: "If it's legendary, it also gains lifelink."
 */
@SerialName("TargetMatchesFilter")
@Serializable
data class TargetMatchesFilter(
    val filter: GameObjectFilter,
    val targetIndex: Int = 0
) : Condition {
    override val description: String = "if target matches $filter"
}

/**
 * Condition: "if [target] is a player".
 *
 * True when the context target at [targetIndex] is a player (rather than a permanent, spell, or
 * card). The companion [TargetMatchesFilter] only matches game objects and returns false for a
 * player target, so this is the dedicated check for "any target" effects whose follow-up applies
 * only when the chosen target was a player — e.g. Sonic Shrieker's "If a player is dealt damage
 * this way, they discard a card." Pair with [com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget]
 * to make that same player the subject of the follow-up.
 */
@SerialName("TargetIsPlayer")
@Serializable
data class TargetIsPlayer(
    val targetIndex: Int = 0
) : Condition {
    override val description: String = "if a player is dealt damage this way"
}

/**
 * Condition: "if excess damage was dealt this way" — true when the target creature's
 * marked damage now strictly exceeds its (projected) toughness.
 *
 * Reads post-damage state, so chain it AFTER a [com.wingedsheep.sdk.scripting.effects.DealDamageEffect]
 * with `Effects.Composite(DealDamage(N, t), ConditionalEffect(IfTargetTookExcessDamage(0), ...))`.
 *
 * What this actually checks is `marked > toughness` on the target at evaluation time,
 * regardless of which preceding step in the chain dealt the damage. CompositeEffect
 * resolves sub-effects sequentially without an interleaved SBA pass or mid-chain trigger
 * processing, so for the canonical "deal N to a target, then check" pipeline this is
 * equivalent to "did the preceding deal-damage step push the target past lethal" — there
 * is no other source of marked damage in scope. Don't reuse this in a chain that deals
 * damage in multiple steps within the same composite, or that runs past SBA somehow; you'd
 * see cumulative marked damage instead, and the condition would lie about which step caused
 * the excess.
 *
 * Defensive guards (target is not a creature, target left the battlefield) return false.
 * In the canonical Orbital Plunge chain these can't fire — `Targets.Creature` rules out
 * non-creature targets, and Composite never reaches SBA between steps so the target is
 * still on the battlefield. They exist for future callers that might wrap this in a longer
 * chain crossing SBA or re-target between steps.
 *
 * Used by Orbital Plunge: "If excess damage was dealt this way, create a Lander token."
 */
@SerialName("TargetMarkedDamageExceedsToughness")
@Serializable
data class TargetMarkedDamageExceedsToughness(
    val targetIndex: Int = 0
) : Condition {
    override val description: String = "if excess damage was dealt this way"
}

/**
 * Condition: "if another permanent with the same name as [target] is on the battlefield".
 *
 * Resolves the context target at [targetIndex], reads its card name, and returns true when at
 * least one *other* battlefield permanent shares that exact name. The target permanent itself is
 * excluded from the comparison (so a single copy never satisfies its own check). Tokens compare by
 * name like any other permanent.
 *
 * Used by Winnow: "Destroy target nonland permanent if another permanent with the same name is on
 * the battlefield."
 */
@SerialName("AnotherPermanentWithSameNameAsTarget")
@Serializable
data class AnotherPermanentWithSameNameAsTarget(
    val targetIndex: Int = 0
) : Condition {
    override val description: String = "if another permanent with the same name is on the battlefield"
}

/**
 * Condition: "if [target] shares a color with the most common color among all permanents
 * or a color tied for most common".
 *
 * Tallies, across every permanent on the battlefield, how many share each of the five
 * colors (a multicolored permanent contributes to each of its colors). The most common
 * color(s) are those with the highest tally; ties all count. The condition is true when
 * the targeted permanent has at least one color in that most-common set. A board with no
 * colored permanents has no most-common color, so the condition is false.
 *
 * Used by Tsabo's Assassin.
 */
@SerialName("TargetSharesMostCommonColor")
@Serializable
data class TargetSharesMostCommonColor(
    val targetIndex: Int = 0
) : Condition {
    override val description: String =
        "if it shares a color with the most common color among all permanents or a color tied for most common"
}

/**
 * Condition: "As long as [color] is the most common color among all permanents, or is tied
 * for most common".
 *
 * Tallies, across every permanent on the battlefield, how many share each of the five colors
 * (a multicolored permanent contributes to each of its colors). The most common color(s) are
 * those with the highest tally; ties all count. The condition is true when [color] is in that
 * most-common set. A board with no colored permanents has no most-common color, so the
 * condition is false.
 *
 * Board-derived only (no targets / triggering entity / kicker state), so it evaluates
 * identically in resolution and in projection — which is required, since the Invasion djinn
 * cycle (Goham/Halam/Ruham/Sulam/Zanam) uses it as a `ConditionalStaticAbility` gate.
 */
@SerialName("ColorIsMostCommon")
@Serializable
data class ColorIsMostCommon(val color: Color) : Condition {
    override val description: String =
        "as long as ${color.displayName.lowercase()} is the most common color among all permanents, or is tied for most common"
}
