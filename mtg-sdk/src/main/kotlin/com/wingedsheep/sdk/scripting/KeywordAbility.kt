package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.dsl.impending
import com.wingedsheep.sdk.dsl.mobilize
import com.wingedsheep.sdk.dsl.sneak
import com.wingedsheep.sdk.dsl.webSlinging

/**
 * Represents a keyword ability, which may be simple (Flying) or parameterized (Ward {2}).
 *
 * Parameterized keywords include:
 * - Ward with a cost (mana, life, discard)
 * - Protection from a quality (color, card type)
 * - Numeric N (Annihilator, Bushido, Rampage, Toxic, Crew, Modular, Fading,
 *   Vanishing, Renown, Fabricate, Tribute, Absorb, Afflict)
 *
 * Usage:
 * ```kotlin
 * KeywordAbility.Simple(Keyword.FLYING)
 * KeywordAbility.Ward(WardCost.Mana("{2}"))
 * KeywordAbility.Protection(ProtectionScope.Color(Color.BLUE))
 * KeywordAbility.Numeric(Keyword.ANNIHILATOR, 2)
 * ```
 */
@Serializable
sealed interface KeywordAbility {
    val description: String

    /**
     * Returns the simple [Keyword] enum that corresponds to this parameterized keyword ability,
     * or null if there is no direct mapping. Used to automatically populate [CardDefinition.keywords]
     * so that parameterized keyword abilities (e.g., Ward {1}) are visible in the base keyword set.
     */
    val keyword: Keyword? get() = null

    /**
     * Non-null for the ninjutsu-family alternative costs ([Sneak], [Ninjutsu]): the mana cost to
     * put this card onto the battlefield tapped and attacking by returning an unblocked attacker
     * you control to hand (CR 702.49 / 506.3a). The shared declare-blockers pipeline
     * ([com.wingedsheep.engine] `SneakWindow` / `SneakCastEnumerator` / the cast handler / the
     * stack resolver) keys off this single property, so a new reflavor of the mechanic only needs
     * to override it — no new code path. Null for every other keyword ability.
     */
    val ninjutsuStyleCost: ManaCost? get() = null

    /**
     * Simple keyword with no parameters.
     * Examples: Flying, Trample, Haste
     */
    @SerialName("Simple")
    @Serializable
    data class Simple(override val keyword: Keyword) : KeywordAbility {
        override val description: String = keyword.displayName
    }

    // =========================================================================
    // Ward
    // =========================================================================

    /**
     * Ward with a configurable cost.
     *
     * Examples:
     * - `Ward(WardCost.Mana("{2}"))`         — "Ward {2}"
     * - `Ward(WardCost.Life(2))`             — "Ward—Pay 2 life"
     * - `Ward(WardCost.Discard())`           — "Ward—Discard a card"
     * - `Ward(WardCost.Sacrifice(filter))`   — "Ward—Sacrifice a Food"
     * - `Ward(WardCost.CollectEvidence(4))`  — "Ward—Collect evidence 4"
     * - `Ward(WardCost.PlayerCounters(Counters.POISON, 5))` — "Ward—Get five poison counters"
     * - `Ward(WardCost.Choice(...))`         — "Ward—Discard a card or pay {2}"
     */
    @SerialName("Ward")
    @Serializable
    data class Ward(val cost: WardCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.WARD
        override val description: String = when (cost) {
            is WardCost.Mana ->
                if (cost.waterbend) "Ward—Waterbend ${cost.manaCost}" else "Ward ${cost.manaCost}"
            is WardCost.Life -> "Ward—Pay ${cost.amount} life"
            is WardCost.DynamicLife -> "Ward—Pay life equal to ${cost.amount.description}"
            is WardCost.Discard -> "Ward—Discard ${cost.description}"
            is WardCost.Sacrifice -> "Ward—Sacrifice ${cost.description}"
            // Capitalized: "Collect evidence N" is a keyword action, and the printed line reads
            // "Ward—Collect evidence 4."
            is WardCost.CollectEvidence -> "Ward—Collect evidence ${cost.amount}"
            is WardCost.PlayerCounters -> "Ward—Get ${cost.description}"
            // A disjunction renders each option with its own verb, so it goes through `clause`
            // (see WardCost.clause) rather than a prefix this branch supplies.
            is WardCost.Choice -> "Ward—${cost.clause.replaceFirstChar { it.uppercase() }}"
            is WardCost.Composite -> "Ward—${cost.description}"
        }
    }

    // =========================================================================
    // Protection / Hexproof
    // =========================================================================

    /**
     * Protection from a quality. Parameterized by [ProtectionScope].
     *
     * Examples:
     * - `Protection(ProtectionScope.Color(Color.BLUE))`            — "Protection from blue"
     * - `Protection(ProtectionScope.Colors(setOf(W, U)))`           — "Protection from white and from blue"
     * - `Protection(ProtectionScope.Subtype("Goblin"))`             — "Protection from Goblins"
     * - `Protection(ProtectionScope.Everything)`                    — "Protection from everything"
     * - `Protection(ProtectionScope.EachOpponent)`                  — "Protection from each opponent" (Rule 702.16e)
     */
    @SerialName("Protection")
    @Serializable
    data class Protection(val scope: ProtectionScope) : KeywordAbility {
        override val keyword: Keyword? = when (scope) {
            is ProtectionScope.EachOpponent -> Keyword.PROTECTION_FROM_EACH_OPPONENT
            else -> null
        }
        override val description: String = when (scope) {
            is ProtectionScope.Color -> "Protection from ${scope.color.displayName.lowercase()}"
            is ProtectionScope.Colors -> "Protection from " +
                scope.colors.joinToString(" and from ") { it.displayName.lowercase() }
            is ProtectionScope.CardType -> "Protection from ${scope.cardType.lowercase()}"
            is ProtectionScope.Subtype -> "Protection from ${scope.subtype}s"
            is ProtectionScope.Supertype -> "Protection from ${scope.supertype.lowercase()}"
            is ProtectionScope.Everything -> "Protection from everything"
            is ProtectionScope.EachOpponent -> "Protection from each opponent"
        }
    }

    /**
     * Hexproof from a quality. Parameterized by [ProtectionScope]; `ProtectionScope.Color`,
     * `ProtectionScope.Colors` and `ProtectionScope.CardType` are engine-supported (the remaining
     * scopes format the oracle text but have no rules-engine wiring yet).
     *
     * Examples:
     * - `Hexproof(ProtectionScope.Color(Color.WHITE))`    — "Hexproof from white" (Knight of Malice)
     * - `Hexproof(ProtectionScope.CardType("Instant"))`   — "Hexproof from instants" (Elenda, Saint of Dusk)
     */
    @SerialName("Hexproof")
    @Serializable
    data class Hexproof(val scope: ProtectionScope) : KeywordAbility {
        override val description: String = when (scope) {
            is ProtectionScope.Color -> "Hexproof from ${scope.color.displayName.lowercase()}"
            is ProtectionScope.Colors -> "Hexproof from " +
                scope.colors.joinToString(" and from ") { it.displayName.lowercase() }
            is ProtectionScope.CardType -> "Hexproof from ${scope.cardType.lowercase()}"
            is ProtectionScope.Subtype -> "Hexproof from ${scope.subtype}s"
            is ProtectionScope.Supertype -> "Hexproof from ${scope.supertype.lowercase()}"
            is ProtectionScope.Everything -> "Hexproof from everything"
            is ProtectionScope.EachOpponent -> "Hexproof from each opponent"
        }
    }

    // =========================================================================
    // Numeric Keywords
    // =========================================================================

    /**
     * A keyword parameterized by a single integer. Covers Annihilator, Bushido,
     * Rampage, Absorb, Afflict, Toxic, Crew, Modular, Fading, Vanishing, Renown,
     * Fabricate, Tribute, etc. The display text is "<keyword> <n>" using
     * [Keyword.displayName].
     *
     * Examples:
     * - `Numeric(Keyword.ANNIHILATOR, 2)` — "Annihilator 2"
     * - `Numeric(Keyword.TOXIC, 1)`       — "Toxic 1"
     * - `Numeric(Keyword.CREW, 3)`        — "Crew 3"
     *
     * [onceEachTurn] models a per-turn activation cap on the keyword ability — currently only
     * meaningful for Crew ("Crew 1. Activate only once each turn." — Luxurious Locomotive). It
     * defaults to false and, with `encodeDefaults = false`, is omitted from the compiled JSON so
     * existing numeric-keyword cards are unaffected.
     */
    @SerialName("Numeric")
    @Serializable
    data class Numeric(
        override val keyword: Keyword,
        val n: Int,
        val onceEachTurn: Boolean = false
    ) : KeywordAbility {
        override val description: String = buildString {
            append("${keyword.displayName} $n")
            if (onceEachTurn) append(". Activate only once each turn.")
        }
    }

    /**
     * A numeric keyword whose count is determined dynamically rather than by a fixed
     * integer — e.g. "Mobilize X, where X is the number of creature cards in your
     * graveyard" (Avenger of the Fallen). The keyword ability itself is display-only;
     * the dynamic count lives in the composed triggered ability that the DSL helper
     * wires alongside it. [label] is the placeholder shown after the keyword name
     * (defaults to "X"), so the card prints "Mobilize X".
     *
     * Examples:
     * - `Variable(Keyword.MOBILIZE)` — "Mobilize X"
     */
    @SerialName("Variable")
    @Serializable
    data class Variable(override val keyword: Keyword, val label: String = "X") : KeywordAbility {
        override val description: String = "${keyword.displayName} $label"
    }

    /**
     * Flanking.
     * "Flanking" - Whenever this creature becomes blocked by a creature without flanking,
     * the blocking creature gets -1/-1 until end of turn.
     */
    @SerialName("Flanking")
    @Serializable
    data object Flanking : KeywordAbility {
        override val description: String = "Flanking"
    }

    // =========================================================================
    // Cost Reduction Keywords
    // =========================================================================

    /**
     * Affinity for a card type.
     * "Affinity for artifacts" - This spell costs {1} less to cast for each artifact you control.
     */
    @SerialName("Affinity")
    @Serializable
    data class Affinity(val forType: CardType) : KeywordAbility {
        override val keyword: Keyword = Keyword.AFFINITY
        override val description: String = "Affinity for ${forType.displayName.lowercase()}s"
    }

    /**
     * Affinity for a creature subtype.
     * "Affinity for Lizards" - This spell costs {1} less to cast for each Lizard you control.
     */
    @SerialName("AffinityForSubtype")
    @Serializable
    data class AffinityForSubtype(val forSubtype: Subtype) : KeywordAbility {
        override val keyword: Keyword = Keyword.AFFINITY
        override val description: String = "Affinity for ${forSubtype.value}s"
    }

    // =========================================================================
    // Cycling Variants
    // =========================================================================

    /**
     * Cycling and its typed variants. Plain cycling (search filter absent) discards
     * and draws a card; typed cycling (search filter present) discards and searches
     * the library for a card matching [searchFilter].
     *
     * Examples:
     * - `Cycling(cost = "{2}")`                                                           — "Cycling {2}"
     * - `Cycling(cost = "{2}", searchFilter = Forest, displayPrefix = "Forestcycling")`   — "Forestcycling {2}"
     * - `Cycling(cost = "{1}{U}", searchFilter = BasicLand, displayPrefix = "Basic landcycling")` — "Basic landcycling {1}{U}"
     *
     * Prefer the [typecycling] / [basicLandcycling] companion factories when constructing
     * typed variants — they hide the filter wiring.
     */
    @SerialName("Cycling")
    @Serializable
    data class Cycling(
        val cost: ManaCost,
        val searchFilter: GameObjectFilter? = null,
        val displayPrefix: String = "Cycling"
    ) : KeywordAbility {
        override val description: String = "$displayPrefix $cost"
    }

    // =========================================================================
    // Optional Additional Cost (Kicker, Multikicker, Offspring, FlashKicker, Bargain)
    // =========================================================================

    /**
     * **Optional additional cost paid at cast time.** Generalises Kicker, Multikicker,
     * Offspring, Bargain, and the pre-kicker "pay {N} more to cast as though it had flash"
     * pattern (Ghitu Fire et al.). The card script gates effect variations on the durable
     * fact recorded in [declaredSlot] — read via [WasKicked] / `Conditions.WasBargained`,
     * or `wasKicked` in trigger filters — when [branchesEffect] is `true`.
     *
     * Mechanically all variants share the same plumbing: the player optionally pays
     * [manaCost] and/or [additionalCost] as an extra cost while casting, the spell carries
     * [declaredSlot] as its declared-cost slot on the stack (and durably, once it resolves
     * into a permanent), and the cost calculator folds the extra mana into the effective
     * cost. The variants differ only in *what the payment unlocks*:
     *
     * - **Kicker / Multikicker / Offspring** ([branchesEffect] = `true`) — the spell's
     *   effect branches on `WasKicked`. [multi] = `true` lets the cost be paid any
     *   number of times (Multikicker).
     * - **Bargain** ([declaredSlot] = [ChoiceSlot.BARGAINED]) — same shape as
     *   kicker-with-a-sacrifice-cost, but a *different* fact (CR 702.166b), so bargaining
     *   never reads as kicking and vice versa.
     * - **FlashKicker** ([grantsFlashTiming] = `true`) — paying the cost lets the spell
     *   be cast as though it had flash. Effect is unchanged unless the card also opts
     *   into [branchesEffect] (rare — Ghitu Fire does not).
     *
     * [displayPrefix] customises the printed label ("Kicker", "Multikicker", "Offspring");
     * for FlashKicker it's ignored and the description is rephrased to match the printed
     * oracle text, and for Bargain the `bargain()` DSL supplies the printed reminder text.
     *
     * The serial name remains `Kicker` for wire compatibility with previously serialised
     * card scripts.
     *
     * Examples:
     * - `OptionalAdditionalCost(manaCost = "{2}{B}")`                                                — "Kicker {2}{B}"
     * - `OptionalAdditionalCost(additionalCost = SacrificePermanent(Creature))`                     — "Kicker—Sacrifice a creature"
     * - `OptionalAdditionalCost(manaCost = "{1}{W}", multi = true, displayPrefix = "Multikicker")`  — "Multikicker {1}{W}"
     * - `OptionalAdditionalCost(manaCost = "{2}", displayPrefix = "Offspring", keyword = Keyword.OFFSPRING)` — "Offspring {2}"
     * - `OptionalAdditionalCost(manaCost = "{2}", grantsFlashTiming = true, branchesEffect = false)` — Ghitu Fire's flash unlock
     *
     * Prefer the [kicker] / [kickerSacrifice] / [multikicker] / [offspring] / [flashKicker]
     * companion factories, or the `bargain()` DSL helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    @SerialName("Kicker")
    @Serializable
    data class OptionalAdditionalCost(
        val manaCost: ManaCost? = null,
        val additionalCost: AdditionalCost? = null,
        val multi: Boolean = false,
        val displayPrefix: String = "Kicker",
        override val keyword: Keyword? = null,
        /**
         * If `true`, paying the optional cost is observable to the card's own effect
         * via the [com.wingedsheep.sdk.scripting.conditions.WasKicked] condition or the
         * `wasKicked` flag on trigger filters. Standard kicker/multikicker/offspring
         * keep this on; FlashKicker turns it off so unrelated cards (Cackling Witch &
         * other "if a kicked spell was cast" payoffs) don't see a flash-timing payment
         * as kicker.
         */
        val branchesEffect: Boolean = true,
        /**
         * If `true`, paying the optional cost grants flash timing — the spell may be
         * cast as though it had flash. The optional cost may be a [manaCost] (Ghitu
         * Fire: "you may cast this as though it had flash if you pay {2} more") or a
         * non-mana [additionalCost] such as Behold (Molten Exhale: "you may cast this
         * as though it had flash if you behold a Dragon as an additional cost").
         */
        val grantsFlashTiming: Boolean = false,
        /**
         * Which durable cast-choice slot records "this optional cost was declared" — the
         * *identity* of the mechanic riding this rail. Kicker/Multikicker/Offspring stamp
         * [ChoiceSlot.KICKED] (the default); Bargain stamps [ChoiceSlot.BARGAINED]
         * (CR 702.166b). The engine stamps this slot on the spell as it is cast and carries it
         * onto the resolving permanent, and the payoff conditions and cast-trigger filters key
         * off it — so two mechanics on the same rail never read each other's declaration.
         */
        val declaredSlot: ChoiceSlot = ChoiceSlot.KICKED
    ) : KeywordAbility {
        init {
            require(manaCost != null || additionalCost != null) {
                "OptionalAdditionalCost requires either a manaCost or an additionalCost"
            }
        }
        override val description: String = when {
            // Bargain's and Teamwork's costs are definitional (CR 702.166a / 702.194a), so the
            // printed text is the bare keyword — never "Bargain—sacrifice …" / "Teamwork 2—tap …".
            // The reminder text belongs to the card's `oracleText`.
            declaredSlot == ChoiceSlot.BARGAINED || declaredSlot == ChoiceSlot.TEAMWORK -> displayPrefix
            // Collect evidence is a keyword *action*, not a keyword ability, so it has no keyword
            // label to hang a "Prefix—cost" shape on: the cards print the additional cost in full
            // (CR 701.59, "As an additional cost to cast this spell, you may collect evidence 6").
            declaredSlot == ChoiceSlot.EVIDENCE_COLLECTED ->
                "As an additional cost to cast this spell, you may " +
                    "${additionalCost?.description?.replaceFirstChar { it.lowercase() }}."
            grantsFlashTiming && additionalCost != null ->
                "You may cast this spell as though it had flash if you " +
                    "${additionalCost.description.replaceFirstChar { it.lowercase() }} as an additional cost to cast it."
            grantsFlashTiming ->
                "You may cast this spell as though it had flash if you pay $manaCost more to cast it."
            manaCost != null && additionalCost != null -> "$displayPrefix—$manaCost, ${additionalCost.description}"
            additionalCost != null -> "$displayPrefix—${additionalCost.description}"
            else -> "$displayPrefix $manaCost"
        }
    }

    // =========================================================================
    // Morph
    // =========================================================================

    /**
     * Morph with a cost to turn face up.
     * "Morph {2}{U}" or "Morph—Pay 5 life."
     * You may cast this card face down as a 2/2 creature for {3}.
     * Turn it face up any time for its morph cost.
     */
    @SerialName("Morph")
    @Serializable
    data class Morph(
        val morphCost: PayCost,
        /** Effect to execute as a replacement effect when turned face up (e.g., put 5 +1/+1 counters on it) */
        val faceUpEffect: com.wingedsheep.sdk.scripting.effects.Effect? = null
    ) : KeywordAbility {
        /** Convenience constructor for mana-based morph costs. */
        constructor(cost: ManaCost) : this(PayCost.Atom(CostAtom.Mana(cost)))

        override val description: String = "Morph ${morphCost.description}"
    }

    // =========================================================================
    // Disguise
    // =========================================================================

    /**
     * Disguise with a cost to turn face up (CR 702.168).
     * "Disguise {2}{U}" or "Disguise—Pay 5 life."
     * You may cast this card face down as a 2/2 creature **with ward {2}** for {3}.
     * Turn it face up any time you have priority for its disguise cost.
     *
     * Disguise is morph plus ward {2}; the ward is part of the face-down characteristic-defining
     * effect ([com.wingedsheep.sdk.scripting.effects.FaceDownMode.DISGUISE]), not of this ability.
     *
     * The cost is a full [PayCost] rather than a [ManaCost] because CR 702.168e explicitly
     * contemplates X in a disguise cost (other abilities of the permanent then refer to the value
     * chosen as the special action was taken), and because non-mana costs are expressible in the
     * same way morph costs are.
     */
    @SerialName("Disguise")
    @Serializable
    data class Disguise(
        val disguiseCost: PayCost,
        /**
         * Effect applied as part of the turn-face-up action, for the "As this creature is turned
         * face up, …" replacement clause (Bubble Smuggler: "put four +1/+1 counters on it"). The
         * exact sibling of [Morph.faceUpEffect] — it does *not* use the stack and can't be
         * responded to, which is what separates it from a `Triggers.TurnedFaceUp` ability.
         */
        val faceUpEffect: com.wingedsheep.sdk.scripting.effects.Effect? = null
    ) : KeywordAbility {
        /** Convenience constructor for mana-based disguise costs. */
        constructor(cost: ManaCost) : this(PayCost.Atom(CostAtom.Mana(cost)))

        override val description: String = "Disguise ${disguiseCost.description}"
    }

    // =========================================================================
    // Flashback
    // =========================================================================

    /**
     * Flashback with a mana cost and an optional additional cost.
     * "Flashback {4}{B}" - You may cast this card from your graveyard for its
     * flashback cost. Then exile it.
     *
     * The optional [additionalCost] models flashback variants that bundle a
     * non-mana cost (e.g., "Flashback—{1}{R}, Behold three Elementals.").
     * It is paid only on the flashback cast path; hand casts ignore it.
     */
    @SerialName("Flashback")
    @Serializable
    data class Flashback(
        val cost: ManaCost,
        val additionalCost: AdditionalCost? = null
    ) : KeywordAbility {
        override val keyword: Keyword = Keyword.FLASHBACK
        override val description: String =
            if (additionalCost == null) "Flashback $cost"
            else "Flashback—$cost, ${additionalCost.description}"
    }

    // =========================================================================
    // Harmonize
    // =========================================================================

    /**
     * Harmonize with a mana cost (Tarkir: Dragonstorm).
     * "Harmonize {5}{R}{R}" — You may cast this card from your graveyard for its
     * harmonize cost. As you do, you may tap a single untapped creature you control
     * to reduce that cost by an amount of generic mana equal to its power. Then exile
     * this card as it leaves the stack.
     *
     * Modelled like [Flashback] (graveyard cast + exile-on-resolution) plus a
     * Convoke-style single-creature reduction routed through
     * [AlternativePaymentChoice.harmonizeCreature]. The reduction lowers only the
     * generic portion of the cost — power can pay only generic mana.
     */
    @SerialName("Harmonize")
    @Serializable
    data class Harmonize(
        val cost: ManaCost
    ) : KeywordAbility {
        override val keyword: Keyword = Keyword.HARMONIZE
        override val description: String = "Harmonize $cost"
    }

    // =========================================================================
    // Mayhem
    // =========================================================================

    /**
     * Mayhem [cost] (CR 702.187, Marvel's Spider-Man). "As long as you discarded this card this
     * turn, you may cast it from your graveyard by paying [cost] rather than paying its mana cost."
     *
     * Unlike [Flashback]/[Harmonize], a Mayhem spell is NOT exiled on resolution: a permanent
     * simply enters the battlefield, and an instant/sorcery goes to the graveyard as normal. It
     * grants no timing permission — normal timing rules still apply (a non-Flash creature/sorcery
     * can only be Mayhem-cast at sorcery speed). The "you discarded this card this turn" gate is
     * `Conditions.YouDiscardedThisCardThisTurn`, checked by the engine's Mayhem enumerator and
     * cast-permission logic.
     *
     * [cost] is empty for the CR 702.187c "Mayhem" (no cost) form used by lands (Oscorp
     * Industries) — those are *played* from the graveyard for no mana rather than cast.
     */
    @SerialName("Mayhem")
    @Serializable
    data class Mayhem(
        val cost: ManaCost
    ) : KeywordAbility {
        override val keyword: Keyword = Keyword.MAYHEM
        override val description: String = "Mayhem $cost"
    }

    // =========================================================================
    // Disturb
    // =========================================================================

    /**
     * Disturb [cost] (CR 702.146a). "You may cast this card transformed from your graveyard by
     * paying [cost] rather than its mana cost."
     *
     * Printed on the front face of a transforming double-faced card, and functional only from its
     * owner's graveyard. Casting this way puts the card on the stack **back face up**, so per
     * CR 712.8c the spell has only the back face's characteristics — its card types decide the
     * timing, and its `targetRequirements` / `auraTarget` decide what is targeted (the Innistrad
     * disturb cycle has both creature and Aura back faces). Its mana value is still calculated from
     * the front face's mana cost.
     *
     * Unlike [Flashback]/[Harmonize] a disturb cast does **not** exile the card on resolution: the
     * back face enters the battlefield, and it is the back face's own printed "if this would be put
     * into a graveyard from anywhere, exile it instead" replacement that keeps it from coming back a
     * second time.
     */
    @SerialName("Disturb")
    @Serializable
    data class Disturb(
        val cost: ManaCost
    ) : KeywordAbility {
        override val keyword: Keyword = Keyword.DISTURB
        override val description: String = "Disturb $cost"
    }

    // =========================================================================
    // Madness
    // =========================================================================

    /**
     * Madness [cost] (CR 702.35). "If you discard this card, discard it into exile. When you do,
     * cast it for its madness cost or put it into your graveyard."
     *
     * Per CR 702.35a this single keyword is two abilities: a static ability functioning in hand
     * ("if a player would discard this card, that player discards it, but exiles it instead of
     * putting it into their graveyard") and a triggered ability functioning on that exile ("when
     * this card is exiled this way, its owner may cast it by paying [cost] rather than paying its
     * mana cost. If that player doesn't, they put this card into their graveyard").
     *
     * Both halves are engine-driven off this entry rather than off printed script: the discard
     * redirect lives in the zone-change replacement check, and the cast offer is the synthesized
     * [com.wingedsheep.sdk.scripting.Madness.castAbility]. The cast follows the alternative-cost
     * rules (CR 702.35b / 601.2b / 601.2f–h), so [cost] replaces the printed mana cost while
     * additional costs and cost increases still apply.
     */
    @SerialName("Madness")
    @Serializable
    data class Madness(
        val cost: ManaCost
    ) : KeywordAbility {
        override val keyword: Keyword = Keyword.MADNESS
        override val description: String = "Madness $cost"
    }

    // =========================================================================
    // Warp
    // =========================================================================

    /**
     * Warp with a mana cost and an optional additional cost.
     * "Warp {2}{R}" - You may cast this card for its warp cost. Exile it at the
     * beginning of the next end step. You may cast it from exile using its warp
     * ability on a later turn.
     *
     * [additionalCost] models warp variants that bundle a non-mana cost
     * (e.g., "Warp—{B}, Pay 2 life." on Timeline Culler). It is paid only on
     * the warp cast path.
     *
     * [fromGraveyard] permits warp to be paid while the card is in the caster's
     * graveyard. Default warp (CR 702.185a) is hand-only; cards like Timeline
     * Culler explicitly grant graveyard access via their oracle text.
     *
     * Provisional: if a sibling keyword ever needs the same "you may cast this
     * from your graveyard using its X ability" wording, extract a generic
     * cast-from-zone permission rather than duplicating this flag.
     */
    @SerialName("Warp")
    @Serializable
    data class Warp(
        val cost: ManaCost,
        val additionalCost: AdditionalCost? = null,
        val fromGraveyard: Boolean = false
    ) : KeywordAbility {
        override val description: String =
            if (additionalCost == null) "Warp $cost"
            else "Warp—$cost, ${additionalCost.description}"
    }

    // =========================================================================
    // Dash
    // =========================================================================

    /**
     * Dash with a mana cost (CR 702.109, Khans of Tarkir).
     * "Dash [cost]" — You may cast this card by paying [cost] rather than its mana cost. If you
     * do, it gains haste, and it's returned from the battlefield to its owner's hand at the
     * beginning of the next end step.
     *
     * Hand-only (CR 702.109a doesn't grant a graveyard/other-zone variant the way Warp's
     * `fromGraveyard` flag does) — no sibling flag needed unless a future card's oracle text
     * says otherwise.
     */
    @SerialName("Dash")
    @Serializable
    data class Dash(val cost: ManaCost) : KeywordAbility {
        override val description: String = "Dash $cost"
    }

    // =========================================================================
    // Plot
    // =========================================================================

    /**
     * Plot (CR 718, Outlaws of Thunder Junction).
     * "Plot {cost}" — special action: pay [cost] and exile this card from your hand
     * any time you have priority during your main phase while the stack is empty.
     * It becomes plotted. You may cast a plotted card from exile as a sorcery on a
     * later turn without paying its mana cost.
     *
     * Per the official ruling: plotting is a special action (does not use the stack
     * and cannot be responded to once announced) and a plotted card cannot be cast
     * the same turn it was plotted. Casting from exile follows the regular spell
     * rules but waives the mana cost.
     */
    @SerialName("Plot")
    @Serializable
    data class Plot(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.PLOT
        override val description: String = "Plot $cost"
    }

    // =========================================================================
    // Foretell
    // =========================================================================

    /**
     * Foretell (CR 702.143, Kaldheim).
     * "Foretell [cost]" — special action: any time you have priority during your turn,
     * pay {2} and exile this card from your hand *face down*. It becomes foretold. On a
     * later turn (after the turn it was foretold has ended) you may cast it from exile by
     * paying [cost] — the foretell cost — rather than its mana cost.
     *
     * Per CR 702.143 / 116.2h: foretelling is a special action (does not use the stack)
     * and a foretold card cannot be cast the same turn it was foretold. [cost] is the
     * foretell cost paid to cast it later; the {2} setup cost is fixed by the rules and
     * is not stored here.
     */
    @SerialName("Foretell")
    @Serializable
    data class Foretell(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.FORETELL
        override val description: String = "Foretell $cost"
    }

    // =========================================================================
    // Suspend
    // =========================================================================

    /**
     * Printed Suspend (CR 702.62, Time Spiral). "Suspend N—[cost]" — a static ability that
     * functions while the card is in hand: "If you could begin to cast this card by putting
     * it onto the stack from your hand, you may pay [cost] and exile it with N time counters
     * on it. This action doesn't use the stack." (CR 702.62a / 116.2f special action.)
     *
     * This class only carries the printed *setup* half — [cost] and [timeCounters]. The
     * countdown-and-cast half is component-driven, not definition-driven: the engine's
     * synthesized [com.wingedsheep.sdk.scripting.Suspend.countdownAbility] is granted to
     * *any* exiled card carrying the runtime `SuspendedComponent` marker, so the same
     * machinery serves both a printed suspend (stamped by the engine's
     * `SuspendCardFromHandHandler` special action) and a suspend granted at runtime to an
     * otherwise-ordinary card (e.g. Taigam, Master Opportunist, via
     * [com.wingedsheep.sdk.dsl.Effects.Suspend]).
     */
    @SerialName("Suspend")
    @Serializable
    data class Suspend(val cost: ManaCost, val timeCounters: Int) : KeywordAbility {
        override val keyword: Keyword = Keyword.SUSPEND
        override val description: String = "Suspend $timeCounters—$cost"
    }

    // =========================================================================
    // Conspire
    // =========================================================================

    /**
     * Conspire.
     * "Conspire" - As you cast this spell, you may tap two untapped creatures you control
     * that share a color with it. When you do, copy it and you may choose new targets for
     * the copy.
     *
     * Conspire is an optional additional cost combined with a reflexive triggered copy.
     * The "share a color with it" predicate is evaluated at cost-payment time against the
     * spell's colors, so Conspire is intrinsically bound to a specific spell — the tap-two
     * payment is handled as a Conspire-specific branch of the cast flow rather than a
     * generic AdditionalCost subtype.
     */
    @SerialName("Conspire")
    @Serializable
    data object Conspire : KeywordAbility {
        override val keyword: Keyword = Keyword.CONSPIRE
        override val description: String = "Conspire"
    }

    // =========================================================================
    // Casualty
    // =========================================================================

    /**
     * Casualty N (CR 702.153).
     * "As an additional cost to cast this spell, you may sacrifice a creature with power [threshold]
     * or greater. When you do, copy this spell and you may choose new targets for the copy."
     *
     * Like Conspire, Casualty is an optional additional cost combined with a reflexive triggered
     * copy. The power-threshold check is evaluated at cost-payment time against the sacrificed
     * creature's projected power, so the choose-the-creature payment is handled as a
     * Casualty-specific branch of the cast flow (the threshold is intrinsic to the spell) rather
     * than a generic [com.wingedsheep.sdk.scripting.AdditionalCost] subtype.
     */
    @SerialName("Casualty")
    @Serializable
    data class Casualty(val threshold: Int) : KeywordAbility {
        override val keyword: Keyword = Keyword.CASUALTY
        override val description: String = "Casualty $threshold"
    }

    // =========================================================================
    // Miracle
    // =========================================================================

    /**
     * Miracle {cost} (CR 702.94).
     * "You may cast this card for its miracle cost when you draw it if it's the first card you drew
     * this turn."
     *
     * Modeled as a hand-only alternative cost gated by a one-turn window: when this card is the
     * first card its owner draws in a turn, the engine opens a miracle window (a per-card component
     * cleared at end of turn) and the cast-from-hand enumerator surfaces a "Cast (Miracle)"
     * alternative cost paying [cost] instead of the mana cost.
     */
    @SerialName("Miracle")
    @Serializable
    data class Miracle(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.MIRACLE
        override val description: String = "Miracle $cost"
    }

    // =========================================================================
    // Evoke
    // =========================================================================

    /**
     * Evoke with a mana cost.
     * "Evoke {R/W}{R/W}" - You may cast this spell for its evoke cost.
     * If you do, it's sacrificed when it enters the battlefield.
     *
     * Evoke is an alternative cost. When cast for evoke, the creature enters
     * the battlefield normally (ETB triggers fire), then a separate "sacrifice self"
     * delayed trigger goes on the stack. Players can respond between ETB and sacrifice.
     */
    @SerialName("Evoke")
    @Serializable
    data class Evoke(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.EVOKE
        override val description: String = "Evoke $cost"
    }

    // =========================================================================
    // Emerge
    // =========================================================================

    /**
     * Emerge [cost] (CR 702.119, Eldritch Moon).
     * "You may cast this spell by paying [cost] and sacrificing a creature rather than paying its
     * mana cost" plus "if you chose to pay this spell's emerge cost, its total cost is reduced by
     * an amount of generic mana equal to the sacrificed creature's mana value" (CR 702.119a).
     *
     * An alternative cost (like [Evoke]) with two extra characteristics:
     *  - an **additional cost** paid alongside the mana: sacrificing one creature you control,
     *    chosen as you choose to pay the emerge cost and sacrificed as you pay the total cost
     *    (CR 702.119c); and
     *  - a **cost reduction** derived from that choice: the sacrificed creature's mana value comes
     *    off the *generic* portion only, so it can never reduce a colored pip and any excess is
     *    simply wasted.
     *
     * The sacrifice rides `CastSpell.additionalCostPayment.sacrificedPermanents`, exactly like
     * [Sneak]'s bounce rides `bouncedPermanents`. Attach via the `emerge("{cost}")` DSL helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    @SerialName("Emerge")
    @Serializable
    data class Emerge(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.EMERGE
        override val description: String = "Emerge $cost"
    }

    // =========================================================================
    // Gift
    // =========================================================================

    /**
     * Gift a [kind] (CR 702.174, Bloomburrow).
     *
     * Two abilities in one keyword (CR 702.174a): the additional cost "as an additional cost to
     * cast this spell, you may choose an opponent" — elected **as the spell is cast**, carried on
     * [com.wingedsheep.sdk.scripting.ChoiceSlot.GIFT_PROMISED] + [ChoiceSlot.OPPONENT] and readable
     * via [com.wingedsheep.sdk.dsl.Conditions.GiftWasPromised] — and, on a permanent, the triggered
     * ability "when this permanent enters, if its gift cost was paid, [effect]" (CR 702.174b),
     * whose effect [kind] defines.
     *
     * Attach via the `gift(kind)` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder], which adds
     * this keyword *and* the derived enters-the-battlefield ability. Instants and sorceries fold
     * their gift-paid branch into the spell's own effect instead — see
     * [com.wingedsheep.sdk.dsl.MechanicPatterns.giftSpell].
     */
    @SerialName("Gift")
    @Serializable
    data class Gift(val kind: GiftKind) : KeywordAbility {
        override val description: String = "Gift ${kind.label}"
    }

    // =========================================================================
    // Sneak
    // =========================================================================

    /**
     * Sneak [cost] (CR 702.190, Teenage Mutant Ninja Turtles).
     * "Any time you could cast an instant during your declare blockers step, you may cast
     * this spell by paying [cost] and returning an unblocked creature you control to its
     * owner's hand rather than paying this spell's mana cost." (CR 702.190a)
     *
     * Sneak is an alternative cost (like [Evoke]) with two extra characteristics:
     *  - a **timing permission**: it is legal only during the active player's declare
     *    blockers step, and only while they control an unblocked attacker to return; and
     *  - an **additional cost**: returning one unblocked creature you control to its
     *    owner's hand, paid alongside the [cost] mana.
     *
     * A permanent spell whose sneak cost was paid enters the battlefield tapped and
     * attacking the same player/planeswalker the returned creature was attacking
     * (CR 702.190b / 506.3a). The "sneak cost was paid" fact rides the resulting permanent
     * durably and is readable via [com.wingedsheep.sdk.scripting.conditions.SneakCostWasPaid].
     *
     * Attach via the `sneak("{cost}")` DSL helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    @SerialName("Sneak")
    @Serializable
    data class Sneak(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.SNEAK
        override val ninjutsuStyleCost: ManaCost get() = cost
        override val description: String = "Sneak $cost"
    }

    // =========================================================================
    // Ninjutsu
    // =========================================================================

    /**
     * Ninjutsu [cost] (CR 702.49).
     * "[cost], Return an unblocked attacker you control to hand: Put this card onto the
     * battlefield from your hand tapped and attacking."
     *
     * The canonical rules keyword for the same mechanic [Sneak] reflavors. It's an alternative
     * cost with a declare-blockers timing permission (CR 702.49b — legal only once blocked/
     * unblocked status is assigned) and an additional cost (return one unblocked attacker you
     * control to its owner's hand). A permanent put onto the battlefield this way enters tapped
     * and attacking the same player/planeswalker/battle the returned creature was attacking
     * (CR 506.3a) — for a card that isn't a creature as it enters, it just enters tapped.
     *
     * Behaviorally unified with [Sneak] via [ninjutsuStyleCost], so the engine's shared sneak
     * pipeline drives it unchanged. Attach via the `ninjutsu("{cost}")` DSL helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    @SerialName("Ninjutsu")
    @Serializable
    data class Ninjutsu(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.NINJUTSU
        override val ninjutsuStyleCost: ManaCost get() = cost
        override val description: String = "Ninjutsu $cost"
    }

    // =========================================================================
    // Splice
    // =========================================================================

    /**
     * Splice onto [onto] [cost] (CR 702.47, Champions of Kamigawa).
     * "You may reveal this card from your hand as you cast a [onto] spell. If you do, that spell
     * gains the text of this card's rules text and you pay [cost] as an additional cost to cast that
     * spell." (CR 702.47a)
     *
     * Unlike every other cost-carrying keyword here, splice never casts or moves the card it is
     * printed on: it is a static ability functioning **in hand**, and the card stays there — free to
     * be cast normally later, or spliced onto a different spell (CR 702.47a). What the splice cost
     * buys is that the *spell being cast* gains this card's rules text.
     *
     * [onto] is the "[quality]" of the rules text, matched against the spell's subtypes; every
     * printed splice card so far reads "splice onto Arcane", which is why that is the default.
     *
     * The engine surfaces one `CastWithSplice` legal-action variant per splice card in hand while an
     * eligible spell is castable, charges [cost] as an additional cost (CR 601.2b / 601.2f–h),
     * reveals the card, and appends this card's own spell effect and target requirements to the
     * spell on the stack — after the main spell's effects (CR 702.47b) and without granting the
     * spell any of this card's other characteristics (CR 702.47c).
     *
     * Attach via the `splice("{cost}")` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    @SerialName("Splice")
    @Serializable
    data class Splice(
        val cost: ManaCost,
        val onto: Subtype = Subtype.ARCANE,
    ) : KeywordAbility {
        override val keyword: Keyword = Keyword.SPLICE
        override val description: String = "Splice onto $onto $cost"
    }

    // =========================================================================
    // Impending
    // =========================================================================

    /**
     * Impending N—[cost] (CR 702.175, Duskmourn: House of Horror).
     * "If you cast this spell for its impending cost, it enters with N time counters
     * and isn't a creature until the last is removed. At the beginning of your end step,
     * remove a time counter from it."
     *
     * Impending is a self-alternative cost: you may pay [cost] rather than the spell's
     * mana cost. When a spell cast for its impending cost resolves, the engine places
     * [time] time counters on the permanent. While it has a time counter it isn't a
     * creature, and at the beginning of the controller's end step a time counter is
     * removed; the permanent becomes a creature once the last is gone.
     *
     * The full behavior is wired by the `impending(n, cost)` DSL helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder], which attaches this keyword ability plus the
     * conditional type-removing static ability and the end-step counter-removal trigger.
     */
    @SerialName("Impending")
    @Serializable
    data class Impending(val time: Int, val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.IMPENDING
        override val description: String = "Impending $time—$cost"
    }

    // =========================================================================
    // Web-slinging
    // =========================================================================

    /**
     * Web-slinging [cost] (CR 702.188, Marvel's Spider-Man).
     * "Web-slinging [cost]" means "You may cast this spell by paying [cost] and returning a tapped
     * creature you control to its owner's hand rather than paying its mana cost." (CR 702.188a)
     *
     * An alternative cost (like [Evoke]) whose payment bundles a non-mana portion — returning one
     * tapped creature you control to its owner's hand — alongside the [cost] mana. Unlike the
     * ninjutsu family ([Sneak] / [Ninjutsu]) it carries **no** timing permission: the spell is
     * web-slung at its normal timing, so this is not routed through [ninjutsuStyleCost]. The mana
     * value is unchanged by web-slinging (CR 118.9c) and any cost increases/reductions still apply
     * on top (CR 118.9d).
     *
     * A permanent spell whose web-slinging cost was paid carries two durable facts merged into its
     * [com.wingedsheep.engine…CastChoicesComponent]: the flag
     * [com.wingedsheep.sdk.scripting.ChoiceSlot.WEB_SLUNG] (readable via
     * [com.wingedsheep.sdk.dsl.Conditions.WebSlungCostWasPaid], used by Spiders-Man, Heroic Horde)
     * and the returned creature's mana value under
     * [com.wingedsheep.sdk.scripting.ChoiceSlot.WEB_SLUNG_RETURNED_MV] (read via
     * [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastChoice], used by Scarlet Spider,
     * Ben Reilly to enter with that many +1/+1 counters).
     *
     * Attach via the `webSlinging("{cost}")` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    @SerialName("WebSlinging")
    @Serializable
    data class WebSlinging(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.WEB_SLINGING
        override val description: String = "Web-slinging $cost"
    }

    // =========================================================================
    // Cleave
    // =========================================================================

    /**
     * Cleave [cost] (CR 702.148, Innistrad: Crimson Vow).
     * "Cleave [cost]" means "You may cast this spell by paying [cost] rather than paying its mana
     * cost" and "If this spell's cleave cost was paid, change its text by removing all text found
     * within square brackets in the spell's rules text." (CR 702.148a)
     *
     * An alternative cost (like [Evoke]) whose second ability is a text-changing effect
     * (CR 702.148b / 612). We model the text change *structurally* rather than by parsing brackets:
     * the card author supplies a brackets-removed effect and target-requirement variant via
     * [com.wingedsheep.sdk.model.CardScript.cleaveSpellEffect] /
     * [com.wingedsheep.sdk.model.CardScript.cleaveTargetRequirements] (mirroring how kicker supplies
     * an alternate effect/target tree). Casting for cleave swaps in that variant at cast time, so a
     * clause inside brackets that would create a delayed triggered ability is never created at all
     * (Alchemist's Gambit ruling). The cleave cost never changes the spell's mana value (CR 202.3b).
     *
     * Wired by the `cleave(cost) { }` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder], which
     * attaches this keyword ability and captures the brackets-removed effect/targets.
     */
    @SerialName("Cleave")
    @Serializable
    data class Cleave(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.CLEAVE
        override val description: String = "Cleave $cost"
    }

    // =========================================================================
    // Devour
    // =========================================================================

    /**
     * Devour (CR 702.82) and variants. "As this creature enters, you may sacrifice
     * any number of [sacrificeFilter]. This creature enters with [multiplier] times
     * that many +1/+1 counters on it."
     *
     * The plain "Devour N" prints with [variant] = `""` and a creature sacrifice
     * filter. Variants such as Edge of Eternities' "Devour land 3" print with
     * [variant] = `"land"` and a land sacrifice filter; the display becomes
     * "Devour land N".
     *
     * The mechanical wiring lives on the matching replacement effect
     * [com.wingedsheep.sdk.scripting.EntersWithDevour], which the card script must
     * also declare. This [KeywordAbility] entry exists so that the parameterized
     * text renders (and so the card surfaces [Keyword.DEVOUR] in its base keyword set).
     */
    @SerialName("Devour")
    @Serializable
    data class Devour(
        val multiplier: Int,
        val sacrificeFilter: GameObjectFilter = GameObjectFilter.Creature,
        val variant: String = ""
    ) : KeywordAbility {
        override val keyword: Keyword = Keyword.DEVOUR
        override val description: String = if (variant.isBlank()) {
            "Devour $multiplier"
        } else {
            "Devour $variant $multiplier"
        }
    }

    // =========================================================================
    // Increment
    // =========================================================================

    /**
     * Increment (Secrets of Strixhaven).
     * "Whenever you cast a spell, if the amount of mana you spent is greater than this
     * creature's power or toughness, put a +1/+1 counter on this creature."
     *
     * This [KeywordAbility] entry is display-only (it prints the keyword + reminder text
     * and surfaces [Keyword.INCREMENT] in the base keyword set). The mechanical wiring —
     * the "whenever you cast a spell" triggered ability gated on the mana-spent intervening-if —
     * is composed by the `increment()` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    @SerialName("Increment")
    @Serializable
    data object Increment : KeywordAbility {
        override val keyword: Keyword = Keyword.INCREMENT
        override val description: String = "Increment"
    }

    // =========================================================================
    // Companion Methods
    // =========================================================================

    companion object {
        /**
         * Create a simple keyword ability from a Keyword enum.
         */
        fun of(keyword: Keyword): KeywordAbility = Simple(keyword)

        /**
         * Create Ward with mana cost from string.
         */
        fun ward(cost: String): KeywordAbility = Ward(WardCost.Mana(cost))

        /**
         * Create Ward—Waterbend with mana cost from string (Avatar: The Last Airbender —
         * The Unagi of Kyoshi Island's "Ward—Waterbend {4}"). Same as [ward] but the {N} may
         * be paid by tapping the paying player's untapped artifacts and creatures, each paying
         * {1} of the generic, through the shared waterbend payment machinery.
         */
        fun wardWaterbend(cost: String): KeywordAbility = Ward(WardCost.Mana(cost, waterbend = true))

        /**
         * Create Ward with a fixed life cost.
         */
        fun wardLife(amount: Int): KeywordAbility = Ward(WardCost.Life(amount))

        /**
         * Create Ward with a [DynamicAmount] life cost — "Ward—Pay life equal to ~"
         * (e.g. Raubahn, Bull of Ala Mhigo with [com.wingedsheep.sdk.dsl.DynamicAmounts.sourcePower]).
         * The amount is evaluated when the ward triggered ability resolves.
         */
        fun wardLife(amount: DynamicAmount): KeywordAbility = Ward(WardCost.DynamicLife(amount))

        /**
         * Create Ward with discard cost. When [filter] is non-null the discarded
         * card(s) must match it (e.g. "Ward—Discard an enchantment, instant, or sorcery card").
         */
        fun wardDiscard(
            count: Int = 1,
            random: Boolean = false,
            filter: GameObjectFilter? = null,
        ): KeywordAbility =
            Ward(WardCost.Discard(count, random, filter))

        /**
         * Create Ward with sacrifice cost. Pass [count] > 1 for "Ward—Sacrifice N ~"
         * (e.g. Valgavoth, Terror Eater — "Ward—Sacrifice three nonland permanents").
         */
        fun wardSacrifice(filter: GameObjectFilter, count: Int = 1): KeywordAbility =
            Ward(WardCost.Sacrifice(filter, count))

        /**
         * Create Ward—Collect evidence N (CR 701.59) — "Ward—Collect evidence 4" (Axebane Ferox).
         * The targeting opponent must exile cards with total mana value [amount] or greater from
         * their own graveyard, or the spell/ability is countered.
         */
        fun wardCollectEvidence(amount: Int): KeywordAbility =
            Ward(WardCost.CollectEvidence(amount))

        /**
         * Create Ward with a composite cost — all components must be paid. E.g.
         * `wardComposite(WardCost.Mana("{2}"), WardCost.Life(2))` for "Ward—{2}, Pay 2 life"
         * (Gisa, the Hellraiser).
         */
        fun wardComposite(vararg parts: WardCost): KeywordAbility =
            Ward(WardCost.Composite(parts.toList()))

        /**
         * Create Ward with a cost paid in counters placed on the paying player — e.g.
         * `wardPlayerCounters(Counters.POISON, 5)` for "Ward—Get five poison counters."
         * (The Serpent Society). [counterType] is a `Counters.*` symbol.
         */
        fun wardPlayerCounters(counterType: String, amount: Int): KeywordAbility =
            Ward(WardCost.PlayerCounters(counterType, amount))

        /**
         * Create Ward with a **disjunctive** cost — the payer picks exactly one of [options] and
         * pays it. The OR sibling of [wardComposite]'s AND. E.g.
         * `wardChoice(WardCost.Discard(), WardCost.Mana("{2}"))` for
         * "Ward—Discard a card or pay {2}." Prefer the named [wardDiscardOrPay] for that printed
         * wording; reach for this factory for any other combination.
         */
        fun wardChoice(vararg options: WardCost): KeywordAbility =
            Ward(WardCost.Choice(options.toList()))

        /**
         * "Ward—Discard a card or pay {N}." (Titania, Rugged Rumbler) — the ward-side rendering of
         * the same printed shape [com.wingedsheep.sdk.dsl.Costs.additional.DiscardOrPay] renders
         * as an *additional cost*, deliberately named to match it so a card carrying both (as
         * Titania does) reads as one shape twice, not two inventions.
         *
         * They stay separate types because the rails genuinely differ: an additional cost's mana
         * leg folds into the spell's own mana cost at cast time (`AdditionalCost.OrPay`), while a
         * ward cost is paid on its own as the ward trigger resolves — so on this side the mana leg
         * is an ordinary [WardCost.Mana] option with no special status.
         */
        fun wardDiscardOrPay(
            alternativeManaCost: String,
            filter: GameObjectFilter? = null,
            count: Int = 1,
        ): KeywordAbility = wardChoice(
            WardCost.Discard(count = count, filter = filter),
            WardCost.Mana(alternativeManaCost),
        )

        /**
         * Create Hexproof from a color.
         */
        fun hexproofFrom(color: Color): KeywordAbility = Hexproof(ProtectionScope.Color(color))

        /**
         * Create Protection from a color.
         */
        fun protectionFrom(color: Color): KeywordAbility =
            Protection(ProtectionScope.Color(color))

        /**
         * Create Protection from multiple colors.
         */
        fun protectionFrom(vararg colors: Color): KeywordAbility =
            Protection(ProtectionScope.Colors(colors.toSet()))

        /**
         * Create Protection from a creature subtype.
         */
        fun protectionFromSubtype(subtype: String): KeywordAbility =
            Protection(ProtectionScope.Subtype(subtype))

        /** Protection from a supertype — "from legendary creatures" (Tsabo Tavoc). */
        fun protectionFromSupertype(supertype: String): KeywordAbility =
            Protection(ProtectionScope.Supertype(supertype))

        /**
         * Plain Devour: sacrifice any number of creatures as this enters, N counters each.
         */
        fun devour(multiplier: Int): KeywordAbility = Devour(multiplier)

        /**
         * Devour-land variant (Edge of Eternities): sacrifice any number of lands as this
         * enters, [multiplier] +1/+1 counters per land sacrificed.
         */
        fun devourLand(multiplier: Int): KeywordAbility =
            Devour(multiplier, sacrificeFilter = GameObjectFilter.Land, variant = "land")

        /**
         * Create plain Cycling with mana cost from string.
         */
        fun cycling(cost: String): KeywordAbility = Cycling(ManaCost.parse(cost))

        /**
         * Create Typecycling for a subtype (e.g., "Forest", "Sliver"). Display text is
         * "${subtype}cycling $cost"; the search filter is "any card with that subtype".
         */
        fun typecycling(subtype: String, cost: ManaCost): KeywordAbility = Cycling(
            cost = cost,
            searchFilter = GameObjectFilter.Any.withSubtype(subtype),
            displayPrefix = "${subtype}cycling"
        )

        fun typecycling(subtype: String, cost: String): KeywordAbility =
            typecycling(subtype, ManaCost.parse(cost))

        /**
         * Create Basic landcycling — search the library for any basic land card.
         */
        fun basicLandcycling(cost: ManaCost): KeywordAbility = Cycling(
            cost = cost,
            searchFilter = GameObjectFilter.BasicLand,
            displayPrefix = "Basic landcycling"
        )

        fun basicLandcycling(cost: String): KeywordAbility = basicLandcycling(ManaCost.parse(cost))

        /**
         * Create Plot with mana cost from a string.
         */
        fun plot(cost: String): KeywordAbility = Plot(ManaCost.parse(cost))

        /**
         * Create Foretell with its foretell cost from a string (CR 702.143).
         * The {2} setup cost is fixed by the rules; [cost] is the cost to cast it later.
         */
        fun foretell(cost: String): KeywordAbility = Foretell(ManaCost.parse(cost))

        /**
         * Create printed Suspend with a mana cost from a string and its time-counter count
         * (CR 702.62). E.g. `suspend("{U}", 4)` for "Suspend 4—{U}".
         */
        fun suspend(cost: String, timeCounters: Int): KeywordAbility = Suspend(ManaCost.parse(cost), timeCounters)

        /**
         * Create Morph with mana cost from string.
         */
        fun morph(cost: String): KeywordAbility = Morph(ManaCost.parse(cost))

        /**
         * Create Morph with life payment cost.
         */
        fun morphPayLife(amount: Int): KeywordAbility = Morph(PayCost.Atom(CostAtom.PayLife(amount)))

        /**
         * Create Disguise with mana cost from string (CR 702.168).
         */
        fun disguise(cost: String): KeywordAbility = Disguise(ManaCost.parse(cost))

        /**
         * Create Flashback with mana cost from string.
         */
        fun flashback(cost: String): KeywordAbility = Flashback(ManaCost.parse(cost))

        /**
         * Create Flashback with a mana cost and an additional cost (e.g., "Flashback—{1}{R}, Behold three Elementals").
         */
        fun flashback(cost: String, additionalCost: AdditionalCost): KeywordAbility =
            Flashback(ManaCost.parse(cost), additionalCost)

        /**
         * Create Harmonize with mana cost from string (e.g., "Harmonize {5}{R}{R}").
         */
        fun harmonize(cost: String): KeywordAbility = Harmonize(ManaCost.parse(cost))

        /**
         * Create Mayhem with mana cost from string (e.g., "Mayhem {B}"). Pass "" for the CR
         * 702.187c "Mayhem" (no cost) land form (Oscorp Industries).
         */
        fun mayhem(cost: String): KeywordAbility = Mayhem(ManaCost.parse(cost))

        /**
         * Create Madness with mana cost from string (e.g., "Madness {R}").
         */
        fun madness(cost: String): KeywordAbility = Madness(ManaCost.parse(cost))

        /**
         * Create Disturb with mana cost from string (e.g., "Disturb {1}{W}"). Belongs on the front
         * face of a transforming double-faced card whose back face is a permanent.
         */
        fun disturb(cost: String): KeywordAbility = Disturb(ManaCost.parse(cost))

        /**
         * Create Kicker with a mana cost.
         */
        fun kicker(cost: String): KeywordAbility = OptionalAdditionalCost(manaCost = ManaCost.parse(cost))

        fun kicker(cost: ManaCost): KeywordAbility = OptionalAdditionalCost(manaCost = cost)

        /**
         * Create Kicker with a non-mana additional cost (e.g., sacrifice a creature).
         */
        fun kicker(additionalCost: AdditionalCost): KeywordAbility =
            OptionalAdditionalCost(additionalCost = additionalCost)

        /**
         * Create Multikicker — a kicker whose cost can be paid any number of times.
         */
        fun multikicker(cost: String): KeywordAbility = OptionalAdditionalCost(
            manaCost = ManaCost.parse(cost),
            multi = true,
            displayPrefix = "Multikicker"
        )

        /**
         * Create a flash-timing kicker (Ghitu Fire pattern) — paying [cost] more lets you
         * cast the spell as though it had flash. The spell's effect is unchanged (the
         * optional payment is invisible to [com.wingedsheep.sdk.scripting.conditions.WasKicked]
         * because [OptionalAdditionalCost.branchesEffect] is `false`).
         */
        fun flashKicker(cost: String): KeywordAbility = OptionalAdditionalCost(
            manaCost = ManaCost.parse(cost),
            grantsFlashTiming = true,
            branchesEffect = false
        )

        /**
         * Create a flash-timing unlock whose cost is a non-mana [additionalCost]
         * (Molten Exhale pattern) — paying it (e.g. beholding a Dragon) lets you cast
         * the spell as though it had flash. As with the mana form, the payment is
         * invisible to [com.wingedsheep.sdk.scripting.conditions.WasKicked]
         * ([OptionalAdditionalCost.branchesEffect] is `false`), so the spell's effect
         * is unchanged.
         */
        fun flashKicker(additionalCost: AdditionalCost): KeywordAbility = OptionalAdditionalCost(
            additionalCost = additionalCost,
            grantsFlashTiming = true,
            branchesEffect = false
        )

        /**
         * Create Offspring — mechanically a kicker, but printed and labelled "Offspring".
         */
        fun offspring(cost: String): KeywordAbility = offspring(ManaCost.parse(cost))

        fun offspring(cost: ManaCost): KeywordAbility = OptionalAdditionalCost(
            manaCost = cost,
            displayPrefix = "Offspring",
            keyword = Keyword.OFFSPRING
        )

        /**
         * Create Warp with mana cost from string.
         */
        fun warp(cost: String): KeywordAbility = Warp(ManaCost.parse(cost))

        /**
         * Create Dash with a mana cost from a string (CR 702.109). E.g. `dash("{1}{R}")` for
         * "Dash {1}{R}".
         */
        fun dash(cost: String): KeywordAbility = Dash(ManaCost.parse(cost))

        /**
         * Create Evoke with mana cost from string.
         */
        fun evoke(cost: String): KeywordAbility = Evoke(ManaCost.parse(cost))

        /**
         * Create Emerge with mana cost from string (CR 702.119). Prefer the `emerge(cost)` DSL
         * helper on [com.wingedsheep.sdk.dsl.CardBuilder].
         */
        fun emerge(cost: String): KeywordAbility = Emerge(ManaCost.parse(cost))

        /**
         * Create Sneak with mana cost from string. Prefer the `sneak(cost)` DSL helper on
         * [com.wingedsheep.sdk.dsl.CardBuilder].
         */
        fun sneak(cost: String): KeywordAbility = Sneak(ManaCost.parse(cost))

        /**
         * Create Ninjutsu with mana cost from string. Prefer the `ninjutsu(cost)` DSL helper on
         * [com.wingedsheep.sdk.dsl.CardBuilder].
         */
        fun ninjutsu(cost: String): KeywordAbility = Ninjutsu(ManaCost.parse(cost))

        /**
         * Create Splice onto [onto] with a splice cost from string (CR 702.47). Prefer the
         * `splice(cost)` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder].
         */
        fun splice(cost: String, onto: Subtype = Subtype.ARCANE): KeywordAbility =
            Splice(ManaCost.parse(cost), onto)

        /**
         * Create Impending with a time-counter count and an impending mana cost.
         * Prefer the `impending(n, cost)` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder],
         * which also wires the associated static ability and end-step trigger.
         */
        fun impending(time: Int, cost: String): KeywordAbility = Impending(time, ManaCost.parse(cost))

        /**
         * Create Web-slinging with a mana cost from string (CR 702.188). Prefer the
         * `webSlinging(cost)` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder].
         */
        fun webSlinging(cost: String): KeywordAbility = WebSlinging(ManaCost.parse(cost))

        /**
         * Create Cleave with a cleave mana cost (CR 702.148). Declared on the card via
         * `keywordAbility(KeywordAbility.cleave("{cost}"))`; the brackets-removed effect / target
         * variant is supplied inside the card's `spell { }` block via `cleaveEffect` /
         * `cleaveTarget(...)` (mirroring how kicker declares `kickerEffect` / `kickerTarget`).
         */
        fun cleave(cost: String): KeywordAbility = Cleave(ManaCost.parse(cost))

        /**
         * Create Conspire keyword ability.
         */
        fun conspire(): KeywordAbility = Conspire

        /**
         * Create Casualty N keyword ability.
         */
        fun casualty(threshold: Int): KeywordAbility = Casualty(threshold)

        /**
         * Create Miracle keyword ability with a miracle mana cost.
         */
        fun miracle(cost: String): KeywordAbility = Miracle(ManaCost.parse(cost))

        /**
         * Create Toxic with a numeric value.
         */
        fun toxic(count: Int): KeywordAbility = Numeric(Keyword.TOXIC, count)

        // Numeric-keyword shorthands. Each `Keyword.<X>` numeric ability is just
        // `Numeric(Keyword.<X>, n)`; these helpers exist purely for readability.
        fun annihilator(n: Int): KeywordAbility = Numeric(Keyword.ANNIHILATOR, n)
        fun bushido(n: Int): KeywordAbility = Numeric(Keyword.BUSHIDO, n)
        fun rampage(n: Int): KeywordAbility = Numeric(Keyword.RAMPAGE, n)
        fun absorb(n: Int): KeywordAbility = Numeric(Keyword.ABSORB, n)
        fun afflict(n: Int): KeywordAbility = Numeric(Keyword.AFFLICT, n)
        fun crew(n: Int, onceEachTurn: Boolean = false): KeywordAbility =
            Numeric(Keyword.CREW, n, onceEachTurn)
        fun saddle(n: Int): KeywordAbility = Numeric(Keyword.SADDLE, n)
        fun modular(n: Int): KeywordAbility = Numeric(Keyword.MODULAR, n)
        fun fading(n: Int): KeywordAbility = Numeric(Keyword.FADING, n)
        fun vanishing(n: Int): KeywordAbility = Numeric(Keyword.VANISHING, n)
        fun renown(n: Int): KeywordAbility = Numeric(Keyword.RENOWN, n)
        fun fabricate(n: Int): KeywordAbility = Numeric(Keyword.FABRICATE, n)
        fun tribute(n: Int): KeywordAbility = Numeric(Keyword.TRIBUTE, n)
        fun mobilize(n: Int): KeywordAbility = Numeric(Keyword.MOBILIZE, n)
        fun firebending(n: Int): KeywordAbility = Numeric(Keyword.FIREBENDING, n)

        /**
         * Mobilize X — display tag for a Mobilize whose count is dynamic (e.g.
         * "Mobilize X, where X is the number of creature cards in your graveyard").
         * The dynamic token count lives in the attack-triggered ability wired by the
         * `mobilize(amount, ...)` DSL helper; this factory only provides the
         * "Mobilize X" display text.
         */
        fun mobilizeVariable(label: String = "X"): KeywordAbility = Variable(Keyword.MOBILIZE, label)

        /**
         * Hideaway N — display tag for the parameterized hideaway keyword. The
         * mechanic itself (look at top N, exile one face down linked to source,
         * bottom-randomize the rest) is composed manually as an ETB triggered
         * ability using `MoveCollectionEffect(faceDown = FaceDownMode.HIDDEN, linkToSource = true)`
         * + `CardSource.FromLinkedExile()`. This factory only provides the
         * "Hideaway 4"-style display text on the card's keyword list.
         */
        fun hideaway(n: Int): KeywordAbility = Numeric(Keyword.HIDEAWAY, n)
    }
}
