package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.WardCost
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
     */
    @SerialName("Ward")
    @Serializable
    data class Ward(val cost: WardCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.WARD
        override val description: String = when (cost) {
            is WardCost.Mana -> "Ward ${cost.manaCost}"
            is WardCost.Life -> "Ward—Pay ${cost.amount} life"
            is WardCost.Discard -> "Ward—Discard ${cost.description}"
            is WardCost.Sacrifice -> "Ward—Sacrifice ${cost.description}"
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
            is ProtectionScope.Everything -> "Protection from everything"
            is ProtectionScope.EachOpponent -> "Protection from each opponent"
        }
    }

    /**
     * Hexproof from a quality. Parameterized by [ProtectionScope]; today only
     * `ProtectionScope.Color` is engine-supported (the other scopes format the
     * oracle text but have no rules-engine wiring yet).
     *
     * Example: `Hexproof(ProtectionScope.Color(Color.WHITE))` — "Hexproof from white".
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
     */
    @SerialName("Numeric")
    @Serializable
    data class Numeric(override val keyword: Keyword, val n: Int) : KeywordAbility {
        override val description: String = "${keyword.displayName} $n"
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
    // Kicker Variants
    // =========================================================================

    /**
     * Kicker and its variants. Pay [manaCost] (mana kicker), pay [additionalCost] (non-mana
     * kicker), or both. [multi] is Multikicker — the cost can be paid any number of times.
     * [displayPrefix] customises the printed-text label ("Kicker", "Multikicker", "Offspring");
     * mechanically these all share the kicker payment + `wasKicked` flag.
     *
     * Examples:
     * - `Kicker(manaCost = "{2}{B}")`                                      — "Kicker {2}{B}"
     * - `Kicker(additionalCost = SacrificePermanent(Creature))`            — "Kicker—Sacrifice a creature"
     * - `Kicker(manaCost = "{1}{W}", multi = true, displayPrefix = "Multikicker")` — "Multikicker {1}{W}"
     * - `Kicker(manaCost = "{2}", displayPrefix = "Offspring", keyword = Keyword.OFFSPRING)` — "Offspring {2}"
     *
     * Prefer the [kicker] / [kickerSacrifice] / [multikicker] / [offspring] companion factories.
     */
    @SerialName("Kicker")
    @Serializable
    data class Kicker(
        val manaCost: ManaCost? = null,
        val additionalCost: AdditionalCost? = null,
        val multi: Boolean = false,
        val displayPrefix: String = "Kicker",
        override val keyword: Keyword? = null
    ) : KeywordAbility {
        init {
            require(manaCost != null || additionalCost != null) {
                "Kicker requires either a manaCost or an additionalCost"
            }
        }
        override val description: String = when {
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
        constructor(cost: ManaCost) : this(PayCost.Mana(cost))

        override val description: String = "Morph ${morphCost.description}"
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
    // Mayhem
    // =========================================================================

    /**
     * Mayhem with a mana cost.
     * "Mayhem {2}{R}" - You may cast this card from your graveyard for its mayhem cost
     * if you discarded it this turn. Then exile it.
     */
    @SerialName("Mayhem")
    @Serializable
    data class Mayhem(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.MAYHEM
        override val description: String = "Mayhem $cost"
    }

    // =========================================================================
    // Warp
    // =========================================================================

    /**
     * Warp with a mana cost.
     * "Warp {2}{R}" - You may cast this card for its warp cost. Exile it at the
     * beginning of the next end step. You may cast it from exile using its warp
     * ability on a later turn.
     */
    @SerialName("Warp")
    @Serializable
    data class Warp(val cost: ManaCost) : KeywordAbility {
        override val description: String = "Warp $cost"
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
         * Create Ward with life cost.
         */
        fun wardLife(amount: Int): KeywordAbility = Ward(WardCost.Life(amount))

        /**
         * Create Ward with discard cost.
         */
        fun wardDiscard(count: Int = 1, random: Boolean = false): KeywordAbility =
            Ward(WardCost.Discard(count, random))

        /**
         * Create Ward with sacrifice cost.
         */
        fun wardSacrifice(filter: GameObjectFilter): KeywordAbility =
            Ward(WardCost.Sacrifice(filter))

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
         * Create Morph with mana cost from string.
         */
        fun morph(cost: String): KeywordAbility = Morph(ManaCost.parse(cost))

        /**
         * Create Morph with life payment cost.
         */
        fun morphPayLife(amount: Int): KeywordAbility = Morph(PayCost.PayLife(amount))

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
         * Create Kicker with a mana cost.
         */
        fun kicker(cost: String): KeywordAbility = Kicker(manaCost = ManaCost.parse(cost))

        fun kicker(cost: ManaCost): KeywordAbility = Kicker(manaCost = cost)

        /**
         * Create Kicker with a non-mana additional cost (e.g., sacrifice a creature).
         */
        fun kicker(additionalCost: AdditionalCost): KeywordAbility =
            Kicker(additionalCost = additionalCost)

        /**
         * Create Multikicker — a kicker whose cost can be paid any number of times.
         */
        fun multikicker(cost: String): KeywordAbility = Kicker(
            manaCost = ManaCost.parse(cost),
            multi = true,
            displayPrefix = "Multikicker"
        )

        /**
         * Create Offspring — mechanically a kicker, but printed and labelled "Offspring".
         */
        fun offspring(cost: String): KeywordAbility = offspring(ManaCost.parse(cost))

        fun offspring(cost: ManaCost): KeywordAbility = Kicker(
            manaCost = cost,
            displayPrefix = "Offspring",
            keyword = Keyword.OFFSPRING
        )

        /**
         * Create Mayhem with mana cost from string.
         */
        fun mayhem(cost: String): KeywordAbility = Mayhem(ManaCost.parse(cost))

        /**
         * Create Warp with mana cost from string.
         */
        fun warp(cost: String): KeywordAbility = Warp(ManaCost.parse(cost))

        /**
         * Create Evoke with mana cost from string.
         */
        fun evoke(cost: String): KeywordAbility = Evoke(ManaCost.parse(cost))

        /**
         * Create Conspire keyword ability.
         */
        fun conspire(): KeywordAbility = Conspire

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
        fun crew(n: Int): KeywordAbility = Numeric(Keyword.CREW, n)
        fun modular(n: Int): KeywordAbility = Numeric(Keyword.MODULAR, n)
        fun fading(n: Int): KeywordAbility = Numeric(Keyword.FADING, n)
        fun vanishing(n: Int): KeywordAbility = Numeric(Keyword.VANISHING, n)
        fun renown(n: Int): KeywordAbility = Numeric(Keyword.RENOWN, n)
        fun fabricate(n: Int): KeywordAbility = Numeric(Keyword.FABRICATE, n)
        fun tribute(n: Int): KeywordAbility = Numeric(Keyword.TRIBUTE, n)
    }
}
