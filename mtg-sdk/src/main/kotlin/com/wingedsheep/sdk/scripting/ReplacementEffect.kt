package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
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
 *     appliesTo = EventPattern.TokenCreationEvent(controller = Player.You)
 * )
 *
 * // Hardened Scales
 * ModifyCounterPlacement(
 *     modifier = 1,
 *     appliesTo = EventPattern.CounterPlacementEvent(
 *         counterType = CounterType.PLUS_ONE_PLUS_ONE,
 *         recipient = Recipient.CreatureYouControl
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
 *         recipient = Recipient.You,
 *         source = GameObjectFilter.Any.withColor(Color.RED),
 *         damageType = DamageType.Combat
 *     )
 * )
 * ```
 */
/**
 * Extra qualifier on *why* a card is changing zones, for replacement effects (`RedirectZoneChange
 * .requiredCause`) and discard triggers (`EventPattern.DiscardEvent.cause`) that only apply to a
 * particular cause rather than to every move into the destination.
 *
 * The zone-change event itself carries the from/to zones; the cause is the piece the zones can't
 * express — "hand → graveyard" is the same move whether you discarded to hand size, paid a cost, or
 * an opponent's Mind Rot made you do it, but only the last one turns on Wilt-Leaf Liege.
 */
@Serializable
enum class ZoneChangeCause {
    /** No extra requirement — it applies however the card got there. */
    Any,

    /**
     * The move is a discard caused by a spell or ability an **opponent** of the discarding player
     * controls (Wilt-Leaf Liege, Loxodon Smiter). Excludes discarding to hand size in the cleanup
     * step (a turn-based action, not a spell or ability) and discarding to pay a cost of your own
     * spell or ability.
     */
    DiscardedByOpponentEffect,
}

@Serializable
sealed interface ReplacementEffect : TextReplaceable<ReplacementEffect> {
    /** Human-readable description of the replacement effect */
    val description: String

    /** What type of event this replacement intercepts (compositional) */
    val appliesTo: EventPattern

    /**
     * Whether this replacement effect is optional (player may decline).
     * Default false — most replacement effects are mandatory.
     * Override to true for effects like "you may draw a card instead" prompts.
     */
    val optional: Boolean get() = false

    /**
     * Priority group per CR 616.1a-f. Each sealed subtype declares its own
     * override; the [ReplacementEffectProcessor] reads this directly rather
     * than re-classifying via pattern matching.
     *
     * Default is [ReplacementPriorityGroup.ANY] (CR 616.1e).
     */
    val priorityGroup: ReplacementPriorityGroup get() = ReplacementPriorityGroup.ANY

    /**
     * The zones from which this replacement effect functions (CR 113.6).
     *
     * Default `{BATTLEFIELD}` — a permanent's abilities function only while it is on the
     * battlefield, which is every replacement effect the corpus prints except the ones that say
     * otherwise in so many words. A card whose printed line scopes itself to another zone declares
     * that zone instead: Dearly Departed's "As long as this creature is in your graveyard, each
     * Human creature you control enters with an additional +1/+1 counter on it" is
     * `activeZones = setOf(Zone.GRAVEYARD)`.
     *
     * This is the replacement-effect twin of [TriggeredAbility.activeZones], and it is read the
     * same way: the *scanner* filters by it. [com.wingedsheep.engine.handlers.effects.EntersWithReplacements]
     * sweeps the battlefield for `BATTLEFIELD` sources and every graveyard for `GRAVEYARD` ones, so
     * declaring `{GRAVEYARD}` both switches the effect **on** in the graveyard and switches it
     * **off** on the battlefield — a Dearly Departed you cast does nothing until it dies, which is
     * what the card says.
     *
     * Only the "as long as this card is in <zone>" *static* zone is expressed here. It is not a
     * duration and not a condition: the effect is live for exactly as long as the card sits in one
     * of these zones.
     */
    val activeZones: Set<Zone> get() = setOf(Zone.BATTLEFIELD)

    /**
     * Additional [Condition]s gating when this replacement applies.
     *
     * Evaluated with the **player the event affects** as `EffectContext.controllerId`, not the
     * source permanent's controller; ALL must hold. So a `Player.You` condition inside a
     * restriction reads as "the drawing/gaining/losing player". The two coincide for a
     * `Player.You` [appliesTo], which is the common case; for a `Player.EachOpponent` one they
     * do not, and a card that needs "you" to mean the source's controller has to say so with a
     * source-relative condition instead. [ReplaceLifeGainWith] and [OptionalSkipTurnWith] use the
     * source controller as `You` and expose the affected player as `TriggeringPlayer`.
     *
     * Default empty list — most replacement effects have no extra gates.
     *
     * Types that carry a `restrictions` field (e.g. [ModifyDrawAmount],
     * [PreventDamage], [DoubleDamage], [ModifyLifeGain], [ModifyLifeLoss],
     * [ModifyKeywordActionAmount], [LifeLossFloor]) override this automatically.
     */
    val restrictions: List<Condition> get() = emptyList()
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
 * [scale] says how many get added: [AdditionalTokenScale.PER_EVENT] adds
 * [additionalTokenCount] once for the whole batch (Worldwalker Helm, Quina's Frog);
 * [AdditionalTokenScale.PER_MATCHING_TOKEN] adds [additionalTokenCount] for *each* token of the
 * batch the `tokenFilter` matches — "those tokens plus that many 1/1 green Squirrel creature
 * tokens" (Chatterfang, Squirrel General). Several entries on one permanent are one ability:
 * they all judge the batch as it stood before that permanent applied, so Academy Manufactor's
 * "instead create one of each" is six entries (a Clue adds a Food and a Treasure, …) that don't
 * see each other's additions — while a *second* permanent judges the batch the first one grew
 * (two Manufactors make three of each, per the ruling).
 *
 * @property additionalTokenType The predefined token to additionally create (e.g. "Map").
 * @property additionalTokenCount How many of that token to add per qualifying event, or per
 *           matching token under [AdditionalTokenScale.PER_MATCHING_TOKEN].
 * @property scale Whether the count is added once per event or once per matching token.
 * @property inheritTapped When true, the added token enters tapped if the original
 *           creation made tapped tokens.
 * @property restrictions Extra gates on when this applies, evaluated as described on
 *           [ReplacementEffect.restrictions]. `Conditions.SourceIsSolved` puts the rider behind a
 *           Case's solved designation (CR 702.169b) — Case of the Pilfered Proof's "Solved — If one
 *           or more tokens would be created under your control, those tokens plus a Clue token are
 *           created instead", which is a Solved *static* ability in replacement-effect form and so
 *           carries its gate here rather than through `solvedStaticAbility { }`.
 */
@SerialName("CreateAdditionalToken")
@Serializable
data class CreateAdditionalToken(
    val additionalTokenType: String,
    val additionalTokenCount: Int = 1,
    val inheritTapped: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.TokenCreationEvent(),
    override val restrictions: List<Condition> = emptyList(),
    val scale: AdditionalTokenScale = AdditionalTokenScale.PER_EVENT
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, create those tokens plus ")
        when {
            scale == AdditionalTokenScale.PER_MATCHING_TOKEN ->
                append(if (additionalTokenCount == 1) "that many " else "$additionalTokenCount times that many ")
            additionalTokenCount == 1 -> append("an additional ")
            else -> append("$additionalTokenCount additional ")
        }
        append(additionalTokenType)
        append(
            if (additionalTokenCount == 1 && scale == AdditionalTokenScale.PER_EVENT) " token instead"
            else " tokens instead"
        )
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        return if (newAppliesTo !== appliesTo || newRestrictions != restrictions) {
            copy(appliesTo = newAppliesTo, restrictions = newRestrictions)
        } else this
    }
}

/** How many tokens a [CreateAdditionalToken] adds: once per creation event, or once per matching token. */
@Serializable
enum class AdditionalTokenScale {
    /** [CreateAdditionalToken.additionalTokenCount] once for the whole batch ("plus an additional Map token"). */
    PER_EVENT,

    /** [CreateAdditionalToken.additionalTokenCount] for each token of the batch the filter matches ("plus that many"). */
    PER_MATCHING_TOKEN
}

/**
 * "If one or more [filtered] tokens would be created under your control, that many [token] are
 * created instead." Substitutes a *different* token for every token of the creation event the
 * [appliesTo] filter matches — the count carries over ("that many"), the characteristics come from
 * the substitute alone. Unlike [CreateAdditionalToken] nothing of the original batch survives, and
 * unlike [ReplaceTokenCreationWithAttachedCopy] the substitute is a fixed token spec rather than a
 * copy.
 *
 * Draconic Visitor: `ReplaceTokenCreationWithToken(tokens = listOf(Effects.CreateToken(5, 5,
 * setOf(RED), setOf("Dragon"), setOf(FLYING))), appliesTo = TokenCreationEvent(You,
 * GameObjectFilter.Artifact))` — a Treasure, a Clue, an artifact creature token or a token copy of
 * an artifact all become 5/5 Dragons.
 *
 * [tokens] lists the substitutes. With one and [optional] false the substitution is mandatory and
 * asks nothing. With several, the replacement's controller picks one of them for the event ("you
 * may instead create that many 2/2 green Cat creature tokens with haste or that many 3/1 green Dog
 * creature tokens with vigilance" — Jinnie Fay, Jetmir's Second); with [optional] they may also
 * keep the original tokens. One answer covers the whole event, so a batch of a Treasure and a Clue
 * becomes two Cats or two Dogs, never one of each.
 *
 * Each substitute must be a `CreateTokenEffect` (build it with `Effects.CreateToken`); only its
 * characteristics are read. "That many" comes from the replaced event and the tokens enter under
 * the player the original tokens were being created for. Everything else the creating effect
 * specified — tapped, attacking, "sacrifice/exile it at the beginning of the next end step", and
 * any follow-up instruction that reads the created tokens — still applies to the substitutes (the
 * rulings on all of these cards). The substitute tokens are created without a second
 * count-replacement pass (a doubler already scaled "that many"). They are checked against this
 * family again (CR 616.2), skipping every substitution already applied to the event (CR 614.5):
 * Fisher's Talent chains Fish → Shark → Octopus, and a substitute that would itself match its own
 * filter can't loop.
 *
 * Read engine-side by `TokenCreationReplacementHelper.findTokenSubstitution` at the token-creation
 * executors that read the other token replacements (creature tokens, predefined tokens, token
 * copies of a target).
 */
@SerialName("ReplaceTokenCreationWithToken")
@Serializable
data class ReplaceTokenCreationWithToken(
    val tokens: List<Effect>,
    override val appliesTo: EventPattern = EventPattern.TokenCreationEvent(),
    /** "You may instead …": the replacement's controller may decline and keep the original tokens. */
    override val optional: Boolean = false
) : ReplacementEffect {
    init {
        require(tokens.isNotEmpty()) { "ReplaceTokenCreationWithToken needs at least one substitute token" }
        tokens.forEach { token ->
            require(token is com.wingedsheep.sdk.scripting.effects.CreateTokenEffect) {
                "ReplaceTokenCreationWithToken.tokens must be CreateTokenEffects (Effects.CreateToken), was ${token::class.simpleName}"
            }
        }
    }

    /** Whether applying this asks its controller anything (a "may", or a pick among substitutes). */
    val asksForChoice: Boolean get() = optional || tokens.size > 1

    override val description: String = buildString {
        append("If ${appliesTo.description}, ")
        if (optional) append("you may instead create ")
        append(tokens.joinToString(" or ") {
            "that many " + tokenNounPhrase(it as com.wingedsheep.sdk.scripting.effects.CreateTokenEffect)
        })
        if (!optional) append(" are created instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newTokens = tokens.map { it.applyTextReplacement(replacer) }
        return if (newAppliesTo !== appliesTo || newTokens.indices.any { newTokens[it] !== tokens[it] }) {
            copy(tokens = newTokens, appliesTo = newAppliesTo)
        } else this
    }

    companion object {
        /** "5/5 red Dragon creature tokens with flying" — the plural noun phrase of [effect]. */
        fun tokenNounPhrase(effect: com.wingedsheep.sdk.scripting.effects.CreateTokenEffect): String = buildString {
            append("${effect.power}/${effect.toughness} ")
            if (effect.colors.isNotEmpty()) {
                append(effect.colors.joinToString(" and ") { it.displayName.lowercase() })
                append(" ")
            }
            append(effect.creatureTypes.joinToString(" "))
            if (effect.artifactToken) append(" artifact")
            append(" creature tokens")
            if (effect.keywords.isNotEmpty()) {
                append(" with ")
                append(effect.keywords.joinToString(", ") { it.name.lowercase() })
            }
        }
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
 * @param effectsOnly When true, only placements made by an *effect* are doubled — "If **an effect**
 *                    would put one or more counters…" (Doubling Season). Counters put on as a
 *                    cost (a [+1] loyalty cost, blight, "put a counter on this: …") and by a
 *                    turn-based action (a Saga's precombat-main lore counter) are not effects, so
 *                    they pass through unchanged. When false (the default), every placement counts
 *                    — "If one or more counters **would be put**" (Corpsejack Menace) and "If **you**
 *                    would put" (Innkeeper's Talent) both see costs too.
 */
@SerialName("DoubleCounterPlacement")
@Serializable
data class DoubleCounterPlacement(
    val placedByYou: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.CounterPlacementEvent(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        recipient = Recipient.CreatureYouControl
    ),
    val effectsOnly: Boolean = false
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
 *
 * @param placedByYou When true, only applies when the controller of this effect is the player
 *                    putting the counters — "**If you** would put one or more counters on a
 *                    permanent you control" (Doc Samson, Super-Psychiatrist). When false (the
 *                    default), applies regardless of who is placing them, so the recipient filter
 *                    on [appliesTo] is the sole "you control" gate — the Hardened Scales /
 *                    Winding Constrictor reading, where an opponent's proliferate also feeds it.
 *                    Same axis as [DoubleCounterPlacement.placedByYou].
 */
@SerialName("ModifyCounterPlacement")
@Serializable
data class ModifyCounterPlacement(
    val modifier: Int = 1,
    override val appliesTo: EventPattern = EventPattern.CounterPlacementEvent(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        recipient = Recipient.CreatureYouControl
    ),
    val placedByYou: Boolean = false
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

/**
 * Cap a counter placement at [amount] and lock the recipient out of further counters of that
 * kind for the rest of the turn — "If you would get one or more poison counters, instead you get
 * one poison counter and you can't get additional poison counters this turn" (Melira, the Living
 * Cure).
 *
 * Applied after the additive/multiplying placement replacements, so the capped recipient ends up
 * with at most [amount] (the order the affected player would normally pick, per Melira's rulings).
 * The lock is the replacement's *result*, not a continuous effect of its source: once applied it
 * holds until end of turn even if the source leaves, and every later placement of that kind on
 * that recipient this turn simply doesn't happen. Only placements on a **player** recipient take
 * the lock today — the one printed use; [appliesTo] is the usual
 * [EventPattern.CounterPlacementEvent] (kind + [Recipient]).
 */
@SerialName("CapCounterPlacementThisTurn")
@Serializable
data class CapCounterPlacementThisTurn(
    val amount: Int = 1,
    override val appliesTo: EventPattern = EventPattern.CounterPlacementEvent(
        counterType = CounterType.POISON,
        recipient = Recipient.You
    )
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, instead $amount is placed and no more can be placed this turn"

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
 *
 * [requiredCause] narrows the replacement to a particular *reason* for the move on top of the
 * from/to zones. `DiscardedByOpponentEffect` + `selfOnly` + `newDestination = Zone.BATTLEFIELD` is
 * Wilt-Leaf Liege / Loxodon Smiter: "if a spell or ability an opponent controls causes you to
 * discard this card, put it onto the battlefield instead of putting it into your graveyard".
 */
@SerialName("RedirectZoneChange")
@Serializable
data class RedirectZoneChange(
    val newDestination: Zone,
    override val appliesTo: EventPattern,
    val linkToSource: Boolean = false,
    val selfOnly: Boolean = false,
    val shuffleIntoLibrary: Boolean = false,
    val reveal: Boolean = false,
    val requiredCause: ZoneChangeCause = ZoneChangeCause.Any
) : ReplacementEffect {
    override val description: String = buildString {
        when (requiredCause) {
            ZoneChangeCause.Any -> append("If ${appliesTo.description}, ")
            ZoneChangeCause.DiscardedByOpponentEffect ->
                append("If a spell or ability an opponent controls causes you to discard this card, ")
        }
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
data class OnEnterRun(
    val effect: Effect,
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
 *
 * @property condition Optional gate evaluated against the replacement *source* at the moment a
 *   permanent would enter — the same axis [RedirectDamage] and [EntersWithCounters] carry. When
 *   non-null the tap applies only while the condition holds, which is how a source whose effect
 *   depends on a choice it made as it entered spells the clause (Ashling's Prerogative:
 *   `SourceChosenModeIs("odd")` on the even-mana-value half and vice versa). `null` = always.
 */
@SerialName("PermanentsEnterTapped")
@Serializable
data class PermanentsEnterTapped(
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    ),
    val condition: Condition? = null
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, it enters tapped")
        condition?.let { append(" (${it.description})") }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newCondition = condition?.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newCondition !== condition)
            copy(appliesTo = newAppliesTo, condition = newCondition) else this
    }
}

/**
 * "If [putter] would put a permanent matching [appliesTo] onto the battlefield, that player instead
 * puts it onto the battlefield, then [effect]" — an entry replacement whose rest *does* something
 * once the permanent is there (CR 614.1a). Land Equilibrium: "If an opponent who controls at least
 * as many lands as you do would put a land onto the battlefield, that player instead puts that land
 * onto the battlefield then sacrifices a land of their choice."
 *
 * The permanent still enters — its enter triggers see it, and the player may pick it for the rider
 * — but the rider runs as part of the same replacement, before anyone gets priority or can tap the
 * new permanent: the engine queues it as a pending replacement rider that drains at the end of the
 * putting effect (or at the settle boundary for a land play), ahead of state-based actions and
 * trigger detection. Applies however the permanent would enter — played, put by a spell or
 * ability, or created as a token (CR 701.7a: to create a token is to put it onto the battlefield).
 *
 * Closest existing types, and why neither takes the axis: [OnEnterRun] runs an effect *as its own
 * source* enters (a self-replacement read off the entering card's definition, never consulted for
 * other permanents); [PermanentsEnterTapped] is consulted from the battlefield for other entering
 * permanents but only ever taps them — it has no effect to run and no player who put it.
 *
 * @property effect the rest of the replacement. In it "that player" is
 *   [com.wingedsheep.sdk.scripting.references.Player.TriggeringPlayer] — the player who put the
 *   permanent onto the battlefield (not necessarily its controller: the Land Equilibrium ruling) —
 *   and "you" is this source's controller.
 * @property putter which player's entries this replaces, relative to this source's controller
 *   ([Player.EachOpponent], [Player.You], [Player.Each]). The player who puts the permanent is the
 *   one playing it, or the controller of the spell or ability putting it there.
 * @property condition optional gate, evaluated as the permanent *would* enter — against the board
 *   without it — with [Player.TriggeringPlayer] bound to the putting player.
 */
@SerialName("PutOntoBattlefieldThen")
@Serializable
data class PutOntoBattlefieldThen(
    val effect: Effect,
    val putter: Player = Player.Each,
    val condition: Condition? = null,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    init {
        require(putter == Player.You || putter == Player.EachOpponent || putter == Player.Each || putter == Player.Any) {
            "PutOntoBattlefieldThen.putter is a relation to this source's controller: You, EachOpponent, Each or Any"
        }
    }

    override val description: String = buildString {
        append("If ")
        append(
            when (putter) {
                Player.EachOpponent -> "an opponent"
                Player.You -> "you"
                else -> "a player"
            }
        )
        condition?.let { append(" (${it.description})") }
        append(" would put ${(appliesTo as? EventPattern.ZoneChangeEvent)?.filter?.description ?: "a permanent"} onto the battlefield, ")
        append("that player instead puts it onto the battlefield, then ${effect.description.replaceFirstChar { it.lowercase() }}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newEffect = effect.applyTextReplacement(replacer)
        val newCondition = condition?.applyTextReplacement(replacer)
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newEffect !== effect || newCondition !== condition || newAppliesTo !== appliesTo)
            copy(effect = newEffect, condition = newCondition, appliesTo = newAppliesTo)
        else this
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
 * @param otherOnly When true the source is excluded — "each **other** [filter] … enters with an
 *                  additional counter" (Metallic Mimic). The [selfOnly] mirror, and the same flag
 *                  [EntersWithDynamicCounters] carries: the source's own entry path skips an
 *                  `otherOnly` effect, so the source can never counter itself as it enters. Leave
 *                  false for a group effect that also covers the source's own entry.
 */
@SerialName("EntersWithCounters")
@Serializable
data class EntersWithCounters(
    val counterType: CounterType = CounterType.PLUS_ONE_PLUS_ONE,
    val count: Int,
    val selfOnly: Boolean = false,
    val condition: Condition? = null,
    val otherOnly: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, it enters with $count ${counterType.printed} counters")
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
    val counterType: CounterType = CounterType.PLUS_ONE_PLUS_ONE,
    val count: DynamicAmount,
    val otherOnly: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    ),
    override val activeZones: Set<Zone> = setOf(Zone.BATTLEFIELD),
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, it enters with ${count.description} ${counterType.printed} counters"

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
 *
 * [onPrevented] is what the prevention effect does with the damage it prevented — the Lorwyn
 * Incarnations' second sentence: Purity's "You gain life equal to the damage prevented this way",
 * Vigor's "Put a +1/+1 counter on that creature for each 1 damage prevented this way", Hostility's
 * tokens. It is part of the same prevention effect (CR 615.5), not a trigger: it never uses the stack, runs before
 * state-based actions, and runs once per application with the amount *that* application actually
 * prevented, read by [com.wingedsheep.sdk.dsl.DynamicAmounts.preventedDamage]. `Self` is this
 * permanent, "you" its controller, and [com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity]
 * the permanent the damage would have been dealt to. Damage that can't be prevented is dealt, and
 * then nothing was prevented, so the rider doesn't run.
 */
@SerialName("PreventDamage")
@Serializable
data class PreventDamage(
    val amount: Int? = null,  // null = prevent all
    override val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern,
    val onPrevented: Effect? = null
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
        onPrevented?.let { append(". ${it.description.replaceFirstChar { c -> c.uppercase() }}") }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val newOnPrevented = onPrevented?.applyTextReplacement(replacer)
        val anyChanged = newAppliesTo !== appliesTo || newOnPrevented !== onPrevented ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) {
            copy(appliesTo = newAppliesTo, restrictions = newRestrictions, onPrevented = newOnPrevented)
        } else this
    }
}

/**
 * How many counters a [PreventDamageByRemovingCounter] spends on one damage event.
 *
 * Two values because Oracle prints exactly two rules, and the difference is not a number the card
 * names — it is which of the two it means. A count would be a third thing neither card says.
 */
@Serializable
enum class CounterRemovalAmount {
    /** One counter per damage event, however large the damage (Unbreathing Horde, shield counters). */
    One,

    /**
     * As many counters as the damage would have dealt, bounded by the counters present — "prevent
     * that damage and remove **that many** counters from it" (Magma Pummeler). Damage beyond the
     * counter count is still prevented in full; the excess simply has nothing left to remove.
     */
    EqualToDamage
}

/**
 * Prevent damage that would be dealt to this permanent and remove one counter of [counterType]
 * from it — the printed twin of the shield counter's prevention half (CR 122.1c).
 *
 * Models Unbreathing Horde: "If this creature would be dealt damage, prevent that damage and
 * remove a +1/+1 counter from it."
 *
 * Distinct from `PreventDamage` rather than a flag on it, because the counter removal is not a
 * parameter of the prevention — it is what the ability *is*, and it makes the effect need a
 * state-returning application path where plain prevention is a pure arithmetic reduction.
 *
 * Two rules the printed rulings pin down, both defaults here and both shared with shield counters:
 *
 * - **Exactly one counter per damage event**, however large the damage and however many counters
 *   are on the permanent. A creature blocking two attackers is dealt combat damage once (CR 510.2),
 *   so it spends one counter and prevents all of it; the first-strike and regular damage steps are
 *   separate events and each cost a counter.
 * - **The prevention does not depend on having a counter.** With no counters left the damage is
 *   still prevented — there is simply nothing to remove. (Unbreathing Horde only survives that way
 *   while something else is holding its toughness above 0.)
 *
 * Both are *defaults*, not the whole family. [removalAmount] and [requiresCounter] are the two axes
 * on which **Magma Pummeler** inverts them: "If damage would be dealt to this creature **while it
 * has a +1/+1 counter on it**, prevent that damage and remove **that many** +1/+1 counters from
 * it." Leave both alone for the Unbreathing Horde shape.
 *
 * [appliesTo] defaults to "any damage that would be dealt to this permanent"; narrow its
 * `damageType`, `source` or `amount` for a card that only shields part of the picture.
 */
@SerialName("PreventDamageByRemovingCounter")
@Serializable
data class PreventDamageByRemovingCounter(
    val counterType: CounterType = CounterType.PLUS_ONE_PLUS_ONE,
    /**
     * How many counters the prevention spends. [CounterRemovalAmount.One] is the printed default
     * (Unbreathing Horde, shield counters); [CounterRemovalAmount.EqualToDamage] is Magma
     * Pummeler's "remove **that many**", bounded by the counters actually present — damage above
     * the count is still prevented in full, and every counter goes (the printed ruling).
     */
    val removalAmount: CounterRemovalAmount = CounterRemovalAmount.One,
    /**
     * When true the ability only applies *while the permanent has a counter of [counterType]* —
     * Magma Pummeler's "while it has a +1/+1 counter on it". With no counter the replacement does
     * not fire at all and the damage is dealt normally. The default `false` is the printed
     * Unbreathing Horde rule, where the prevention happens regardless.
     */
    val requiresCounter: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.DamageEvent(
        recipient = Recipient.Self
    ),
    /**
     * The rest of the replacement after the removal — Bloatfly Swarm's "…, then give each player
     * a rad counter for each +1/+1 counter removed this way". Part of the same replacement, so it
     * is a replacement rider (no stack, no priority), run once per application that removed at
     * least one counter, with the number removed readable as
     * `DynamicAmounts.triggerCountersRemoved()`. It runs even when the damage can't be prevented,
     * because the counters are still removed (the Bloatfly ruling).
     */
    val onRemoved: Effect? = null
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}")
        if (requiresCounter) append(" while it has a ${counterType.printed} counter on it")
        append(", prevent that damage")
        append(if (onRemoved != null) ", remove " else " and remove ")
        append(
            when (removalAmount) {
                CounterRemovalAmount.One -> "a ${counterType.printed} counter"
                CounterRemovalAmount.EqualToDamage -> "that many ${counterType.printed} counters"
            }
        )
        append(" from it")
        onRemoved?.let { append(", then ${it.description}") }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newOnRemoved = onRemoved?.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newOnRemoved !== onRemoved)
            copy(appliesTo = newAppliesTo, onRemoved = newOnRemoved) else this
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
 *
 * [multiplier] is the factor the damage is scaled by — 2 for "double" (the default), 3 for
 * "triple" (City on Fire). The type keeps its name because doubling is the family's common case
 * and every consumer (the amplification pass, the client badges) treats any factor alike.
 */
@SerialName("DoubleDamage")
@Serializable
data class DoubleDamage(
    override val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern,
    val multiplier: Int = 2,
) : ReplacementEffect {
    init {
        require(multiplier >= 2) { "DoubleDamage.multiplier must be at least 2, was $multiplier" }
    }

    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(appliesTo.description)
        append(", it deals ${multiplierWord(multiplier)} that damage instead")
    }

    private fun multiplierWord(factor: Int): String = when (factor) {
        2 -> "double"
        3 -> "triple"
        else -> "$factor times"
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
 * Halve damage dealt, **rounded down** — the dividing mirror of [DoubleDamage].
 *
 * Ghosts of the Innocent: *"If a source would deal damage to a permanent or player, it deals half
 * that damage, rounded down, to that permanent or player instead."* →
 * `HalveDamage(appliesTo = EventPattern.DamageEvent(recipient = Recipient.Any))`.
 *
 * Modelled as its own type rather than a [ModifyDamageAmount] with a negative modifier because the
 * reduction is *multiplicative*: it scales with the incoming amount, which no `DynamicAmount` can
 * read. Its rulings follow from that:
 *
 * - Half of 1 rounded down is 0, so a 1-damage source deals no damage at all.
 * - Each applicable `HalveDamage` applies once (CR 616.1) and they compound: three of them turn
 *   14 into 7, then 3, then 1.
 * - It is **not** a prevention effect, so [DamageCantBePrevented] (Excruciator) does not switch it
 *   off, and prevention shields are not consumed by it.
 *
 * [restrictions] gates the halving on further conditions, mirroring [PreventDamage.restrictions] /
 * [DoubleDamage.restrictions]; each entry is evaluated against the **replacement source's
 * controller**, as everywhere else in the damage family.
 */
@SerialName("HalveDamage")
@Serializable
data class HalveDamage(
    override val restrictions: List<Condition> = emptyList(),
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
        append(", it deals half that damage, rounded down, instead")
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
 *   `ModifyDamageAmount(dynamicModifier = DynamicAmounts.countersOnSelf(CounterType.FIRE),
 *                       appliesTo = DamageEvent(source = GameObjectFilter.Any.youControl(),
 *                                               recipient = Recipient.OpponentOrPermanentTheyControl))`.
 *
 * When [dynamicModifier] is non-null it is evaluated with the replacement's source
 * permanent as the resolution source (so `DynamicAmount.EntityProperty(Self, …)` reads
 * the source's own characteristics/counters); otherwise the flat [modifier] is added.
 *
 * The optional [restrictions] list gates the bonus on further conditions, mirroring
 * [PreventDamage.restrictions] / [DoubleDamage.restrictions]. Like the rest of the damage family —
 * and unlike the draw/life-total replacements, whose restrictions read the *affected* player — each
 * entry is evaluated against the **replacement source's controller**, so a `Player.You` condition
 * reads as "the controller of the permanent with this ability". That's what lets Far Fortune, End
 * Boss's "Max speed — …" rider gate on *your* speed while the damage lands on an opponent.
 */
@SerialName("ModifyDamageAmount")
@Serializable
data class ModifyDamageAmount(
    val modifier: Int = 0,
    val dynamicModifier: DynamicAmount? = null,
    override val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String = buildString {
        val bonus = dynamicModifier?.description ?: "$modifier"
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(appliesTo.description)
        append(", it deals that much damage plus $bonus instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newDynamic = dynamicModifier?.applyTextReplacement(replacer)
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo || newDynamic !== dynamicModifier ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged)
            copy(appliesTo = newAppliesTo, dynamicModifier = newDynamic, restrictions = newRestrictions)
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
 *   recipient = Opponent, source = red you-control, damageType = NonCombat))`.
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

/**
 * The damage is still dealt in full, but as part of the same replacement all *other* damage
 * already marked on the recipient is **healed** (CR 701.69a: "If an effect states that damage
 * already dealt to a permanent 'is healed,' that permanent's controller removes all marked damage
 * from that permanent").
 *
 * Wolverine, Fierce Fighter: "If damage would be dealt to Wolverine, instead that damage is dealt,
 * but all other damage already dealt to him is healed." →
 * `HealOtherDamage(appliesTo = DamageEvent(recipient = Recipient.Self))`.
 *
 * Unlike every other member of this family the *amount* is untouched — this is the one damage
 * replacement whose whole job is a side effect on the recipient's already-marked damage, which is
 * why it can't be expressed as [PreventDamage] (which subtracts), [CapDamage] (which clamps), or
 * [ReplaceDamageWithCounters] (which swaps the damage for something else). The observable result is
 * that marked damage never accumulates across separate damage events: the recipient effectively
 * only ever has the most recent event's damage on it.
 *
 * Because it heals only damage dealt *before* this event, the engine applies it **once per
 * damage event**, not once per instance: all combat damage in a step is dealt simultaneously
 * (CR 510.2), so a creature blocked by two attackers keeps both attackers' damage and heals only
 * what was marked before the step. The first-strike and regular combat damage steps are separate
 * events, so each heals in turn.
 *
 * Healing removes marked damage only; -1/-1 counters from a wither/infect source are not marked
 * damage (CR 120.3d) and survive, though a wither source dealing damage still triggers the heal.
 * Recipients that don't mark damage (players, planeswalkers, battles) have nothing to heal, so the
 * replacement is a no-op on them.
 */
@SerialName("HealOtherDamage")
@Serializable
data class HealOtherDamage(
    override val appliesTo: EventPattern = EventPattern.DamageEvent(recipient = Recipient.Self)
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, instead that damage is dealt, but all other damage already " +
            "dealt to it is healed"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

// =============================================================================
// Draw Replacement Effects
// =============================================================================

/**
 * Modify the number of cards a draw event draws — `(count * multiplier) + modifier`, clamped
 * to ≥ 0 — optionally gated by additional [restrictions]. Applied at the call site where the
 * original draw count is announced (spell/ability resolution and the draw step), so the
 * modification fires once per draw instruction (CR 121.2a: "An instruction to draw multiple
 * cards can be modified by replacement effects that refer to the number of cards drawn. This
 * modification occurs before considering any of the individual card draws.") and is not
 * re-applied when a paused per-card draw loop resumes. CR 616.1g is what makes that two-level
 * split legal: the announced draw *contains* the individual draws, and a replacement applying
 * to the contained event can't be chosen until the containing one has been.
 *
 * Two independent knobs so one type covers the whole family: [multiplier] for the doubling
 * wording ("if you would draw a card, draw two cards instead" — which per the Vnwxt rulings
 * multiplies the *announced* count, so a "draw three cards" spell draws six) and [modifier]
 * for the additive wording ("you draw that many cards plus one instead"). Multiple such
 * effects are cumulative: two doublers quadruple the draw, matching the Vnwxt ruling about
 * controlling both Vnwxt and Thought Reflection.
 *
 * Each entry in [restrictions] is a [Condition] evaluated against the drawing player as
 * the controller context; the modification only applies when ALL restrictions hold. This
 * mirrors [ModifyLifeLoss]'s shape — use it for cards whose extra-draw clause is gated by
 * arbitrary additional conditions, including a "Max speed —" gate. Note that "you" in
 * restriction text reads as the drawing player, not the source's controller — for
 * `DrawEvent(player = Player.You)` they're the same, but a future
 * `DrawEvent(player = Player.EachOpponent)` card whose restriction means "you" = source
 * controller would need a source-relative condition instead.
 *
 * [appliesTo] is deliberately typed as [EventPattern.DrawCardsEvent] rather than the general
 * [EventPattern], so the announcement-only contract above is a compile error to violate rather
 * than a runtime surprise. The per-card [EventPattern.DrawEvent] does not terminate for this
 * type: modifying a draw count without drawing a card leaves the game state unchanged, so the
 * draw loop would re-check, re-match and re-apply forever. Use [ReplaceDrawWith] for a
 * genuinely per-card replacement.
 *
 * Examples:
 * - Quantum Riddler ("As long as you have one or fewer cards in hand, if you would draw
 *   one or more cards, you draw that many cards plus one instead"):
 *     `ModifyDrawAmount(modifier = 1,
 *                       restrictions = listOf(Conditions.CardsInHandAtMost(1)),
 *                       appliesTo = DrawCardsEvent(player = Player.You))`
 * - Vnwxt, Verbose Host ("Max speed — If you would draw a card, draw two cards instead"):
 *     `ModifyDrawAmount(multiplier = 2,
 *                       restrictions = listOf(Conditions.YouHaveMaxSpeed),
 *                       appliesTo = DrawCardsEvent(player = Player.You))`
 *
 * @param multiplier Factor the announced draw count is multiplied by. `2` is the "draw twice
 *        that many instead" wording; the default `1` leaves the count alone.
 * @param modifier Flat amount added after multiplying. Negative values reduce the draw
 *        (clamped to ≥ 0 by the caller).
 * @param restrictions Additional [Condition]s gating when the modification applies. Evaluated
 *        against the drawing player as controller; ALL must hold.
 * @param appliesTo Which announced draws are affected — the drawing player relative to the
 *        source's controller, and the threshold count.
 */
@SerialName("ModifyDrawAmount")
@Serializable
data class ModifyDrawAmount(
    val modifier: Int = 0,
    val multiplier: Int = 1,
    override val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern.DrawCardsEvent = EventPattern.DrawCardsEvent()
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
        // Pick the natural English for each shape rather than spelling out the formula.
        append(
            when {
                multiplier != 1 && modifier != 0 ->
                    ", they draw that many cards times $multiplier plus $modifier instead"
                multiplier == 2 -> ", they draw twice that many cards instead"
                multiplier != 1 -> ", they draw that many cards times $multiplier instead"
                else -> ", they draw that many cards plus $modifier instead"
            }
        )
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer) as? EventPattern.DrawCardsEvent ?: appliesTo
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newAppliesTo !== appliesTo ||
            newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(appliesTo = newAppliesTo, restrictions = newRestrictions) else this
    }
}

/**
 * Modify how many cards a player mills, scries or surveils — "mill that many cards plus four
 * instead", "scry that many cards plus one instead", "look at an additional two cards each time you
 * surveil". Additive: a [modifier] of `+N` turns the announced count C into `C + N`; negative values
 * reduce it (clamped to ≥ 0 by the caller). One type across these keyword actions rather than one
 * per action, like [ModifyKeywordAction]: [appliesTo] says which action and whose —
 *
 *  - [EventPattern.MillEvent] — mill (CR 701.17). Applied at the mill announcement: the mill
 *    pipeline's gather and the mill cost atom.
 *  - [EventPattern.ScryEvent] — scry (CR 701.22). Applied at the scry pipeline's gather, so the
 *    bigger look carries through to the top/bottom choice and the `ScriedEvent` count.
 *  - [EventPattern.SurveilEvent] — surveil (CR 701.25). Applied at the surveil pipeline's gather,
 *    so the extra cards are part of what is surveiled (CR 701.25b): each may go to the graveyard or
 *    back on top, and the `SurveiledEvent` count includes them.
 *
 * Each pattern's `player` gates whose action is affected relative to the source's controller
 * (`Player.You` / `Player.EachOpponent` / `Player.Each`). Applied once per instruction, so a
 * paused-and-resumed pipeline never double-modifies; a count of 0 is no event (CR 701.22b /
 * 701.25c) and is never modified; multiple instances sum. [restrictions] are additional
 * [Condition]s evaluated against the acting player as controller; ALL must hold.
 *
 * Why not [ModifyDrawAmount]: a draw is not a keyword action and has its own announcement site
 * (`DrawReplacementDispatcher`), with "N or more" draw gating these actions don't have.
 *
 * Examples — The Water Crystal: `ModifyKeywordActionAmount(MillEvent(Player.EachOpponent), 4)`;
 * Kenessos, Priest of Thassa: `ModifyKeywordActionAmount(ScryEvent(), 1)`; Enhanced Surveillance:
 * `ModifyKeywordActionAmount(SurveilEvent(), 2)`.
 */
@SerialName("ModifyKeywordActionAmount")
@Serializable
data class ModifyKeywordActionAmount(
    override val appliesTo: EventPattern.KeywordActionCountEvent,
    val modifier: Int,
    override val restrictions: List<Condition> = emptyList()
) : ReplacementEffect {

    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) {
            append(restrictionDesc.replaceFirstChar { it.uppercase() })
            append(", if ")
        } else {
            append("If ")
        }
        append(
            when (appliesTo) {
                is EventPattern.MillEvent -> "${appliesTo.player.description} would mill one or more cards"
                else -> appliesTo.description
            }
        )
        val verb = when (appliesTo) {
            is EventPattern.MillEvent -> "mill"
            is EventPattern.ScryEvent -> "scry"
            is EventPattern.SurveilEvent -> "surveil"
        }
        append(", they $verb that many cards plus $modifier instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        // A keyword-action pattern carries only a Player — no replaceable text.
        val newRestrictions = restrictions.map { it.applyTextReplacement(replacer) }
        val anyChanged = newRestrictions.zip(restrictions).any { (n, o) -> n !== o }
        return if (anyChanged) copy(restrictions = newRestrictions) else this
    }
}

/**
 * Replace drawing with another effect.
 * Example: Underrealm Lich (look at 3, put 1 in hand, rest in graveyard)
 */
@SerialName("ReplaceDrawWith")
@Serializable
data class ReplaceDrawWith(
    val replacementEffect: Effect,
    override val optional: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.DrawEvent(),
    override val restrictions: List<Condition> = emptyList()
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}")
        if (restrictions.isNotEmpty()) {
            val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
            append(" while $restrictionDesc")
        }
        append(", ")
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
 * Insert an extra effect *in front of* a keyword action (CR 614). Replaces "[a permanent matching
 * [appliesTo]'s filter] <acts>" with "[prefixEffect] happens, then that permanent <acts>".
 *
 * This is the printed "If a permanent you control would X, instead <something>, then that permanent
 * Xs" shape. It is one type across keyword actions rather than one per action, because the printed
 * cards differ only in *which* action and *what* the prefix is — [appliesTo] carries the action
 * (and its subject filter), [prefixEffect] carries the rest:
 *
 *  - Twists and Turns — `ModifyKeywordAction(Effects.Scry(1), ExploredEvent(Creature.youControl()))`
 *    "If a creature you control would explore, instead you scry 1, then that creature explores."
 *  - Leader, Super-Genius — `ModifyKeywordAction(Effects.DrawCards(1), ConnivedEvent(Creature.youControl()))`
 *    "If a creature you control would connive, instead you draw a card, then that creature connives."
 *
 * Supported [appliesTo] patterns: [EventPattern.ExploredEvent] (CR 701.44) and
 * [EventPattern.ConnivedEvent] (CR 701.50). Any other pattern never matches — the two executors
 * below are the only consumers.
 *
 * Modeled on [ReplaceDrawWith]: like draw replacement, neither explore nor connive is
 * dispatched as a generic replaceable event, so `ExploreEffectExecutor` / `ConniveEffectExecutor`
 * consult this directly at action time. On a match the executor re-issues the action as
 * `Composite([prefixEffect], <action>(sameCreature, replacementsApplied = true))`, reusing the
 * composite executor's pause-sequencing so a prefix that pauses (Scry's top/bottom decision) or the
 * action's own decision (connive's discard) resolves fully and in the printed order. The
 * `replacementsApplied` flag is what stops a replacement from applying to its own re-issue
 * (CR 614.5).
 *
 * [appliesTo]'s filter scopes *which* actions are modified and is evaluated with the replacement
 * source's controller as "you", so "a creature you control would connive" only fires for that
 * player's creatures. Note the [prefixEffect] itself runs in the *replaced action's* context, not a
 * fresh one rooted at this source — with the usual "creature you control" filter those controllers
 * are the same player, but an opponent's effect that makes your creature connive would run the
 * prefix as the opponent. Pre-existing behavior inherited from the explore case; no printed card
 * in either set distinguishes them today.
 */
@SerialName("ModifyKeywordAction")
@Serializable
data class ModifyKeywordAction(
    val prefixEffect: Effect,
    override val appliesTo: EventPattern
) : ReplacementEffect {
    // The verb comes from the action, so both printed cards read the way they are printed:
    // "…then it explores" / "…then it connives", not a generic "then it does".
    override val description: String =
        "If ${appliesTo.description}, first ${prefixEffect.description}, then it " +
            when (appliesTo) {
                is EventPattern.ExploredEvent -> "explores"
                is EventPattern.ConnivedEvent -> "connives"
                else -> "does so"
            }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newPrefix = prefixEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newPrefix !== prefixEffect)
            copy(appliesTo = newAppliesTo, prefixEffect = newPrefix)
        else this
    }
}

/**
 * Perform a keyword action (CR 701) [times] times instead of once (CR 614.1a) — "If you would
 * proliferate, proliferate twice instead." The sibling of [ModifyKeywordAction]: that one puts an
 * extra effect in front of the action, this one repeats the action itself.
 *
 *  - Tekuthal, Inquiry Dominus — `RepeatKeywordAction(appliesTo = EventPattern.ProliferatedEvent())`
 *
 * Supported [appliesTo] patterns: [EventPattern.ProliferatedEvent] (CR 701.34), whose `player` is
 * matched against the replacement source's controller as "you". Any other pattern never matches.
 * Only the untargeted form of proliferate is proliferating — the targeted "another counter of each
 * kind on target …" form (Powerful Broker) is not, and is never repeated.
 *
 * Each repetition is a complete proliferate of its own: the recipients are chosen again, after the
 * previous one has placed its counters (so a permanent that just got its first counter is now
 * eligible), and each emits its own "you proliferated" event, so "whenever you proliferate"
 * triggers once per repetition. Several applicable instances multiply — two Tekuthals make one
 * proliferate into four, the second replacement applying to each of the two proliferates the
 * first one produced.
 */
@SerialName("RepeatKeywordAction")
@Serializable
data class RepeatKeywordAction(
    val times: Int = 2,
    override val appliesTo: EventPattern
) : ReplacementEffect {
    init {
        require(times >= 2) { "RepeatKeywordAction.times must be at least 2, was $times" }
    }

    override val description: String
        get() {
            val count = when (times) {
                2 -> "twice"
                3 -> "three times"
                else -> "$times times"
            }
            return when (val pattern = appliesTo) {
                is EventPattern.ProliferatedEvent ->
                    "If ${pattern.player.description} would proliferate, proliferate $count instead"
                else -> "If ${appliesTo.description}, it happens $count instead"
            }
        }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
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

/** Replace a life-gain event with an effect, without gaining life or using the stack. */
@SerialName("ReplaceLifeGainWith")
@Serializable
data class ReplaceLifeGainWith(
    val replacementEffect: Effect,
    override val appliesTo: EventPattern.LifeGainEvent = EventPattern.LifeGainEvent(),
    override val restrictions: List<Condition> = emptyList()
) : ReplacementEffect {
    override val description: String = "If ${appliesTo.description}, instead ${replacementEffect.description}"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val pattern = appliesTo.applyTextReplacement(replacer) as EventPattern.LifeGainEvent
        val effect = replacementEffect.applyTextReplacement(replacer)
        val conditions = restrictions.map { it.applyTextReplacement(replacer) }
        return if (pattern !== appliesTo || effect !== replacementEffect || conditions != restrictions)
            copy(replacementEffect = effect, appliesTo = pattern, restrictions = conditions)
        else this
    }

    companion object {
        const val AMOUNT = "replacementLifeGainAmount"
    }
}

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
    override val restrictions: List<Condition> = emptyList()
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
 *        Evaluated against the player the event affects; ALL must hold.
 */
@SerialName("ModifyLifeLoss")
@Serializable
data class ModifyLifeLoss(
    val multiplier: Int = 1,
    val modifier: Int = 0,
    override val restrictions: List<Condition> = emptyList(),
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
 *        Evaluated against the player the event affects; ALL must hold.
 * @param appliesTo Life-loss event filter (which player is protected).
 */
@SerialName("LifeLossFloor")
@Serializable
data class LifeLossFloor(
    val floor: Int = 1,
    override val restrictions: List<Condition> = emptyList(),
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

/**
 * Life loss of one [origin] becomes life gain: "You gain life rather than lose life from radiation"
 * (Strong, the Brutish Thespian). The [appliesTo]-matching player gains that much life instead of
 * losing it; the gain is an ordinary life-gain event, so life-gain modifiers, "can't gain life" and
 * "whenever you gain life" triggers all see it. Anything else the losing effect does still happens
 * (radiation still removes its rad counters).
 *
 * When `ModifyLifeLoss` replacements also apply, the affected player orders them (CR 616.1) and
 * the engine takes the order that gains the most life: every increase applies before the
 * conversion, and every reduction is left with no loss to modify.
 *
 * Scope: non-damage life loss from a `LoseLife` effect of the given [origin]
 * (`LifeLossOrigin.RADIATION` is the inherent radiation trigger's). Damage-caused loss and life
 * payments are never converted.
 */
@SerialName("GainLifeInsteadOfLosingLife")
@Serializable
data class GainLifeInsteadOfLosingLife(
    val origin: com.wingedsheep.sdk.scripting.effects.LifeLossOrigin,
    override val restrictions: List<Condition> = emptyList(),
    override val appliesTo: EventPattern = EventPattern.LifeLossEvent(Player.You)
) : ReplacementEffect {
    override val description: String = buildString {
        val restrictionDesc = restrictions.joinToString(" and ") { it.description.removePrefix("if ") }
        if (restrictionDesc.isNotEmpty()) append(restrictionDesc.replaceFirstChar { it.uppercase() }).append(", ")
        val who = when ((appliesTo as? EventPattern.LifeLossEvent)?.player) {
            Player.You -> "you gain"
            Player.EachOpponent -> "each opponent gains"
            else -> "each player gains"
        }
        append(if (restrictionDesc.isEmpty()) who.replaceFirstChar { it.uppercase() } else who)
        append(" life rather than lose life")
        if (origin == com.wingedsheep.sdk.scripting.effects.LifeLossOrigin.RADIATION) append(" from radiation")
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
 * A life *payment* becomes an exile of that many cards off the top of the payer's library, so long
 * as the library is deep enough to cover it. Ashiok, Wicked Manipulator: "If you would pay life
 * while your library has at least that many cards in it, exile that many cards from the top of your
 * library instead."
 *
 * Applies only to [EventPattern.LifePaymentEvent] — life spent on a cost (CR 118.8). Damage and
 * "you lose N life" effects are life *loss*, not payment, and are untouched; that is exactly the
 * card's reminder text ("Damage and unpayable costs still cause you to lose life").
 *
 * Three consequences of it being a mandatory replacement, all per the printed rulings:
 * - **Not optional, not splittable.** With the library deep enough, every point is exiled instead;
 *   the payer cannot choose to pay some in life and some in cards.
 * - **Shallow library falls through.** With fewer cards in the library than the payment, the
 *   replacement simply doesn't apply and life is paid normally — this is a condition on the
 *   replacement, not a choice.
 * - **It doesn't raise what you can pay.** CR 118.5 still requires a life total at least equal to
 *   the payment, so cost legality is unchanged; only how the payment is made changes.
 *
 * @param appliesTo Which player's life payments are replaced (default: the source's controller).
 */
@SerialName("ReplaceLifePaymentWithLibraryExile")
@Serializable
data class ReplaceLifePaymentWithLibraryExile(
    override val appliesTo: EventPattern = EventPattern.LifePaymentEvent()
) : ReplacementEffect {
    override val description: String = "If ${appliesTo.description} while " +
        "${(appliesTo as? EventPattern.LifePaymentEvent)?.player?.possessive ?: "their"} library " +
        "has at least that many cards in it, exile that many cards from the top of " +
        "${(appliesTo as? EventPattern.LifePaymentEvent)?.player?.possessive ?: "their"} " +
        "library instead"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
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
 * @param additionalColors Colors unioned onto the copied colors — "it's a 4/4 black Zombie in addition
 *                   to its other colors and types" (Lazotep Convert, the back of Invasion of Amonkhet).
 *                   Rides the same [com.wingedsheep.sdk.scripting.effects.CopyExceptions.addedColors]
 *                   axis every other copy path uses.
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
 * @param entersWithCounters When non-null, the permanent enters with these counters if (and only if)
 *                   it enters as a copy — the "except it enters with N additional +1/+1 counters on
 *                   it" rider (Altered Ego with [com.wingedsheep.sdk.scripting.values.DynamicAmount.XValue])
 *                   and "except it enters with a shield counter on it if you control that creature"
 *                   (Undercover Operative). See [CopyEntryCounters]. It belongs to the *copy*
 *                   effect, not to a separate [EntersWithCounters]: copying replaces the permanent's
 *                   own copiable text, so a self-targeted enters-with-counters replacement would be
 *                   gone by the time the copy applies. Declining the copy therefore also declines
 *                   the counters, which is the printed ruling ("You can choose not to copy anything.
 *                   … It won't have +1/+1 counters placed on it by its ability.").
 * @param duration How long the copy lasts. [Duration.Permanent] (default) is every Clone;
 *                   [Duration.EndOfTurn] is "you may have it become a copy of any creature on the
 *                   battlefield until end of turn" (Cursed Mirror) — at cleanup the permanent reverts
 *                   to its printed self, riders (the granted haste) included. No other duration is
 *                   supported.
 */
@SerialName("EntersAsCopy")
@Serializable
data class EntersAsCopy(
    override val optional: Boolean = true,
    val copyFilter: GameObjectFilter = GameObjectFilter.Creature,
    val copyFromZone: Zone = Zone.BATTLEFIELD,
    val filterByTotalManaSpent: Boolean = false,
    val additionalSubtypes: List<String> = emptyList(),
    val additionalColors: Set<Color> = emptySet(),
    val additionalKeywords: List<Keyword> = emptyList(),
    val nameOverride: String? = null,
    val powerOverride: Int? = null,
    val toughnessOverride: Int? = null,
    val exileCopiedCard: Boolean = false,
    val tappedIfCopied: Boolean = false,
    val entersWithCounters: CopyEntryCounters? = null,
    val exceptions: com.wingedsheep.sdk.scripting.effects.CopyExceptions =
        com.wingedsheep.sdk.scripting.effects.CopyExceptions.None,
    val duration: Duration = Duration.Permanent,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    init {
        require(duration == Duration.Permanent || duration == Duration.EndOfTurn) {
            "EntersAsCopy supports only Permanent or EndOfTurn durations, got $duration"
        }
    }

    override val priorityGroup: ReplacementPriorityGroup
        get() = ReplacementPriorityGroup.COPY

    override val description: String = run {
        val filterDesc = copyFilter.description
        val where = if (copyFromZone == Zone.GRAVEYARD) "$filterDesc card in a graveyard" else "$filterDesc on the battlefield"
        val subject = if (copyFilter == GameObjectFilter.Land) "this land" else "this creature"
        val tappedWord = if (tappedIfCopied) "tapped " else ""
        val lead = if (duration == Duration.EndOfTurn) {
            val who = if (optional) "you may have it" else "it"
            // "Becomes a copy … until end of turn" is printed on non-creatures (Cursed Mirror).
            "As this permanent enters, $who become${if (optional) "" else "s"} a copy of any $where until end of turn"
        } else if (optional) {
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
                if (additionalSubtypes.isNotEmpty() || additionalColors.isNotEmpty()) {
                    val colorWords = additionalColors.joinToString(" ") { it.displayName.lowercase() }
                    val words = listOf(colorWords, additionalSubtypes.joinToString(" ")).filter { it.isNotEmpty() }
                    val what = if (additionalColors.isNotEmpty() && additionalSubtypes.isNotEmpty()) "colors and types"
                        else if (additionalColors.isNotEmpty()) "colors" else "types"
                    add("a ${words.joinToString(" ")} in addition to its other $what")
                }
                if (additionalKeywords.isNotEmpty()) {
                    add("it has ${additionalKeywords.joinToString(", ") { it.name.lowercase() }}")
                }
                entersWithCounters?.let { add(it.description) }
            }
            val allExceptions = exceptions + this@EntersAsCopy.exceptions.clauses()
            if (allExceptions.isNotEmpty()) append(", except ${allExceptions.joinToString(" and ")}")
            if (exileCopiedCard) append(". When you do, exile that card")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newExceptions = exceptions.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newExceptions !== exceptions)
            copy(appliesTo = newAppliesTo, exceptions = newExceptions) else this
    }
}

/**
 * The counters an [EntersAsCopy] permanent enters with when — and only when — it actually enters as
 * a copy ("except it enters with …").
 *
 * @property count How many counters: `Fixed(1)`, or `DynamicAmount.XValue` for Altered Ego's X.
 * @property counterType Which counter. Defaults to +1/+1, the "additional +1/+1 counters" rider;
 *   [CounterType.SHIELD] is Undercover Operative's.
 * @property ifCopiedMatches When non-null, the counters are placed only if the copied object matches
 *   this filter as the permanent enters, read against the entrant's controller — "if you control that
 *   creature" is `GameObjectFilter.Any.youControl()`. A copied object that has left the battlefield
 *   by then no longer matches a battlefield filter, so no counters.
 */
@Serializable
data class CopyEntryCounters(
    val count: DynamicAmount,
    val counterType: CounterType = CounterType.PLUS_ONE_PLUS_ONE,
    val ifCopiedMatches: GameObjectFilter? = null,
) {
    val description: String
        get() = buildString {
            val plusOne = counterType == CounterType.PLUS_ONE_PLUS_ONE
            val countText = count.description
            if (plusOne) {
                append("it enters with $countText additional +1/+1 counters on it")
            } else {
                val article = if (countText == "1") "a" else countText
                val plural = if (countText == "1") "" else "s"
                append("it enters with $article ${counterType.printed} counter$plural on it")
            }
            when (ifCopiedMatches) {
                null -> {}
                GameObjectFilter.Any.youControl() -> append(" if you control that creature")
                else -> append(" if the copied object matches: ${ifCopiedMatches.description}")
            }
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
    /**
     * Choose a permanent on the battlefield — by default another creature you control (Dauntless
     * Bodyguard); [EntersWithChoice.permanentFilter] widens it (Scheming Fence's "a nonland
     * permanent"). Stored in [ChoiceSlot.CREATURE] whatever its type.
     */
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
 * - [NONLAND] — every registered card name that isn't a land (Skyseer's Chariot: "choose a nonland
 *   card name").
 * - [ANY] — every registered card name (Sorcerous Spyglass / Pithing Needle: "choose any card
 *   name"). The chosen name is still stored under [com.wingedsheep.sdk.scripting.ChoiceSlot.CARD_NAME]
 *   and read the same way; only the offered option set differs.
 */
enum class CardNamePool {
    LAND,
    NONLAND,
    ANY;

    /** The decision prompt shown when naming a card from this pool. */
    val prompt: String
        get() = when (this) {
            LAND -> "Choose a land card name"
            NONLAND -> "Choose a nonland card name"
            ANY -> "Choose a card name"
        }
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
    val iconKey: String? = null,
    /**
     * The characteristics the permanent *becomes* when this option is chosen — "as this creature
     * enters, it becomes your choice of a 3/3 creature with flying, …" (Primal Clay, Corrupted
     * Shapeshifter). Unlike a mode-gated static ability, these are written into the permanent's
     * copiable values (CR 707.2: "as … enters" abilities that set power and toughness), so a
     * copy of the permanent is the chosen shape without choosing. `null` = the option only records
     * its [id].
     */
    val becomes: EntryCharacteristics? = null,
)

/**
 * Copiable characteristics an [EntersWithChoice] option sets as the permanent enters (CR 707.2).
 * [power]/[toughness] replace the printed (usually star) values; [keywords] and [subtypes] are
 * added to the printed ones ("a 1/6 Wall artifact creature with defender in addition to its other
 * types"). A copy entering as a copy of such a permanent that makes its own choice layers its
 * choice on top: the keywords accumulate and the last-set power/toughness wins.
 */
@Serializable
data class EntryCharacteristics(
    val power: Int,
    val toughness: Int,
    val keywords: Set<Keyword> = emptySet(),
    val subtypes: List<String> = emptyList(),
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
     * before making the choice. "Look at" is not a keyword action (a player normally can't see an
     * opponent's hand — CR 402.3); it's modeled here as a durable reveal to the chooser
     * (RevealedToComponent), the same convention used for "look at target player's hand". Models the
     * "look at an opponent's hand, then …" clause of Sorcerous Spyglass. The look is purely
     * informational — it does not restrict the choice. Defaults to false.
     */
    val lookAtOpponentHand: Boolean = false,
    /**
     * When [choiceType] is [ChoiceType.COLOR], the colors the chooser may *not* name — "choose a
     * color other than red" (the Thriving lands). Empty means any of the five colors. Ignored for
     * every other choice type.
     */
    val excludedColors: Set<Color> = emptySet(),
    /**
     * When [choiceType] is [ChoiceType.CREATURE_ON_BATTLEFIELD], which permanents on the battlefield
     * may be chosen, matched from the entering permanent's controller's point of view (so
     * `youControl()` means "you control"). The entering object itself is never a candidate. `null`
     * is the default "another creature you control" (Dauntless Bodyguard); Scheming Fence's "a
     * nonland permanent" is [GameObjectFilter.NonlandPermanent]. Whatever the filter, the pick is
     * stored in [ChoiceSlot.CREATURE] and read back through
     * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ChosenBySource] /
     * [com.wingedsheep.sdk.scripting.targets.EffectTarget.ChosenCreature]. Ignored for every other
     * choice type.
     */
    val permanentFilter: GameObjectFilter? = null,
    /**
     * When true, the chooser may decline — "you may choose …" (Scheming Fence) — and the permanent
     * enters with no choice recorded. Only [ChoiceType.CREATURE_ON_BATTLEFIELD] honours it. Not
     * [ReplacementEffect.optional]: that one asks whether to apply the replacement at all, while
     * the permanent enters either way and only the pick may be empty.
     */
    val mayChooseNothing: Boolean = false,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    init {
        require(choiceType == ChoiceType.CREATURE_ON_BATTLEFIELD || (permanentFilter == null && !mayChooseNothing)) {
            "permanentFilter and mayChooseNothing apply only to ChoiceType.CREATURE_ON_BATTLEFIELD"
        }
    }

    override val description: String = when (choiceType) {
        ChoiceType.COLOR -> {
            val otherThan = if (excludedColors.isEmpty()) "" else
                " other than " + excludedColors.sortedBy { it.ordinal }.joinToString(" or ") { it.displayName.lowercase() }
            if (chooser == Player.AnOpponent) {
                "As this permanent enters, an opponent chooses a color$otherThan"
            } else {
                "As this permanent enters, choose a color$otherThan"
            }
        }
        ChoiceType.CREATURE_TYPE -> if (chooser == Player.AnOpponent) {
            "As this permanent enters, an opponent chooses a creature type"
        } else {
            "As this permanent enters, choose a creature type"
        }
        ChoiceType.CREATURE_ON_BATTLEFIELD -> if (permanentFilter == null && !mayChooseNothing) {
            "As this creature enters, choose another creature you control"
        } else {
            val verb = if (mayChooseNothing) "you may choose" else "choose"
            val what = permanentFilter?.let { "${it.indefiniteArticle} ${it.description}" }
                ?: "another creature you control"
            "As this permanent enters, $verb $what"
        }
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
        val newFilter = permanentFilter?.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newFilter !== permanentFilter)
            copy(appliesTo = newAppliesTo, permanentFilter = newFilter) else this
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
 * @param counterType Counter type (default: +1/+1)
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
    val counterType: CounterType = CounterType.PLUS_ONE_PLUS_ONE,
    val countersPerReveal: Int,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Creature.youControl(),
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String =
        "As this creature enters, you may reveal any number of cards from your ${revealSource.name.lowercase()} that match. For each card revealed this way, put $countersPerReveal ${counterType.printed} counter${if (countersPerReveal > 1) "s" else ""} on it."

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAppliesTo !== appliesTo) copy(filter = newFilter, appliesTo = newAppliesTo) else this
    }
}

/**
 * As this permanent enters, its controller may exile up to [maxCards] matching cards from their
 * [sourceZone]. The exiled cards are linked to the entering permanent, and it enters with
 * [countersPerCard] [counterType] counters for each card actually exiled this way.
 *
 * This is the reusable linked-exile counterpart to [EntersWithRevealCounters]. The selection and
 * zone changes happen as part of the entry replacement, before the permanent reaches the
 * battlefield; no triggered ability is put on the stack.
 */
@SerialName("EntersWithExileCounters")
@Serializable
data class EntersWithExileCounters(
    val filter: GameObjectFilter,
    val sourceZone: Zone = Zone.GRAVEYARD,
    val maxCards: DynamicAmount,
    val counterType: CounterType = CounterType.PLUS_ONE_PLUS_ONE,
    val countersPerCard: Int = 1,
    override val appliesTo: EventPattern = EventPattern.ZoneChangeEvent(
        filter = GameObjectFilter.Any,
        to = Zone.BATTLEFIELD
    )
) : ReplacementEffect {
    override val description: String =
        "As this permanent enters, exile up to ${maxCards.description} matching cards from your " +
            "${sourceZone.name.lowercase()}. It enters with $countersPerCard ${counterType.printed} " +
            "counter${if (countersPerCard == 1) "" else "s"} for each card exiled this way."

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAppliesTo !== appliesTo) {
            copy(filter = newFilter, appliesTo = newAppliesTo)
        } else this
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
    val counterType: CounterType =
        CounterType.PLUS_ONE_PLUS_ONE,
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
        append(counterType.printed)
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
 * Where the counters land when a [ReplaceDamageWithCounters] applies.
 *
 * The recipient of the *damage* is already named by the replacement's `appliesTo` pattern; this
 * names the recipient of the *counters*, which is a separate question the printed cards answer
 * two different ways.
 */
@Serializable
enum class DamageCounterRecipient {
    /**
     * The permanent that has the replacement effect — "put that many depletion counters on
     * **this enchantment** instead" (Force Bubble), and the self-damage case where the host and
     * the damaged permanent happen to be the same object (Anti-Venom).
     */
    ReplacementHost,

    /**
     * The permanent that would have been dealt the damage — "put that many -1/-1 counters on
     * **that creature** instead" (Soul-Scar Mage). Only meaningful when the pattern's recipient
     * is a permanent; a player recipient has nowhere to put them, and the replacement declines.
     */
    DamagedPermanent
}

/**
 * Replace damage with counters (CR 614.1a — an "instead" effect, so the damage is never dealt and
 * nothing that keys on damage being dealt sees it; notably *not* a prevention effect, so it still
 * applies to damage that can't be prevented).
 *
 * Which damage is replaced comes from [appliesTo] — recipient, source, damage type and amount are
 * all filterable there. Where the counters go comes from [counterRecipient]:
 *
 * - Force Bubble — "If damage would be dealt to you, put that many depletion counters on this
 *   enchantment instead": `DamageEvent(recipient = You)`, counters on the [ReplacementHost].
 * - Soul-Scar Mage — "If a source you control would deal noncombat damage to a creature an
 *   opponent controls, put that many -1/-1 counters on that creature instead":
 *   `DamageEvent(recipient = CreatureOpponentControls, source = YouControl,
 *   damageType = NonCombat)`, counters on the [DamagedPermanent].
 *
 * @param counterType The type of counter to add (e.g. [CounterType.DEPLETION], [CounterType.MINUS_ONE_MINUS_ONE])
 * @param sacrificeThreshold If non-null, sacrifice this permanent when it has
 *        this many or more counters of the specified type (state-triggered ability).
 *        Only meaningful together with [DamageCounterRecipient.ReplacementHost].
 * @param counterRecipient Which permanent receives the counters. Defaults to the replacement's
 *        own host, which is what every "on this permanent" printing says.
 * @param damagedPlayerMills When the replaced damage was headed for a **player**, that player also
 *        mills that many cards as part of the same replacement — Szadek, Lord of Secrets: "If
 *        Szadek would deal combat damage to a player, instead put that many +1/+1 counters on
 *        Szadek and that player mills that many cards." One replacement producing both results, so
 *        they can never be split across two replacements that would each want to consume the same
 *        damage event. Ignored when the recipient is a permanent.
 */
@SerialName("ReplaceDamageWithCounters")
@Serializable
data class ReplaceDamageWithCounters(
    val counterType: CounterType,
    val sacrificeThreshold: Int? = null,
    override val appliesTo: EventPattern = EventPattern.DamageEvent(
        recipient = Recipient.You
    ),
    val counterRecipient: DamageCounterRecipient = DamageCounterRecipient.ReplacementHost,
    val damagedPlayerMills: Boolean = false
) : ReplacementEffect {
    override val description: String = buildString {
        val where = when (counterRecipient) {
            DamageCounterRecipient.ReplacementHost -> "this permanent"
            DamageCounterRecipient.DamagedPermanent -> "that permanent"
        }
        append("If ${appliesTo.description}, put that many ${counterType.printed} counters on $where")
        if (damagedPlayerMills) append(" and that player mills that many cards")
        append(" instead")
        if (sacrificeThreshold != null) {
            append(". When there are $sacrificeThreshold or more ${counterType.printed} counters on this permanent, sacrifice it")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * Prevent matched damage and mill [multiplier] times that many cards — [miller] does the milling.
 *
 * Models The Mindskinner (DSK): "If a source you control would deal damage to an opponent, prevent
 * that damage and each opponent mills that many cards" (the defaults), and Angel of Suffering (SNC):
 * "If damage would be dealt to you, prevent that damage and mill twice that many cards"
 * (`recipient = You`, `miller = Player.You`, `multiplier = 2`). The [appliesTo] pattern scopes which
 * damage is replaced.
 *
 * [miller] is read relative to this permanent's controller: [Player.EachOpponent] (every opponent,
 * not just the damaged one — the printed "each opponent") or [Player.You]. ("The damaged player
 * mills" is [ReplaceDamageWithCounters.damagedPlayerMills].)
 *
 * The mill is part of the same prevention effect, not a trigger. Per both cards' rulings, damage
 * that **can't be prevented** is still dealt in full, and the mill still happens; damage that can be
 * prevented is prevented even when the library is too small to mill the full count.
 */
@SerialName("ReplaceDamageWithMill")
@Serializable
data class ReplaceDamageWithMill(
    override val appliesTo: EventPattern = EventPattern.DamageEvent(
        recipient = Recipient.Opponent
    ),
    val miller: Player = Player.EachOpponent,
    val multiplier: Int = 1
) : ReplacementEffect {
    init {
        require(miller in SUPPORTED_MILLERS) { "ReplaceDamageWithMill.miller must be one of $SUPPORTED_MILLERS, was $miller" }
        require(multiplier >= 1) { "ReplaceDamageWithMill.multiplier must be at least 1, was $multiplier" }
    }

    override val description: String = buildString {
        append("If ${appliesTo.description}, prevent that damage and ")
        append(
            if (miller == Player.You) "you mill" else "each opponent mills"
        )
        append(if (multiplier == 2) " twice that many cards" else if (multiplier > 2) " $multiplier times that many cards" else " that many cards")
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }

    companion object {
        private val SUPPORTED_MILLERS: Set<Player> =
            setOf(Player.EachOpponent, Player.You)
    }
}

// =============================================================================
// Counter Replacement Effects
// =============================================================================

/**
 * "If a spell or ability you control would counter a spell, instead exile that spell and you may
 * play that card without paying its mana cost." — Guile.
 *
 * Replaces the counter, so the spell is **never countered**: no "whenever a spell is countered"
 * trigger sees it, and it goes to exile rather than to its owner's graveyard. Exiling is
 * mandatory. [then] is the rest of the replacement and runs immediately, before the countering
 * spell or ability continues to resolve; it reads the exiled card from the pipeline collection
 * [EXILED_CARD] (and as [com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity]).
 * Guile's `then` is a "you may" cast without paying the mana cost — declining leaves the card in
 * exile for good.
 *
 * A spell that can't be countered isn't countered, so this replacement never gets a look at it
 * and it resolves normally. A copy of a spell is exiled and ceases to exist (CR 707.10a), so there
 * is nothing left to play.
 *
 * Only the counters that go through the engine's counter routine are seen — "counter target
 * spell", "counter unless its controller pays", ward, and "counter all spells".
 */
@SerialName("ExileCounteredSpellInstead")
@Serializable
data class ExileCounteredSpellInstead(
    val then: Effect? = null,
    override val appliesTo: EventPattern = EventPattern.CounterSpellEvent()
) : ReplacementEffect {
    override val description: String = buildString {
        append("If ${appliesTo.description}, instead exile that spell")
        then?.let { append(" and ${it.description.replaceFirstChar { c -> c.lowercase() }}") }
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newThen = then?.applyTextReplacement(replacer)
        return if (newThen !== then) copy(then = newThen) else this
    }

    companion object {
        /** The pipeline collection [then] reads the exiled card from. */
        const val EXILED_CARD = "exiledInsteadOfCountered"
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
 * @param riderTiming Whether [additionalEffect] is part of the replacement itself ("instead exile
 *        it and you gain 2 life") or a reflexive triggered ability that goes on the stack after it
 *        ("exile it instead. When you do, create a 1/1 green Saproling creature token" — CR 603.12).
 * @param appliesTo The zone change event this replacement intercepts
 */
@SerialName("RedirectZoneChangeWithEffect")
@Serializable
data class RedirectZoneChangeWith(
    val newDestination: Zone,
    val additionalEffect: Effect,
    val selfOnly: Boolean = false,
    val linkToSource: Boolean = false,
    val riderTiming: ReplacementRiderTiming = ReplacementRiderTiming.WITH_REPLACEMENT,
    override val appliesTo: EventPattern
) : ReplacementEffect {
    override val description: String = when (riderTiming) {
        ReplacementRiderTiming.WITH_REPLACEMENT ->
            "If ${appliesTo.description}, instead put it into ${newDestination.displayName} and ${additionalEffect.description}"
        ReplacementRiderTiming.WHEN_YOU_DO ->
            "If ${appliesTo.description}, instead put it into ${newDestination.displayName}. " +
                "When you do, ${additionalEffect.description.replaceFirstChar { it.lowercase() }}"
    }

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer)
        val newAdditionalEffect = additionalEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newAdditionalEffect !== additionalEffect)
            copy(appliesTo = newAppliesTo, additionalEffect = newAdditionalEffect)
        else this
    }
}

/**
 * "If <permanent> would be destroyed, instead <effect>" — the destruction doesn't happen and
 * [replacementEffect] runs in its place (CR 614.1a). Crackling Emergence / Harmonious Emergence:
 * "If enchanted land would be destroyed, instead sacrifice this Aura and that land gains
 * indestructible until end of turn."
 *
 * Applies to every destruction — a destroy effect, a board wipe, or the lethal-damage / deathtouch
 * state-based actions — but not to a sacrifice or any other way of dying (the Emergence rulings).
 * [replacementEffect] is the rest of the replacement: it uses no stack and runs before anything
 * else happens — at the end of the destroying instruction, or inside the same state-based-action
 * pass. Its source is this permanent ("this Aura"); the permanent that would have been destroyed is
 * [com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity] ("that land").
 *
 * Not [RedirectZoneChangeWith]: that one still moves the permanent somewhere, and matches a zone
 * change, which a sacrifice also is. Not the umbra armor keyword ([com.wingedsheep.sdk.core.Keyword.UMBRA_ARMOR]):
 * a keyword's replacement is fixed by the rules (CR 702.89a), this one is whatever the card prints.
 *
 * @param replacementEffect what happens instead
 * @param appliesTo which permanents' destruction this replaces, relative to this permanent
 */
@SerialName("ReplaceDestructionWith")
@Serializable
data class ReplaceDestructionWith(
    val replacementEffect: Effect,
    override val appliesTo: EventPattern.DestroyEvent
) : ReplacementEffect {
    override val description: String =
        "If ${appliesTo.description}, instead ${replacementEffect.description.replaceFirstChar { it.lowercase() }}"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer) as EventPattern.DestroyEvent
        val newEffect = replacementEffect.applyTextReplacement(replacer)
        return if (newAppliesTo !== appliesTo || newEffect !== replacementEffect)
            copy(appliesTo = newAppliesTo, replacementEffect = newEffect)
        else this
    }
}

/**
 * "If <permanent> would be destroyed, regenerate it" — static regeneration (CR 701.19b). Each time a
 * matching permanent would be destroyed, instead remove all damage marked on it, its controller taps
 * it, and it's removed from combat. Mossbridge Troll, Clergy of the Holy Nimbus.
 *
 * Unlike a regeneration shield ([com.wingedsheep.sdk.scripting.effects.RegenerateEffect]) nothing is
 * spent: it applies to every destruction for as long as the ability exists. It *is* regeneration, so
 * "can't be regenerated" (a CantBeRegenerated effect, or a Wrath-style "they can't be regenerated")
 * switches it off, and it reaches every destruction a shield does — destroy effects, board wipes and
 * the lethal-damage / deathtouch state-based actions.
 *
 * Not [ReplaceDestructionWith]: that one's "instead" is whatever the card prints and runs as a rider
 * after the destroying instruction, which "can't be regenerated" doesn't touch; regeneration is fixed
 * by the rules and gated by it. Not RegenerateEffect: a shield is one-shot and lasts until end of turn.
 *
 * @param appliesTo which permanents regenerate, relative to this permanent (default: itself)
 */
@SerialName("RegenerateInsteadOfDestruction")
@Serializable
data class RegenerateInsteadOfDestruction(
    override val appliesTo: EventPattern.DestroyEvent =
        EventPattern.DestroyEvent(GameObjectFilter.Any.sourceItself())
) : ReplacementEffect {
    override val description: String = "If ${appliesTo.description}, regenerate it"

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val newAppliesTo = appliesTo.applyTextReplacement(replacer) as EventPattern.DestroyEvent
        return if (newAppliesTo !== appliesTo) copy(appliesTo = newAppliesTo) else this
    }
}

/**
 * When the rider of a [RedirectZoneChangeWith] happens relative to the replaced zone change.
 */
@Serializable
enum class ReplacementRiderTiming {
    /**
     * The rider is part of the replacement: it happens immediately, with no stack and no priority
     * between the move and the rider (CR 614.1). Ugin's Nexus, Darigaaz Reincarnated, The Darkness
     * Crystal. Only a small executor whitelist can run here.
     */
    WITH_REPLACEMENT,

    /**
     * "When you do, …" — a reflexive triggered ability (CR 603.12) that triggers each time the
     * replacement applies and goes on the stack the next time a player would receive priority.
     * Any effect can be the rider; it may be responded to. Head of the Hunt, Nemata, Primeval Warden.
     */
    WHEN_YOU_DO,
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
    override val optional: Boolean = true,
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
