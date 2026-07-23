package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed interface for replacement effects.
 *
 * Replacement effects intercept game events BEFORE they happen and
 * modify or replace them entirely. Unlike triggered abilities, replacement
 * effects do not use the stack.
 *
 * The system is compositional - replacement effects are specified by combining
 * a EventPattern filter with a modification/replacement behavior.
 *
 * Examples:
 * ```kotlin
 * // Doubling Season (tokens) — factor defaults to 2; Ojer Taq passes factor = 3
 * MultiplyTokenCreation(
 *     appliesTo = EventPattern.TokenCreationEvent(controller = ControllerFilter.You)
 * )
 *
 * // Hardened Scales
 * ModifyCounterPlacement(
 *     modifier = 1,
 *     appliesTo = EventPattern.CounterPlacementEvent(
 *         counterType = CounterTypeFilter.PlusOnePlusOne,
 *         recipient = RecipientFilter.CreatureYouControl
 *     )
 * )
 *
 * // Rest in Peace
 * RedirectZoneChange(
 *     newDestination = Zone.Exile,
 *     appliesTo = EventPattern.ZoneChangeEvent(to = Zone.Graveyard)
 * )
 *
 * // Prevention shield (combat damage from red sources)
 * PreventDamage(
 *     appliesTo = EventPattern.DamageEvent(
 *         recipient = RecipientFilter.You,
 *         source = SourceFilter.HasColor(Color.RED),
 *         damageType = DamageType.Combat
 *     )
 * )
 * ```
 */
@Serializable
sealed interface ReplacementEffect : TextReplaceable<ReplacementEffect> {
    /** Human-readable description of the replacement effect */
    val description: String

    /** What type of event this replacement intercepts (compositional) */
    val appliesTo: EventPattern
}

// =============================================================================
// Token Replacement Effects
// =============================================================================

/**
 * Double the number of tokens created.
 * Example: Doubling Season, Parallel Lives, Anointed Procession
 */
@SerialName("MultiplyTokenCreation")
@Serializable
data class MultiplyTokenCreation(
    val factor: Int = 2,
    override val appliesTo: EventPattern = EventPattern.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String
        get() {
            val times = when (factor) {
                2 -> "twice"
                3 -> "three times"
                else -> "$factor times"
            }
            return "If ${appliesTo.description}, create $times that many of those tokens instead"
        }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Modify the number of tokens created by a fixed amount.
 */
@SerialName("ModifyTokenCount")
@Serializable
data class ModifyTokenCount(
    val modifier: Int,
    override val appliesTo: EventPattern = EventPattern.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, create ")
        if (modifier > 0) append("$modifier more")
        else append("${-modifier} fewer")
        append(" of those tokens instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * When a player would create one or more tokens matching [appliesTo], also create
 * [additionalTokenCount] predefined token(s) of a *different* type ([additionalTokenType]).
 *
 * Models "If you would create one or more artifact tokens, instead create those tokens
 * plus an additional Map token" (Worldwalker Helm). Unlike [ModifyTokenCount] (which adds
 * more copies of the *same* token), this appends tokens of a named predefined type. The
 * extra tokens are created once per qualifying creation event, regardless of how many
 * tokens the original effect made.
 *
 * The [appliesTo] event's `controller` / `tokenFilter` gate which creations qualify
 * (e.g. `TokenCreationEvent(controller = You, tokenFilter = GameObjectFilter.Artifact)`).
 * Per the Worldwalker Helm ruling, the added token inherits the original effect's
 * "tapped" rider when [inheritTapped] is set, but never the original tokens' abilities.
 *
 * @property additionalTokenType The predefined token to additionally create (e.g. "Map").
 * @property additionalTokenCount How many of that token to add per qualifying event.
 * @property inheritTapped When true, the added token enters tapped if the original
 *           creation made tapped tokens.
 */
@SerialName("CreateAdditionalToken")
@Serializable
data class CreateAdditionalToken(
    val additionalTokenType: String,
    val additionalTokenCount: Int = 1,
    val inheritTapped: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, create those tokens plus ")
        append(if (additionalTokenCount == 1) "an additional " else "$additionalTokenCount additional ")
        append(additionalTokenType)
        append(if (additionalTokenCount == 1) " token instead" else " tokens instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Counter Replacement Effects
// =============================================================================

/**
 * Double the number of counters placed.
 * Example: Doubling Season (counters), Corpsejack Menace, Innkeeper's Talent Level 3
 *
 * @param placedByYou When true, only applies when the controller of this effect is the
 *                    player putting the counters (e.g., Innkeeper's Talent: "If YOU would
 *                    put one or more counters..."). When false, applies regardless of who
 *                    is placing the counters — the recipient filter on [appliesTo] is the
 *                    sole "you control" gate (e.g., Doubling Season: "on a permanent you
 *                    control").
 */
@SerialName("DoubleCounterPlacement")
@Serializable
data class DoubleCounterPlacement(
    val placedByYou: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.CounterPlacementEvent(
        counterType = CounterTypeFilter.PlusOnePlusOne,
        recipient = RecipientFilter.CreatureYouControl
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, place twice that many counters instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Add additional counters when counters are placed.
 * Example: Hardened Scales (+1), Winding Constrictor (+1), Branching Evolution (double)
 */
@SerialName("ModifyCounterPlacement")
@Serializable
data class ModifyCounterPlacement(
    val modifier: Int = 1,
    override val appliesTo: EventPattern = EventPattern.CounterPlacementEvent(
        counterType = CounterTypeFilter.PlusOnePlusOne,
        recipient = RecipientFilter.CreatureYouControl
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, ")
        if (modifier > 0) {
            append("$modifier additional counter")
            if (modifier > 1) append("s")
            append(" is placed")
        } else {
            append("${-modifier} fewer counter")
            if (-modifier > 1) append("s")
            append(" is placed")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Zone Change Replacement Effects
// =============================================================================

/**
 * Redirect a zone change to a different destination.
 * Example: Rest in Peace (graveyard → exile), Leyline of the Void
 *
 * When [linkToSource] is true and [newDestination] is [Zone.EXILE], the redirected card is
 * linked to the replacement's source permanent via its `LinkedExileComponent`, so the source
 * can later reference the cards it exiled — e.g. Valgavoth, Terror Eater ("If a card you didn't
 * control would be put into an opponent's graveyard from anywhere, exile it instead." + "you may
 * play cards exiled with Valgavoth"). Ignored for non-exile destinations.
 *
 * ## Card-intrinsic "from anywhere" self-replacements
 *
 * When [selfOnly] is true the redirect is the moving card's own ability referring to itself
 * ("If ~ would be put into a graveyard from anywhere, …") and therefore functions in **every**
 * zone (CR 614.12), not just while the source is on the battlefield. The engine carries it on the
 * card entity itself rather than scanning the battlefield, so a card milled, discarded, or
 * countered on the stack is redirected too. It stops applying only while the source is on the
 * battlefield and has lost all abilities.
 *
 * [shuffleIntoLibrary] pairs with `newDestination = Zone.LIBRARY` for the Darksteel Colossus /
 * Progenitus family ("reveal ~ and shuffle it into its owner's library instead") — the card is
 * shuffled in rather than placed on top. [reveal] shows the card as it is shuffled away.
 */
@SerialName("RedirectZoneChange")
@Serializable
data class RedirectZoneChange(
    val newDestination: Zone,
    override val appliesTo: EventPattern,
    val linkToSource: Boolean = false,
    val selfOnly: Boolean = false,
    val shuffleIntoLibrary: Boolean = false,
    val reveal: Boolean = false
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, ")
        if (reveal) append("reveal it and ")
        if (shuffleIntoLibrary && newDestination == Zone.LIBRARY) {
            append("shuffle it into its owner's library instead")
        } else {
            append("put it into ${newDestination.displayName} instead")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Permanent enters the battlefield tapped.
 * Example: Glacial Fortress (conditional), tap lands, Thalia Heretic Cathar, Steam Vents (pay life)
 *
 * @param unlessCondition If non-null, the permanent only enters tapped when this condition is NOT met.
 *                        Used for "check lands" like Sulfur Falls ("enters tapped unless you control an Island or a Mountain").
 * @param payLifeCost If non-null, the player may pay this much life to have the permanent enter untapped.
 *                    Used for "shock lands" like Steam Vents ("you may pay 2 life. If you don't, it enters tapped").
 */
/**
 * Generic "as ~ enters the battlefield, run [effect]" replacement.
 *
 * The wrapped [effect] executes via the normal effect-executor pipeline at the
 * moment the source permanent enters, AFTER it has been placed on the battlefield
 * (so `EffectTarget.Self` resolves to the entering permanent) but BEFORE the
 * standard `EntersTapped` check runs. The effect may pause for player input
 * (continuations, target selection, sub-decisions) just like any other effect.
 *
 * Use this to compose ETB-time choices out of existing atoms — e.g. SOI shadow
 * lands wrap [com.wingedsheep.sdk.scripting.effects.MayRevealCardFromHandEffect]
 * with `otherwise = Effects.Tap(EffectTarget.Self)`; a future "as ~ enters,
 * sacrifice another creature" land could wrap `Effects.Sacrifice(filter)`.
 *
 * Distinct from a "when ~ enters" [com.wingedsheep.sdk.scripting.trigger.Trigger]:
 * triggers fire after entry resolves and use the stack, so they can't gate ETB-time
 * state like the tapped-on-entry flag. This replacement runs synchronously inline
 * with the entry event.
 */
@SerialName("OnEnterRunEffect")
@Serializable
data class OnEnterRunEffect(
    val effect: com.wingedsheep.sdk.scripting.effects.Effect,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = "As this permanent enters, ${effect.description.lowercase()}"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newEffect = effect.applyTextReplacement(replacer)
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newEffect !== effect || newAppliesTo !== appliesTo)
            copy(effect = newEffect, appliesTo = newAppliesTo)
        else this
    }
}

@SerialName("EntersTapped")
@Serializable
data class EntersTapped(
    val unlessCondition: Condition? = null,
    val payLifeCost: Int? = null,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = when {
        payLifeCost != null -> "As this permanent enters, you may pay $payLifeCost life. If you don't, it enters tapped."
        unlessCondition != null -> "This permanent enters tapped unless ${unlessCondition.description}"
        else -> "If ${appliesTo.description}, it enters tapped"
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * "Permanents matching [appliesTo] enter the battlefield untapped" — the inverse of
 * [EntersTapped]. Models a *static* effect carried by a permanent that overrides a tapped
 * entry of OTHER permanents it cares about (e.g. The Wandering Minstrel's "Lands you control
 * enter untapped"). Unlike [EntersTapped], which is a self-replacement consumed once as the
 * source itself enters, this is a runtime replacement consulted from the battlefield while the
 * source is in play, so the [appliesTo] filter should describe the *affected* permanents (e.g.
 * `GameObjectFilter.Land.youControl()`).
 *
 * Per CR 614 (replacement-effect ordering), if a permanent would enter tapped via another
 * replacement, the affected permanent's controller chooses which to apply first — with this
 * effect available they'd choose untapped — and a permanent simply put onto the battlefield
 * tapped (no replacement) enters untapped instead. Both outcomes collapse to "enters untapped",
 * which is what the engine applies.
 */
@SerialName("EntersUntapped")
@Serializable
data class EntersUntapped(
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = "If ${appliesTo.description}, it enters untapped"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * "Permanents matching [appliesTo] enter the battlefield tapped" — the global/group counterpart
 * of the self-only [EntersTapped]. Models a *static* effect carried by a permanent that taps
 * OTHER permanents it cares about as they enter (e.g. Zhao, the Moon Slayer's "Nonbasic lands
 * enter tapped"; also Imposing Sovereign / Authority of the Consuls / Thalia, Heretic Cathar
 * "creatures your opponents control enter tapped").
 *
 * Unlike [EntersTapped] — a self-replacement consumed once as the source itself enters — this is
 * a runtime replacement stamped into the source's replacement component and consulted from the
 * battlefield whenever some *other* permanent would enter, so the [appliesTo] filter describes
 * the *affected* permanents (e.g. `GameObjectFilter.NonbasicLand`).
 *
 * Per CR 614 (replacement-effect ordering), an [EntersUntapped] effect that also matches the
 * entering permanent wins — the engine's entry paths consult [EntersUntapped] first and only
 * apply this tap when no untapped replacement applies.
 */
@SerialName("PermanentsEnterTapped")
@Serializable
data class PermanentsEnterTapped(
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = "If ${appliesTo.description}, it enters tapped"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Permanent/creature enters with counters.
 * Example: Master Biomancer, Metallic Mimic
 *
 * @param condition When non-null, the counters are only added if this condition evaluates true
 *                  at the moment the permanent enters the battlefield. Used for cards like
 *                  Frilled Sparkshooter ("This creature enters with a +1/+1 counter on it if
 *                  an opponent lost life this turn.").
 */
@SerialName("EntersWithCounters")
@Serializable
data class EntersWithCounters(
    val counterType: CounterTypeFilter = CounterTypeFilter.PlusOnePlusOne,
    val count: Int,
    val selfOnly: Boolean = false,
    val condition: Condition? = null,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, it enters with $count ${counterType.description} counters")
        if (condition != null) append(" if ${condition.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newCondition = condition?.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newCondition !== condition)
            copy(appliesTo = newAppliesTo, condition = newCondition)
        else this
    }
}

/**
 * Permanent/creature enters with a dynamic number of counters.
 * Example: Stag Beetle (enters with X +1/+1 counters where X = number of other creatures)
 *
 * @param otherOnly When true, this effect only applies to OTHER creatures entering
 *                  (not the permanent with this replacement effect). Used for
 *                  Gev, Scaled Scorch: "Other creatures you control enter with additional counters."
 */
@SerialName("EntersWithDynamicCounters")
@Serializable
data class EntersWithDynamicCounters(
    val counterType: CounterTypeFilter = CounterTypeFilter.PlusOnePlusOne,
    val count: DynamicAmount,
    val otherOnly: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it enters with ${count.description} ${counterType.description} counters"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newCount = count.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newCount !== count) copy(appliesTo = newAppliesTo, count = newCount) else this
    }
}

/**
 * Permanent enters the battlefield with [keywords] (CR 614.1c) — the keyword counterpart of
 * [EntersWithCounters]. Example: Kavu Titan "If this creature was kicked, it enters with three
 * +1/+1 counters on it and with trample" — an [EntersWithCounters] plus an [EntersWithKeywords],
 * both gated on the same [condition].
 *
 * The grant happens as the permanent enters: no trigger, no stack, no response window. It is
 * entry-timestamped for Rule 613 layer ordering (a later "loses all abilities" effect removes
 * it) and lasts as long as the permanent remains on the battlefield — it is cleaned up when the
 * permanent leaves (a new object per CR 400.7) and does NOT re-apply if the keyword is removed.
 *
 * @param keywords The keywords the entering permanent has from the moment it enters.
 * @param condition When non-null, the keywords are only granted if this condition evaluates true
 *                  at the moment the permanent enters (e.g. [conditions.WasKicked], read from the
 *                  durable cast-choices bag).
 * @param selfOnly When true, only applies to the permanent carrying this effect as it enters,
 *                 never to other permanents matching [appliesTo] (mirrors [EntersWithCounters]).
 */
@SerialName("EntersWithKeywords")
@Serializable
data class EntersWithKeywords(
    val keywords: List<Keyword>,
    val condition: Condition? = null,
    val selfOnly: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, it enters with ")
        append(keywords.joinToString(" and ") { it.displayName.lowercase() })
        if (condition != null) append(" if ${condition.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newCondition = condition?.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newCondition !== condition)
            copy(appliesTo = newAppliesTo, condition = newCondition)
        else this
    }
}

// =============================================================================
// Damage Replacement Effects
// =============================================================================

/**
 * Prevent damage.
 * Example: Fog effects, protection, damage shields
 *
 * The optional [restrictions] list lets a card gate the prevention on arbitrary
 * additional conditions (mirroring [ModifyLifeLoss.restrictions]). Each entry is a
 * [Condition] evaluated against the source permanent's controller; the prevention
 * only applies when *all* restrictions hold. This is how "as long as …, prevent …"
 * statics are expressed without a dedicated conditional-replacement wrapper — e.g.
 * Spirit of Resistance ("As long as you control a permanent of each color, prevent
 * all damage that would be dealt to you").
 */
@SerialName("PreventDamage")
@Serializable
data class PreventDamage(
    val amount: Int? = null,  // null = prevent all
    val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(appliesTo.description)
        append(", prevent ")
        if (amount == null) {
            append("that damage")
        } else {
            append("$amount of that damage")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(appliesTo = newAppliesTo, restrictions = newRestrictions) else this
    }
}

/**
 * Redirect damage to another target.
 * Example: Pariah, Stuffy Doll, Boros Reckoner
 */
@SerialName("RedirectDamage")
@Serializable
data class RedirectDamage(
    val redirectTo: EffectTarget,
    override val appliesTo: EventPattern,
    /**
     * Optional gate evaluated against the *replacement source* at the moment damage
     * would be redirected. When non-null, the redirect applies only while the condition
     * holds — e.g. `SourceIsUntapped` for Martyrs of Korlis ("As long as this creature
     * is untapped, …"). A `null` condition means the redirect always applies (Harsh
     * Judgment, Pariah).
     */
    val condition: Condition? = null
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, that damage is dealt to ${redirectTo.description} instead")
        condition?.let { append(" (${it.description})") }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Double damage dealt.
 * Example: Furnace of Rath, Insult // Injury
 *
 * The optional [restrictions] list lets a card gate the doubling on arbitrary
 * additional conditions (mirroring [PreventDamage.restrictions]). Each entry is a
 * [Condition] evaluated against the source permanent's controller; the doubling only
 * applies when *all* restrictions hold — this is how delirium-gated forms are expressed
 * without a dedicated conditional-replacement wrapper, e.g. The Rollercrusher Ride
 * ("…while there are four or more card types among cards in your graveyard, it deals
 * double that damage instead").
 */
@SerialName("DoubleDamage")
@Serializable
data class DoubleDamage(
    val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(appliesTo.description)
        append(", it deals double that damage instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(appliesTo = newAppliesTo, restrictions = newRestrictions) else this
    }
}

/**
 * Modify damage dealt by an additive amount — either a fixed [modifier] or, when
 * [dynamicModifier] is supplied, an amount computed at damage time against the
 * replacement's *source* permanent.
 *
 * Examples:
 * - Valley Flamecaller ("If a Lizard, Mouse, Otter, or Raccoon you control would deal
 *   damage to a permanent or player, it deals that much damage plus 1 instead.") →
 *   `ModifyDamageAmount(modifier = 1, appliesTo = …)`.
 * - Fated Firepower ("If a source you control would deal damage to an opponent or a
 *   permanent an opponent controls, it deals that much damage plus an amount of damage
 *   equal to the number of fire counters on this enchantment instead.") →
 *   `ModifyDamageAmount(dynamicModifier = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named("fire")),
 *                       appliesTo = DamageEvent(source = SourceFilter.YouControl,
 *                                               recipient = RecipientFilter.OpponentOrPermanentTheyControl))`.
 *
 * When [dynamicModifier] is non-null it is evaluated with the replacement's source
 * permanent as the resolution source (so `DynamicAmount.EntityProperty(Source, …)` reads
 * the source's own characteristics/counters); otherwise the flat [modifier] is added.
 */
@SerialName("ModifyDamageAmount")
@Serializable
data class ModifyDamageAmount(
    val modifier: Int = 0,
    val dynamicModifier: DynamicAmount? = null,
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String = buildString {
        val bonus = dynamicModifier?.description ?: "$modifier"
        append("If ${appliesTo.description}, it deals that much damage plus $bonus instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newDynamic = dynamicModifier?.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newDynamic !== dynamicModifier)
            copy(appliesTo = newAppliesTo, dynamicModifier = newDynamic)
        else this
    }
}

/**
 * Cap damage at a maximum amount. If a matching source would deal more than
 * [maxAmount] damage, it deals exactly [maxAmount] instead (smaller amounts are
 * unchanged). Distinct from [PreventDamage] (which subtracts) and [ModifyDamageAmount]
 * (which adds): capping clamps to an upper bound.
 *
 * Example: Divine Presence ("If a source would deal 4 or more damage to a permanent
 * or player, that source deals 3 damage to that permanent or player instead.") →
 * `CapDamage(maxAmount = 3, appliesTo = DamageEvent(recipient = AnyPermanentOrPlayer))`.
 */
@SerialName("CapDamage")
@Serializable
data class CapDamage(
    val maxAmount: Int,
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it deals $maxAmount damage instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Raise damage to a minimum amount — the floor mirror of [CapDamage]. If a matching source
 * would deal **less than** the minimum, it deals exactly the minimum instead (larger amounts
 * are unchanged, and a zero would-be amount is not raised — the source only "deals damage" once
 * it deals a positive amount). Distinct from [ModifyDamageAmount] (which adds unconditionally):
 * this clamps to a lower bound.
 *
 * The minimum is [minAmount], or — when [dynamicMinimum] is non-null — an amount evaluated at
 * damage time against the **replacement's source** permanent (as with [ModifyDamageAmount]'s
 * `dynamicModifier`). Ojer Axonil, Deepest Might: "If a red source you control would deal an
 * amount of noncombat damage less than Ojer Axonil's power to an opponent, that source deals
 * damage equal to Ojer Axonil's power instead." →
 * `SetMinimumDamage(dynamicMinimum = DynamicAmount.SourcePower, appliesTo = DamageEvent(
 *   recipient = Opponent, source = SourceFilter.Matching(red you-control), damageType = NonCombat))`.
 */
@SerialName("SetMinimumDamage")
@Serializable
data class SetMinimumDamage(
    val minAmount: Int = 0,
    val dynamicMinimum: DynamicAmount? = null,
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String
        get() {
            val floor = dynamicMinimum?.description ?: "$minAmount"
            return "If ${appliesTo.description}, it deals $floor damage instead"
        }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newDynamic = dynamicMinimum?.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newDynamic !== dynamicMinimum)
            copy(appliesTo = newAppliesTo, dynamicMinimum = newDynamic)
        else this
    }
}

// =============================================================================
// Draw Replacement Effects
// =============================================================================

/**
 * Modify the number of cards a draw event draws by a fixed amount, optionally gated by
 * additional [restrictions]. Applied at the call site where the original draw count is
 * announced (spell/ability resolution and the draw step), so the modifier fires once per
 * draw instruction (CR 121.2a: "An instruction to draw multiple cards can be modified by
 * replacement effects that refer to the number of cards drawn. This modification occurs
 * before considering any of the individual card draws.") and is not re-applied when a
 * paused per-card draw loop resumes.
 *
 * Each entry in [restrictions] is a [Condition] evaluated against the drawing player as
 * the controller context; the modification only applies when ALL restrictions hold. This
 * mirrors [ModifyLifeLoss]'s shape — use it for cards whose extra-draw clause is gated by
 * arbitrary additional conditions. Note that "you" in restriction text reads as the drawing
 * player, not the source's controller — for `DrawEvent(player = Player.You)` they're the
 * same, but a future `DrawEvent(player = Player.EachOpponent)` card whose restriction means
 * "you" = source controller would need a source-relative condition instead.
 *
 * Examples:
 * - Quantum Riddler ("As long as you have one or fewer cards in hand, if you would draw
 *   one or more cards, you draw that many cards plus one instead"):
 *     `ModifyDrawAmount(modifier = 1,
 *                       restrictions = listOf(Conditions.CardsInHandAtMost(1)),
 *                       appliesTo = DrawEvent(player = Player.You))`
 *
 * @param modifier Flat amount added to the draw count when the event fires for a matching
 *        player. Negative values reduce the draw (clamped to ≥ 0 by the caller).
 * @param restrictions Additional [Condition]s gating when the modifier applies. Evaluated
 *        against the drawing player as controller; ALL must hold.
 */
@SerialName("ModifyDrawAmount")
@Serializable
data class ModifyDrawAmount(
    val modifier: Int,
    val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern = EventPattern.DrawEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(appliesTo.description)
        append(", they draw that many cards plus $modifier instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(appliesTo = newAppliesTo, restrictions = newRestrictions) else this
    }
}

/**
 * Modify how many cards a player mills (CR 701.13). Additive: a [modifier] of `+4` makes a
 * player who would mill N instead mill `N + 4`; negative values reduce the mill (clamped to ≥ 0
 * by the caller). Applied at the mill announcement, once per mill instruction, exactly like
 * [ModifyDrawAmount] — so a paused-and-resumed mill never double-modifies.
 *
 * The [appliesTo] [EventPattern.MillEvent] gates which player's mills are affected relative to
 * the source's controller (`Player.You` / `Player.EachOpponent` / `Player.Each`). [restrictions]
 * are additional [Condition]s evaluated against the milling player as controller; ALL must hold.
 *
 * Example — The Water Crystal: "If an opponent would mill one or more cards, they mill that many
 * cards plus four instead" → `ModifyMillAmount(4, appliesTo = MillEvent(Player.EachOpponent))`.
 */
@SerialName("ModifyMillAmount")
@Serializable
data class ModifyMillAmount(
    val modifier: Int,
    val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern = EventPattern.MillEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(appliesTo.description)
        append(", they mill that many cards plus $modifier instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(appliesTo = newAppliesTo, restrictions = newRestrictions) else this
    }
}

/**
 * Replace drawing with another effect.
 * Example: Underrealm Lich (look at 3, put 1 in hand, rest in graveyard)
 */
@SerialName("ReplaceDrawWith")
@Serializable
data class ReplaceDrawWithEffect(
    val replacementEffect: Effect,
    val optional: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.DrawEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, ")
        if (optional) append("you may ")
        append("instead ${replacementEffect.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newReplacementEffect = replacementEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newReplacementEffect !== replacementEffect)
            copy(appliesTo = newAppliesTo, replacementEffect = newReplacementEffect)
        else this
    }
}

/**
 * Insert an extra effect into an explore (CR 614, CR 701.44). Replaces "[a permanent matching
 * [appliesTo]'s filter] explores" with "[prefixEffect] happens, then that permanent explores".
 *
 * Modeled on [ReplaceDrawWithEffect]: like draw replacement, explore isn't dispatched as a
 * generic replaceable event, so `ExploreEffectExecutor` consults this directly at explore time.
 * When a matching `ModifyExplore` is on the battlefield, the executor re-issues the explore as
 * `Composite([prefixEffect], ExploreEffect(sameCreature, replacementsApplied = true))`, reusing
 * the composite executor's pause-sequencing so a prefix that pauses (e.g. Scry's top/bottom
 * decision) resolves fully before the explore runs.
 *
 * [appliesTo]'s filter scopes *which* explores are modified, evaluated with the replacement
 * source's controller as "you" — `ExploredEvent(Creature.youControl())` for "if a creature you
 * control would explore". The [prefixEffect] runs as the source's controller.
 *
 * Twists and Turns: `ModifyExplore(Effects.Scry(1), ExploredEvent(Creature.youControl()))` —
 * "If a creature you control would explore, instead you scry 1, then that creature explores."
 */
@SerialName("ModifyExplore")
@Serializable
data class ModifyExplore(
    val prefixEffect: Effect,
    override val appliesTo: EventPattern = EventPattern.ExploredEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, first ${prefixEffect.description}, then it explores"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newPrefix = prefixEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newPrefix !== prefixEffect)
            copy(appliesTo = newAppliesTo, prefixEffect = newPrefix)
        else this
    }
}

/**
 * Prevent drawing (with optional replacement).
 * Example: Spirit of the Labyrinth (second draw), Narset Parter of Veils
 */
@SerialName("PreventDraw")
@Serializable
data class PreventDraw(
    override val appliesTo: EventPattern = EventPattern.DrawEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, that draw doesn't happen"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Life Replacement Effects
// =============================================================================

/**
 * Prevent life gain.
 * Example: Erebos, Sulfuric Vortex
 */
@SerialName("PreventLifeGain")
@Serializable
data class PreventLifeGain(
    override val appliesTo: EventPattern = EventPattern.LifeGainEvent()
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, that player gains no life instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Damage can't be prevented.
 * Example: Sunspine Lynx, Leyline of Punishment
 *
 * While a permanent with this replacement effect is on the battlefield,
 * all damage is treated as though it can't be prevented (protection,
 * prevention shields, etc. are ignored).
 */
@SerialName("DamageCantBePrevented")
@Serializable
data class DamageCantBePrevented(
    override val appliesTo: EventPattern = EventPattern.DamageEvent()
) : ReplacementEffect {
    override val description: String = "Damage can't be prevented"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Modify life gain amount. Combines multiplicative and additive modifications:
 * `newAmount = (originalAmount * multiplier) + modifier`, clamped to ≥ 0.
 *
 * Examples:
 * - Alhammarret's Archive — double life gain: `ModifyLifeGain(multiplier = 2)`
 * - Leyline of Hope — "you gain that much life plus 1 instead":
 *     `ModifyLifeGain(modifier = 1, appliesTo = LifeGainEvent(player = Player.You))`
 *
 * @param multiplier Multiplicative factor applied first (default 2 to preserve the
 *        historical Alhammarret's Archive default).
 * @param modifier Flat amount added after multiplication (default 0 = unchanged).
 */
@SerialName("ModifyLifeGain")
@Serializable
data class ModifyLifeGain(
    val multiplier: Int = 2,
    val modifier: Int = 0,
    override val appliesTo: EventPattern = EventPattern.LifeGainEvent(),
    /**
     * Additional [Condition]s gating when this modification applies, evaluated against the
     * gaining player as controller; ALL must hold. Used by Phial of Galadriel
     * (`restrictions = listOf(Conditions.LifeAtMost(5))` — "while you have 5 or less life").
     */
    val restrictions: List<Condition> = emptyList()
) : ReplacementEffect {
    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(appliesTo.description)
        append(", gain ")
        when {
            multiplier == 0 && modifier == 0 -> append("no life")
            multiplier == 1 && modifier > 0 -> append("that much life plus $modifier")
            multiplier == 1 && modifier < 0 -> append("${-modifier} less life")
            multiplier != 1 && modifier == 0 -> when (multiplier) {
                2 -> append("twice that much life")
                else -> append("$multiplier times that much life")
            }
            else -> {
                when (multiplier) {
                    2 -> append("twice that much life")
                    else -> append("$multiplier times that much life")
                }
                if (modifier > 0) append(" plus $modifier") else append(" minus ${-modifier}")
            }
        }
        append(" instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(appliesTo = newAppliesTo, restrictions = newRestrictions) else this
    }
}

/**
 * Modify life loss amount. Combines multiplicative and additive modifications:
 * `newAmount = (originalAmount * multiplier) + modifier`, clamped to ≥ 0.
 *
 * Per the printed reminder text "(Damage causes loss of life.)" on Bloodletter of
 * Aclazotz, this replacement applies to life loss caused by damage as well as direct
 * life-loss effects. Lifelink and other damage-based triggers still see the original
 * damage amount — only the life total reduction is modified.
 *
 * The [restrictions] list lets cards layer arbitrary additional gates (e.g., "during
 * your turn", "while you control a Vampire") onto the replacement; the engine
 * evaluates each entry with the source permanent's controller as the [Condition]
 * context and only applies the modification when *all* restrictions hold.
 *
 * Examples:
 * - Bloodletter of Aclazotz (loses twice as much during your turn):
 *     `ModifyLifeLoss(multiplier = 2, restrictions = listOf(IsYourTurn),
 *                    appliesTo = LifeLossEvent(player = Player.EachOpponent))`
 * - "Each opponent loses an additional 1 life":
 *     `ModifyLifeLoss(modifier = 1, appliesTo = LifeLossEvent(player = Player.EachOpponent))`
 * - "If you would lose life, you lose 1 less life instead" (with floor at 0):
 *     `ModifyLifeLoss(modifier = -1, appliesTo = LifeLossEvent(player = Player.You))`
 *
 * @param multiplier Multiplicative factor applied first (default 1 = unchanged).
 * @param modifier Flat amount added after multiplication (default 0 = unchanged).
 * @param restrictions Additional [Condition]s gating when this replacement applies.
 *        Evaluated against the source permanent's controller; ALL must hold.
 */
@SerialName("ModifyLifeLoss")
@Serializable
data class ModifyLifeLoss(
    val multiplier: Int = 1,
    val modifier: Int = 0,
    val restrictions: List<com.wingedsheep.sdk.scripting.conditions.Condition> = emptyList(),
    override val appliesTo: EventPattern = EventPattern.LifeLossEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(appliesTo.description)
        append(", they lose ")
        when {
            multiplier == 0 && modifier == 0 -> append("no life")
            multiplier == 1 && modifier > 0 -> append("that much life plus $modifier")
            multiplier == 1 && modifier < 0 -> append("${-modifier} less life")
            multiplier != 1 && modifier == 0 -> when (multiplier) {
                2 -> append("twice that much life")
                else -> append("$multiplier times that much life")
            }
            else -> {
                when (multiplier) {
                    2 -> append("twice that much life")
                    else -> append("$multiplier times that much life")
                }
                if (modifier > 0) append(" plus $modifier") else append(" minus ${-modifier}")
            }
        }
        append(" instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(appliesTo = newAppliesTo, restrictions = newRestrictions) else this
    }
}

/**
 * Floor the resulting life total when a player would lose life from damage.
 *
 * If a damage event would reduce the [appliesTo]-matching player's life total below
 * [floor], the life-loss amount is capped so the resulting life total equals [floor].
 * Damage that would not breach the floor is unchanged. The damage event itself still
 * fires at the original amount — only the life-total reduction is capped — so
 * lifelink, damage-dealt triggers, and abilities that key off the dealt damage still
 * see the full amount, matching the printed ruling on Ali from Cairo: "the full
 * damage is dealt (and abilities that trigger on damage being dealt still trigger),
 * but the full loss of life is not applied."
 *
 * Scope: damage-as-life-loss (CR 120.3a) only. Direct life-loss effects (pay-life
 * costs, Drain Life, Greed) are deliberately unaffected — the engine wires this
 * replacement only at the damage-pipeline call sites; `LoseLifeExecutor` skips it,
 * matching the ruling "This effect does not apply to effects which reduce your life
 * without doing damage."
 *
 * Multiple instances pick the strictest floor (highest resulting life total).
 *
 * Examples:
 * - Ali from Cairo ("Damage that would reduce your life total to less than 1
 *   reduces it to 1 instead"):
 *     `LifeLossFloor(floor = 1, appliesTo = LifeLossEvent(Player.You))`
 * - Worship ("If you control a creature, damage that would reduce your life total
 *   to less than 1 reduces it to 1 instead"):
 *     `LifeLossFloor(floor = 1, restrictions = listOf(YouControlACreature),
 *                    appliesTo = LifeLossEvent(Player.You))`
 *
 * @param floor Minimum resulting life total (default 1).
 * @param restrictions Additional [Condition]s gating when this floor applies.
 *        Evaluated against the source permanent's controller; ALL must hold.
 * @param appliesTo Life-loss event filter (which player is protected).
 */
@SerialName("LifeLossFloor")
@Serializable
data class LifeLossFloor(
    val floor: Int = 1,
    val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern = EventPattern.LifeLossEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", damage")
        } else {
            append("Damage")
        }
        append(" that would reduce ")
        when ((appliesTo as? EventPattern.LifeLossEvent)?.player) {
            Player.You -> append("your")
            Player.EachOpponent -> append("an opponent's")
            else -> append("a player's")
        }
        append(" life total to less than $floor reduces it to $floor instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(appliesTo = newAppliesTo, restrictions = newRestrictions) else this
    }
}

// =============================================================================
// Copy Replacement Effects
// =============================================================================

/**
 * Enter the battlefield as a copy of a card or permanent.
 * Example: Clone ("You may have this creature enter as a copy of any creature on the battlefield")
 * Example: Clever Impersonator ("You may have this creature enter as a copy of any nonland permanent on the battlefield")
 * Example: Superior Spider-Man ("You may have this creature enter as a copy of any creature card in a
 *          graveyard, except his name is Superior Spider-Man and he's a 4/4 Spider Human Hero ... When
 *          you do, exile that card.")
 *
 * When this permanent would enter the battlefield, the controller may choose an object in [copyFromZone]
 * matching [copyFilter]. If they do, the permanent enters as a copy of that object (with the overrides
 * below applied). If they don't (or can't), the permanent enters as itself (typically 0/0 and dies).
 *
 * @param copyFilter Filter for what can be copied. Defaults to creatures only (Clone).
 *                   Use [GameObjectFilter.Companion.NonlandPermanent] for Clever Impersonator.
 * @param copyFromZone Where to look for copy candidates. [Zone.BATTLEFIELD] (default) copies a permanent;
 *                   [Zone.GRAVEYARD] copies a creature *card* from any graveyard (Superior Spider-Man).
 * @param filterByTotalManaSpent When true, only creatures with mana value ≤ total mana spent
 *                                to cast this spell are valid copy targets. Used for Mockingbird.
 * @param additionalSubtypes Subtypes to add to the copy (e.g., "Bird" for Mockingbird; "Spider", "Human",
 *                   "Hero" for Superior Spider-Man — added "in addition to its other types").
 * @param additionalKeywords Keywords to grant to the copy (e.g., FLYING for Mockingbird).
 * @param nameOverride When non-null, the copy keeps this name instead of the copied object's name
 *                   ("except his name is Superior Spider-Man").
 * @param powerOverride When non-null, the copy's base power is set to this value ("he's a 4/4 ...").
 * @param toughnessOverride When non-null, the copy's base toughness is set to this value.
 * @param exileCopiedCard When true, the copied card is exiled after the copy is applied
 *                   ("When you do, exile that card"). Only meaningful with [copyFromZone] = graveyard.
 * @param tappedIfCopied When true, the permanent enters **tapped** if (and only if) it enters as a
 *                   copy — the "enter tapped as a copy" rider on the land-copy cycle (Vesuva,
 *                   Thespian's Stage, Echoing Deeps). If the copy is declined (or no candidate
 *                   exists) the permanent enters untapped as its printed self.
 */
@SerialName("EntersAsCopy")
@Serializable
data class EntersAsCopy(
    val optional: Boolean = true,
    val copyFilter: GameObjectFilter = GameObjectFilter.Creature,
    val copyFromZone: Zone = Zone.BATTLEFIELD,
    val filterByTotalManaSpent: Boolean = false,
    val additionalSubtypes: List<String> = emptyList(),
    val additionalKeywords: List<Keyword> = emptyList(),
    val nameOverride: String? = null,
    val powerOverride: Int? = null,
    val toughnessOverride: Int? = null,
    val exileCopiedCard: Boolean = false,
    val tappedIfCopied: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = run {
        val filterDesc = copyFilter.description
        val where = if (copyFromZone == Zone.GRAVEYARD) "$filterDesc card in a graveyard" else "$filterDesc on the battlefield"
        val subject = if (copyFilter == GameObjectFilter.Land) "this land" else "this creature"
        val tappedWord = if (tappedIfCopied) "tapped " else ""
        val lead = if (optional) {
            "You may have $subject enter ${tappedWord}as a copy of any $where"
        } else {
            "$subject enters ${tappedWord}as a copy of any $where"
        }
        buildString {
            append(lead)
            val exceptions = buildList {
                if (nameOverride != null) add("its name is $nameOverride")
                if (powerOverride != null && toughnessOverride != null) {
                    add("it's $powerOverride/$toughnessOverride")
                }
                if (additionalSubtypes.isNotEmpty()) {
                    add("a ${additionalSubtypes.joinToString(" ")} in addition to its other types")
                }
                if (additionalKeywords.isNotEmpty()) {
                    add("it has ${additionalKeywords.joinToString(", ") { it.name.lowercase() }}")
                }
            }
            if (exceptions.isNotEmpty()) append(", except ${exceptions.joinToString(" and ")}")
            if (exileCopiedCard) append(". When you do, exile that card")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Enter-With-Choice Replacement Effects
// =============================================================================

/**
 * What the player chooses as the permanent enters.
 */
@Serializable
enum class ChoiceType {
    /** Choose a color (e.g., Riptide Replicator, Ward Sliver) */
    COLOR,
    /** Choose a creature type (e.g., Doom Cannon, Cover of Darkness) */
    CREATURE_TYPE,
    /** Choose another creature you control (e.g., Dauntless Bodyguard) */
    CREATURE_ON_BATTLEFIELD,
    /**
     * Choose one of a card-defined set of named [ModeOption]s. Used for cards
     * whose entry choice gates which abilities are active — e.g., the Khans
     * cycle of Sieges ("As this enters, choose Khans or Dragons"). The chosen
     * mode is stored on the permanent and queryable via
     * [com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs].
     */
    MODE,
    /**
     * Choose a basic land type (Plains, Island, Swamp, Mountain, or Forest)
     * (e.g., Phantasmal Terrain: "As this Aura enters, choose a basic land type").
     * The chosen type is stored on the permanent in a
     * [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent]
     * and read by [com.wingedsheep.sdk.scripting.SetEnchantedLandTypeFromChosen].
     */
    BASIC_LAND_TYPE,
    /**
     * Choose an opponent (e.g., Jihad: "As this enchantment enters, choose a color
     * and an opponent"). Stored on the permanent in a
     * [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent]
     * under [com.wingedsheep.sdk.scripting.ChoiceSlot.OPPONENT] as a
     * [com.wingedsheep.engine.state.components.battlefield.ChoiceValue.EntityChoice]
     * holding the chosen player's entity id, and read back through
     * [com.wingedsheep.sdk.scripting.references.Player.ChosenOpponent].
     */
    OPPONENT,
    /**
     * Choose a land card name (e.g., Petrified Hamlet: "When this land enters, choose a land
     * card name"). Stored on the permanent in a
     * [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent] under
     * [com.wingedsheep.sdk.scripting.ChoiceSlot.CARD_NAME] as a
     * [com.wingedsheep.engine.state.components.battlefield.ChoiceValue.TextChoice], and read
     * back at static-projection / activation-legality time by
     * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.NameEqualsChosenComponent].
     */
    CARD_NAME,
    /**
     * Choose a number in `[minValue, maxValue]` as the permanent enters (CR 614.1c), e.g.
     * Shapeshifter: "As this creature enters, choose a number between 0 and 7." The chosen number
     * is stored on the permanent in a
     * [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent] under
     * [com.wingedsheep.sdk.scripting.ChoiceSlot.CHOSEN_NUMBER] as a
     * [com.wingedsheep.engine.state.components.battlefield.ChoiceValue.NumberChoice], read back by a
     * characteristic-defining ability via [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastChoice].
     * This is the *as-enters replacement* analogue of the on-resolution
     * [com.wingedsheep.sdk.scripting.effects.ChooseNumberForSourceEffect] (used for a later upkeep
     * re-choice into the same slot). Set [EntersWithChoice.minValue] / [EntersWithChoice.maxValue].
     */
    NUMBER
}

/**
 * The pool of card names offered by a [ChoiceType.CARD_NAME] [EntersWithChoice].
 *
 * - [LAND] — only registered *land* card names (Petrified Hamlet: "choose a land card name").
 * - [ANY] — every registered card name (Sorcerous Spyglass / Pithing Needle: "choose any card
 *   name"). The chosen name is still stored under [com.wingedsheep.sdk.scripting.ChoiceSlot.CARD_NAME]
 *   and read the same way; only the offered option set differs.
 */
enum class CardNamePool {
    LAND,
    ANY
}

/**
 * A single named option in an [EntersWithChoice] of type [ChoiceType.MODE].
 *
 * The [id] is the stable, machine-readable identifier referenced by
 * [com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs] and stored
 * on the resulting [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent].
 * The [label] is the human-readable display text shown in the prompt.
 *
 * [description] supplies optional rules text shown alongside the label
 * (e.g., the corresponding ability's reminder text). [iconKey] is an
 * optional asset identifier the frontend can map to an SVG icon — cards
 * that do not supply an icon get a purely textual choice.
 */
@Serializable
data class ModeOption(
    val id: String,
    val label: String,
    val description: String? = null,
    val iconKey: String? = null
)

/**
 * As this permanent enters, make a choice. The chosen value is stored on
 * the permanent for use by other abilities.
 *
 * Replaces the former EntersWithColorChoice, EntersWithCreatureTypeChoice,
 * and EntersWithCreatureChoice with a single parameterized type.
 *
 * @param choiceType What kind of choice to present
 * @param chooser Who makes the choice (default: controller)
 *
 * Examples:
 * - Riptide Replicator: `EntersWithChoice(ChoiceType.COLOR)`
 * - Callous Oppressor: `EntersWithChoice(ChoiceType.CREATURE_TYPE, chooser = Player.AnOpponent)`
 * - Dauntless Bodyguard: `EntersWithChoice(ChoiceType.CREATURE_ON_BATTLEFIELD)`
 */
@SerialName("EntersWithChoice")
@Serializable
data class EntersWithChoice(
    val choiceType: ChoiceType,
    val chooser: Player = Player.You,
    /**
     * When [choiceType] is [ChoiceType.CREATURE_TYPE], restrict the choosable
     * subtypes to this list. `null` means any creature type is allowed (the
     * default, matching cards like Three Tree City). Used by cards that
     * enumerate a specific tribal shortlist such as Eclipsed Realms.
     */
    val allowedCreatureTypes: List<String>? = null,
    /**
     * When [choiceType] is [ChoiceType.MODE], the card-defined list of named
     * options the player picks between. Required for MODE; ignored otherwise.
     */
    val modeOptions: List<ModeOption> = emptyList(),
    /**
     * When [choiceType] is [ChoiceType.NUMBER], the inclusive bounds of the number the chooser may
     * pick (e.g. `0`/`7` for Shapeshifter). Ignored for every other choice type. The chosen number
     * is written to [ChoiceSlot.CHOSEN_NUMBER].
     */
    val minValue: Int = 0,
    val maxValue: Int = 0,
    /**
     * When [choiceType] is [ChoiceType.CARD_NAME], which names are offered — a pool of just land
     * names ([CardNamePool.LAND], the default matching Petrified Hamlet) or every registered card
     * name ([CardNamePool.ANY], "choose any card name" à la Sorcerous Spyglass / Pithing Needle).
     * Ignored for every other choice type.
     */
    val cardNamePool: CardNamePool = CardNamePool.LAND,
    /**
     * When true, the chooser first looks at an opponent's hand as the permanent enters, immediately
     * before making the choice (the opponent's hand is revealed to the chooser for the rest of the
     * game per CR 701.16). Models the "look at an opponent's hand, then …" clause of Sorcerous
     * Spyglass. The look is purely informational — it does not restrict the choice. Defaults to false.
     */
    val lookAtOpponentHand: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = when (choiceType) {
        ChoiceType.COLOR -> if (chooser == Player.AnOpponent) {
            "As this permanent enters, an opponent chooses a color"
        } else {
            "As this permanent enters, choose a color"
        }
        ChoiceType.CREATURE_TYPE -> if (chooser == Player.AnOpponent) {
            "As this permanent enters, an opponent chooses a creature type"
        } else {
            "As this permanent enters, choose a creature type"
        }
        ChoiceType.CREATURE_ON_BATTLEFIELD -> "As this creature enters, choose another creature you control"
        ChoiceType.MODE -> {
            val labels = modeOptions.joinToString(" or ") { it.label }
            if (chooser == Player.AnOpponent) {
                "As this permanent enters, an opponent chooses $labels"
            } else {
                "As this permanent enters, choose $labels"
            }
        }
        ChoiceType.BASIC_LAND_TYPE -> if (chooser == Player.AnOpponent) {
            "As this permanent enters, an opponent chooses a basic land type"
        } else {
            "As this permanent enters, choose a basic land type"
        }
        ChoiceType.OPPONENT -> if (chooser == Player.AnOpponent) {
            "As this permanent enters, an opponent chooses an opponent"
        } else {
            "As this permanent enters, choose an opponent"
        }
        ChoiceType.CARD_NAME -> {
            val lookPrefix = if (lookAtOpponentHand) "look at an opponent's hand, then " else ""
            val nameKind = if (cardNamePool == CardNamePool.ANY) "any card name" else "a land card name"
            val verb = if (chooser == Player.AnOpponent) "an opponent chooses" else "choose"
            "As this permanent enters, $lookPrefix$verb $nameKind"
        }
        ChoiceType.NUMBER -> if (chooser == Player.AnOpponent) {
            "As this permanent enters, an opponent chooses a number between $minValue and $maxValue"
        } else {
            "As this permanent enters, choose a number between $minValue and $maxValue"
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Enters-With-Reveal-Counters Replacement Effect
// =============================================================================

/**
 * As this creature enters, you may reveal any number of cards from a zone
 * that match a filter. For each card revealed, put N counters on this creature.
 *
 * Generalizes the Amplify mechanic — the default parameters reproduce Amplify
 * exactly (reveal creatures from hand sharing a type, +1/+1 counters).
 *
 * @param filter Which cards can be revealed (default: creatures sharing a creature type with this)
 * @param revealSource Which zone to reveal from (default: HAND)
 * @param counterType Counter type description (default: "+1/+1")
 * @param countersPerReveal How many counters per revealed card
 *
 * Examples:
 * - Embalmed Brawler (Amplify 1): `EntersWithRevealCounters(countersPerReveal = 1)`
 * - Kilnmouth Dragon (Amplify 3): `EntersWithRevealCounters(countersPerReveal = 3)`
 */
@SerialName("EntersWithRevealCounters")
@Serializable
data class EntersWithRevealCounters(
    val filter: GameObjectFilter = GameObjectFilter(
        cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.SharesCreatureTypeWithSource)
    ),
    val revealSource: Zone = Zone.HAND,
    val counterType: String = "+1/+1",
    val countersPerReveal: Int,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String =
        "As this creature enters, you may reveal any number of cards from your ${revealSource.name.lowercase()} that match. For each card revealed this way, put $countersPerReveal $counterType counter${if (countersPerReveal > 1) "s" else ""} on it."

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAppliesTo !== appliesTo) copy(filter = newFilter, appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Enters-With-Devour Replacement Effect
// =============================================================================

/**
 * Devour (CR 702.82) and its variants. As this permanent enters, the controller
 * may sacrifice any number of permanents matching [sacrificeFilter]. The
 * permanent then enters with [multiplier] × (count sacrificed) counters of
 * [counterType] on it.
 *
 * Devour is the rules-precise replacement effect generated by
 * [com.wingedsheep.sdk.scripting.KeywordAbility.Devour]. Cards that print the
 * Devour keyword should declare both: the [KeywordAbility] surfaces the
 * keyword for rules-text rendering, and this replacement effect supplies the
 * mechanical behavior.
 *
 * Examples:
 * - Plain Devour 2 (creatures): `EntersWithDevour(multiplier = 2)`
 * - Famished Worldsire — Devour land 3:
 *     `EntersWithDevour(multiplier = 3,
 *                       sacrificeFilter = GameObjectFilter.Land,
 *                       variant = "land")`
 *
 * @param multiplier Counters placed per sacrificed permanent.
 * @param sacrificeFilter Which permanents the controller may sacrifice. The
 *        engine restricts to permanents the controller controls regardless
 *        (CR 701.21a "to sacrifice a permanent, its controller moves it…").
 * @param counterType The counter type granted (default: +1/+1).
 * @param variant Optional rules-text variant ("" or "land") used in the
 *        description string. Mechanically inert.
 */
@SerialName("EntersWithDevour")
@Serializable
data class EntersWithDevour(
    val multiplier: Int,
    val sacrificeFilter: GameObjectFilter = GameObjectFilter.Creature,
    val counterType: com.wingedsheep.sdk.scripting.events.CounterTypeFilter =
        com.wingedsheep.sdk.scripting.events.CounterTypeFilter.PlusOnePlusOne,
    val variant: String = "",
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("Devour")
        if (variant.isNotBlank()) {
            append(" ")
            append(variant)
        }
        append(" ")
        append(multiplier)
        append(" (As this creature enters, you may sacrifice any number of ")
        append(sacrificeFilter.description)
        append("s. It enters with ")
        append(multiplier)
        append(" times that many ")
        append(counterType.description)
        append(" counters on it.)")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newFilter = sacrificeFilter.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newFilter !== sacrificeFilter)
            copy(appliesTo = newAppliesTo, sacrificeFilter = newFilter)
        else this
    }
}

// =============================================================================
// Damage-to-Counters Replacement Effect
// =============================================================================

/**
 * Replace damage dealt to a player with counters on this permanent.
 * Example: Force Bubble — "If damage would be dealt to you, put that many
 * depletion counters on this enchantment instead."
 *
 * @param counterType The type of counter to add (e.g., "depletion")
 * @param sacrificeThreshold If non-null, sacrifice this permanent when it has
 *        this many or more counters of the specified type (state-triggered ability)
 */
@SerialName("ReplaceDamageWithCounters")
@Serializable
data class ReplaceDamageWithCounters(
    val counterType: String,
    val sacrificeThreshold: Int? = null,
    override val appliesTo: EventPattern = EventPattern.DamageEvent(
        recipient = RecipientFilter.You
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, put that many $counterType counters on this permanent instead")
        if (sacrificeThreshold != null) {
            append(". When there are $sacrificeThreshold or more $counterType counters on this permanent, sacrifice it")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Prevent matched damage and, instead, each opponent of this permanent's controller mills that
 * many cards.
 *
 * Models The Mindskinner (DSK): "If a source you control would deal damage to an opponent, prevent
 * that damage and each opponent mills that many cards." The [appliesTo] pattern scopes which damage
 * is replaced — typically `DamageEvent(recipient = Opponent, source = Matching(<you control>))`.
 *
 * Like [ReplaceDamageWithCounters], the damage is neither dealt nor prevented in the
 * "prevention shield" sense — it is *replaced* (CR 615): the mill happens instead. The mill goes to
 * every opponent of the controller, not just the damaged one, matching the printed "each opponent"
 * wording (identical to a single opponent in a two-player game).
 */
@SerialName("ReplaceDamageWithMill")
@Serializable
data class ReplaceDamageWithMill(
    override val appliesTo: EventPattern = EventPattern.DamageEvent(
        recipient = RecipientFilter.Opponent
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, prevent that damage and each opponent mills that many cards instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Extra Turn Replacement Effects
// =============================================================================

/**
 * Prevent extra turns from being taken.
 * Example: Ugin's Nexus — "If a player would begin an extra turn, that player
 * skips that turn instead."
 *
 * Checked by TakeExtraTurnExecutor before granting extra turns.
 */
@SerialName("PreventExtraTurns")
@Serializable
data class PreventExtraTurns(
    override val appliesTo: EventPattern = EventPattern.ExtraTurnEvent()
) : ReplacementEffect {
    override val description: String =
        "If a player would begin an extra turn, that player skips that turn instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Redirect a zone change to a different destination AND execute an additional effect.
 * Example: Ugin's Nexus — "If Ugin's Nexus would be put into a graveyard from
 * the battlefield, instead exile it and take an extra turn after this one."
 *
 * Extends RedirectZoneChange with an additional effect that fires when the replacement applies.
 *
 * @param newDestination The zone to redirect to (e.g., Exile)
 * @param additionalEffect The effect to execute when replacement fires (e.g., TakeExtraTurnEffect)
 * @param selfOnly When true, only applies when the entity being moved IS this permanent
 * @param linkToSource When true and [newDestination] is [Zone.EXILE], the redirected card is linked
 *        to this replacement's source permanent via its `LinkedExileComponent`, so the source can
 *        later reference the cards it exiled (mirrors [RedirectZoneChange.linkToSource]). Enables
 *        "if a creature an opponent controls would die, instead exile it and ...; {cost}: return a
 *        creature card exiled with ~" recursion (The Darkness Crystal). Ignored for non-exile
 *        destinations.
 * @param appliesTo The zone change event this replacement intercepts
 */
@SerialName("RedirectZoneChangeWithEffect")
@Serializable
data class RedirectZoneChangeWithEffect(
    val newDestination: Zone,
    val additionalEffect: Effect,
    val selfOnly: Boolean = false,
    val linkToSource: Boolean = false,
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, instead put it into ${newDestination.displayName} and ${additionalEffect.description}"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newAdditionalEffect = additionalEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newAdditionalEffect !== additionalEffect)
            copy(appliesTo = newAppliesTo, additionalEffect = newAdditionalEffect)
        else this
    }
}

// =============================================================================
// Token Creation Replacement Effects
// =============================================================================

/**
 * Replace token creation with creating token copies of the permanent this source is attached to.
 *
 * Works for both Equipment (attached creature) and Auras (enchanted artifact / creature / etc.).
 * The source must have an [com.wingedsheep.engine.state.components.battlefield.AttachedToComponent]
 * — the engine looks at that to find the permanent to copy.
 *
 * Examples:
 * - Mirrormind Crown — "As long as this Equipment is attached to a creature, the first time
 *   you would create one or more tokens each turn, you may instead create that many tokens
 *   that are copies of equipped creature."
 *     `ReplaceTokenCreationWithAttachedCopy(attachmentVerb = "equipped")`
 * - Moonlit Meditation — "Enchant artifact or creature you control. The first time you would
 *   create one or more tokens each turn, you may instead create that many tokens that are
 *   copies of enchanted permanent."
 *     `ReplaceTokenCreationWithAttachedCopy(attachmentVerb = "enchanted")`
 *
 * @param optional If true, the player may choose whether to apply the replacement ("you may")
 * @param oncePerTurn If true, only applies to the first token creation each turn
 * @param attachmentVerb Word used in the description for the attached permanent
 *        (e.g. "equipped", "enchanted", "fortified"). Display-only — behavior is driven
 *        entirely by the source's [com.wingedsheep.engine.state.components.battlefield.AttachedToComponent]
 *        and validated at cast / attach time by auraTarget / equipmentTarget.
 */
@SerialName("ReplaceTokenCreationWithAttachedCopy")
@Serializable
data class ReplaceTokenCreationWithAttachedCopy(
    val optional: Boolean = true,
    val oncePerTurn: Boolean = true,
    val attachmentVerb: String = "attached",
    override val appliesTo: EventPattern = EventPattern.TokenCreationEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        if (oncePerTurn) append("The first time ")
        else append("If ")
        append("you would create one or more tokens")
        if (oncePerTurn) append(" each turn")
        append(", ")
        if (optional) append("you may instead ")
        else append("instead ")
        append("create that many tokens that are copies of ")
        append(attachmentVerb)
        append(" permanent")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Generic Replacement Effect
// =============================================================================
