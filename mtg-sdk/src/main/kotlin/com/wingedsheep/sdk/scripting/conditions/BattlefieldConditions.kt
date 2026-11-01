package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.core.Zone
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
 * Condition: "if you cast it" referring to the *triggering* entity (not the ability's source).
 * True when the entering permanent that triggered the ability was cast — i.e. it carries a
 * cast-origin marker — and false when it was put onto the battlefield by another effect (token,
 * reanimation, "put onto the battlefield"). This is the sibling of
 * [com.wingedsheep.sdk.scripting.conditions.WasCast] for "whenever a creature you control enters,
 * if you cast it" triggers, where the source is a separate permanent and the cast subject is the
 * entering creature (e.g. The Sibsig Ceremony).
 *
 * [zone] narrows it to a cast from one zone — `Zone.HAND` reads "if you cast it from your hand",
 * the triggering-entity sibling of [com.wingedsheep.sdk.scripting.conditions.WasCastFromHand].
 * Negated, that is Chainer, Nightmare Adept's "if you didn't cast it from your hand", which holds
 * for a creature cast from another zone *and* for one that wasn't cast at all. Null (the default)
 * accepts a cast from any zone. The engine records a cast origin for hand, graveyard, library and
 * exile; any other zone never matches.
 */
@SerialName("TriggeringEntityWasCast")
@Serializable
data class TriggeringEntityWasCast(val zone: Zone? = null) : Condition {
    override val description: String = when (zone) {
        null -> "if you cast it"
        Zone.HAND -> "if you cast it from your hand"
        else -> "if you cast it from ${zone.displayName}"
    }
}

/**
 * Condition: "if no mana was spent to cast it" — about the TRIGGERING spell (the triggering-entity
 * counterpart of [NoManaSpentToCast], which reads the ability source's own cast record). True when
 * the triggering entity's `CastRecordComponent` shows zero total mana spent (or is absent). Used by
 * Boromir, Warden of the Tower: "Whenever an opponent casts a spell, if no mana was spent to cast it,
 * counter that spell."
 */
@SerialName("TriggeringSpellCastWithoutPayingMana")
@Serializable
data object TriggeringSpellCastWithoutPayingMana : Condition {
    override val description: String = "if no mana was spent to cast it"
}

/**
 * Condition: "if at least [amount] mana was spent to cast it" — about the TRIGGERING spell.
 * Reads the total mana paid recorded on the triggering spell's `SpellOnStackComponent`
 * (the same source [TriggeringSpellCastWithoutPayingMana] reads) and is true when that total
 * is `>= amount`. The threshold counterpart of [TriggeringSpellCastWithoutPayingMana]; the
 * intervening-"if" clause for cast-payoffs like Sahagin ("Whenever you cast a noncreature
 * spell, if at least four mana was spent to cast it, …"). False when the triggering entity
 * isn't a spell on the stack.
 */
@SerialName("TriggeringSpellManaSpentAtLeast")
@Serializable
data class TriggeringSpellManaSpentAtLeast(val amount: Int) : Condition {
    override val description: String = "if at least $amount mana was spent to cast it"
}

/**
 * Condition: "if it entered from [zone] or was cast from [zone]" about the triggering entity.
 * True when the triggering permanent was put onto the battlefield directly from [zone] (a
 * reanimation, a blink, a library search — the engine's entered-from marker) or resolved from a
 * spell cast from [zone] (its cast-origin marker). [zone] is any of [PROVENANCE_ZONES].
 *
 * The single-entity twin of [AnyEnteredOrWasCastFrom], which reads the same markers over a
 * batch-enters capture.
 * - **Twilight Diviner**, **Archfiend's Vessel** — `Zone.GRAVEYARD`.
 * - **Fblthp, the Lost** — `Zone.LIBRARY` ("if it entered from your library or was cast from your
 *   library", with the "your" carried by a separate owned-by-you check).
 */
@SerialName("TriggeringEntityEnteredOrWasCastFrom")
@Serializable
data class TriggeringEntityEnteredOrWasCastFrom(val zone: Zone) : Condition {
    init {
        require(zone in PROVENANCE_ZONES) {
            "TriggeringEntityEnteredOrWasCastFrom tracks ${PROVENANCE_ZONES.joinToString()} provenance, not $zone"
        }
    }

    override val description: String =
        "if it entered or was cast from ${provenanceZoneText(zone)}"
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
 * Condition: "if it had counters on it" (intervening-if for dies/leaves triggers).
 * Reads the last-known total counter count (across every counter kind) captured on the
 * dying/leaving entity at the moment it left the battlefield (Rule 603.10 / 603.6c). True
 * when that entity had at least one counter of any kind. Used by cards like Host of the
 * Hereafter whose dies trigger only fires when the creature died with counters on it.
 */
@SerialName("TriggeringEntityHadCounters")
@Serializable
data object TriggeringEntityHadCounters : Condition {
    override val description: String = "if it had counters on it"
}

/**
 * Condition: "if it was [filter]" — an intervening-if for dies/leaves triggers that matches the
 * triggering permanent's **last-known** type line (CR 603.10, 608.2h): the projected card types and
 * subtypes captured at the moment it left the battlefield, so types set or granted by continuous
 * effects count, not just the printed ones.
 *
 * Used by self-recursion loop guards whose returning permanent is a different kind of object:
 * - Infernal Vessel — `Not(TriggeringEntityWas(Any.withSubtype(DEMON)))`: "if it wasn't a Demon,
 *   return it … It's a Demon in addition to its other types".
 * - Tom, Bert, and William — `TriggeringEntityWas(Creature)`: "if they were a creature, return
 *   them … They're an artifact", so the second death is of an artifact and the loop stops.
 *
 * A card in a graveyard has its printed type line back, so asking the live entity would answer
 * the same thing on both deaths — only the leave-time snapshot tells them apart. That snapshot
 * holds card types and subtypes only, so [filter] may use just those card predicates (and
 * `And`/`Or`/`Not` over them); anything else is rejected at construction. A trigger whose source
 * never left the battlefield has no last-known type line, and the condition is false.
 */
@SerialName("TriggeringEntityWas")
@Serializable
data class TriggeringEntityWas(val filter: GameObjectFilter) : Condition {
    init {
        requireTypeLineOnly(filter)
    }

    override val description: String = "if it was ${filter.indefiniteArticle} ${filter.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }

    private companion object {
        /**
         * Rejects anything the last-known type line can't answer, in [filter] and in every `anyOf`
         * alternative at any depth — the snapshot matcher reports such a predicate unknown, and an
         * unknown under `Not` must not be read as "wasn't".
         */
        fun requireTypeLineOnly(filter: GameObjectFilter) {
            require(filter.statePredicates.isEmpty() && filter.controllerPredicate == null) {
                "TriggeringEntityWas reads only the last-known type line; state and controller predicates aren't captured"
            }
            val unsupported = filter.cardPredicates.flatMap { unsupportedPredicates(it) }
            require(unsupported.isEmpty()) {
                "TriggeringEntityWas reads only card types and subtypes, not $unsupported"
            }
            filter.anyOf.forEach { requireTypeLineOnly(it) }
        }

        /** The card predicates a last-known type line (card types + subtypes) can answer. */
        fun unsupportedPredicates(predicate: CardPredicate): List<CardPredicate> = when (predicate) {
            CardPredicate.IsCreature, CardPredicate.IsLand, CardPredicate.IsArtifact,
            CardPredicate.IsEnchantment, CardPredicate.IsInstant, CardPredicate.IsSorcery,
            CardPredicate.IsPlaneswalker, CardPredicate.IsBattle, CardPredicate.IsKindred, CardPredicate.IsPermanent,
            is CardPredicate.HasSubtype -> emptyList()
            is CardPredicate.Not -> unsupportedPredicates(predicate.predicate)
            is CardPredicate.And -> predicate.predicates.flatMap { unsupportedPredicates(it) }
            is CardPredicate.Or -> predicate.predicates.flatMap { unsupportedPredicates(it) }
            else -> listOf(predicate)
        }
    }
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
 * Condition: "if [target] is a player".
 *
 * True when the context target at [targetIndex] is a player (rather than a permanent, spell, or
 * card). The companion `EntityMatches(ContextTarget(targetIndex), filter)` only matches game
 * objects and returns false for a player target, so this is the dedicated check for "any target"
 * effects whose follow-up applies only when the chosen target was a player — e.g. Sonic Shrieker's
 * "If a player is dealt damage this way, they discard a card." Pair with
 * [com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget] to make that same player the
 * subject of the follow-up.
 */
@SerialName("TargetIsPlayer")
@Serializable
data class TargetIsPlayer(
    val targetIndex: Int = 0
) : Condition {
    override val description: String = "if a player is dealt damage this way"
}

/**
 * Condition: "if [target] is tapped".
 *
 * Resolves the context target at [targetIndex] to a battlefield permanent and returns true when it
 * is currently tapped. Non-permanent targets and permanents that have left the battlefield return
 * false defensively. Pairs with a [com.wingedsheep.sdk.dsl.Effects.If] to branch
 * on a target's tapped state at resolution time — e.g. Shackle Slinger's "choose target creature an
 * opponent controls. If it's tapped, put a stun counter on it. Otherwise, tap it."
 */
@SerialName("TargetIsTapped")
@Serializable
data class TargetIsTapped(
    val targetIndex: Int = 0
) : Condition {
    override val description: String = "if it's tapped"
}

/**
 * Condition: "if [target] is this permanent (the source)".
 *
 * True when the context target at [targetIndex] resolves to the same entity as the ability's
 * source. Wrap in [com.wingedsheep.sdk.dsl.Conditions.Not] for "if it's a different permanent" /
 * "another" wordings — e.g. Arid Archway's "If another Desert was returned this way" pairs
 * `Not(TargetIsSource())` with a Desert-land filter check so returning the Archway itself doesn't
 * count. Non-permanent targets return false.
 */
@SerialName("TargetIsSource")
@Serializable
data class TargetIsSource(
    val targetIndex: Int = 0
) : Condition {
    override val description: String = "if it's this permanent"
}

/**
 * Condition: "if excess damage was dealt this way" — true when the target creature's
 * marked damage now strictly exceeds its (projected) toughness.
 *
 * Reads post-damage state, so chain it AFTER a [com.wingedsheep.sdk.scripting.effects.DealDamageEffect]
 * with `DealDamage(N, t) then Effects.If(IfTargetTookExcessDamage(0), ...)`.
 *
 * What this actually checks is `marked > lethal threshold` (toughness, or power under a
 * lethal-damage-by-power rule) on the target at evaluation time,
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
 * In the canonical Orbital Plunge chain these can't fire — `target(TargetFilter.Creature)` rules out
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
