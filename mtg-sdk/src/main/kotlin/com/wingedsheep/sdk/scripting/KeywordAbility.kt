package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.costs.PayCost
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a keyword ability, which may be simple (Flying) or parameterized (Ward {2}).
 *
 * Parameterized keywords include:
 * - Ward with a cost (mana, life, discard)
 * - Protection from a quality (color, card type)
 * - Annihilator, Bushido, Rampage with a number
 * - Crew, Fabricate, Modular, Renown with a number
 *
 * Usage:
 * ```kotlin
 * KeywordAbility.Simple(Keyword.FLYING)
 * KeywordAbility.Ward(ManaCost.parse("{2}"))
 * KeywordAbility.ProtectionFromColor(Color.BLUE)
 * KeywordAbility.Annihilator(2)
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
    // Ward Variants
    // =========================================================================

    /**
     * Ward with a mana cost.
     * "Ward {2}" - Whenever this creature becomes the target of a spell or ability
     * an opponent controls, counter it unless that player pays {2}.
     */
    @SerialName("WardMana")
    @Serializable
    data class WardMana(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.WARD
        override val description: String = "Ward $cost"
    }

    /**
     * Ward with a life cost.
     * "Ward—Pay 2 life."
     */
    @SerialName("WardLife")
    @Serializable
    data class WardLife(val amount: Int) : KeywordAbility {
        override val keyword: Keyword = Keyword.WARD
        override val description: String = "Ward—Pay $amount life"
    }

    /**
     * Ward with a discard cost.
     * "Ward—Discard a card."
     */
    @SerialName("WardDiscard")
    @Serializable
    data class WardDiscard(val count: Int = 1, val random: Boolean = false) : KeywordAbility {
        override val keyword: Keyword = Keyword.WARD
        override val description: String = buildString {
            append("Ward—Discard ")
            if (count == 1) {
                append("a card")
            } else {
                append("$count cards")
            }
            if (random) append(" at random")
        }
    }

    /**
     * Ward with a sacrifice cost.
     * "Ward—Sacrifice a creature."
     */
    @SerialName("WardSacrifice")
    @Serializable
    data class WardSacrifice(val filter: GameObjectFilter) : KeywordAbility {
        override val keyword: Keyword = Keyword.WARD
        override val description: String = "Ward—Sacrifice a ${filter.description}"
    }

    // =========================================================================
    // Hexproof Variants
    // =========================================================================

    /**
     * Hexproof from a color.
     * "Hexproof from white" - This creature can't be the target of white spells
     * or abilities your opponents control.
     */
    @SerialName("HexproofFromColor")
    @Serializable
    data class HexproofFromColor(val color: Color) : KeywordAbility {
        override val description: String = "Hexproof from ${color.displayName.lowercase()}"
    }

    // =========================================================================
    // Protection Variants
    // =========================================================================

    /**
     * Protection from a color.
     * "Protection from blue"
     */
    @SerialName("ProtectionFromColor")
    @Serializable
    data class ProtectionFromColor(val color: Color) : KeywordAbility {
        override val description: String = "Protection from ${color.displayName.lowercase()}"
    }

    /**
     * Protection from multiple colors.
     * "Protection from white and from blue"
     */
    @SerialName("ProtectionFromColors")
    @Serializable
    data class ProtectionFromColors(val colors: Set<Color>) : KeywordAbility {
        override val description: String = "Protection from " +
                colors.joinToString(" and from ") { it.displayName.lowercase() }
    }

    /**
     * Protection from a card type.
     * "Protection from creatures"
     */
    @SerialName("ProtectionFromCardType")
    @Serializable
    data class ProtectionFromCardType(val cardType: String) : KeywordAbility {
        override val description: String = "Protection from ${cardType.lowercase()}"
    }

    /**
     * Protection from a creature subtype.
     * "Protection from Goblins"
     */
    @SerialName("ProtectionFromCreatureSubtype")
    @Serializable
    data class ProtectionFromCreatureSubtype(val subtype: String) : KeywordAbility {
        override val description: String = "Protection from ${subtype}s"
    }

    /**
     * Protection from everything.
     * "Protection from everything"
     */
    @SerialName("ProtectionFromEverything")
    @Serializable
    data object ProtectionFromEverything : KeywordAbility {
        override val description: String = "Protection from everything"
    }

    /**
     * Protection from each of the controller's opponents (Rule 702.16e).
     * Damage from sources controlled by an opponent is prevented; the permanent
     * can't be targeted by an opponent's spells/abilities, can't be blocked by
     * creatures controlled by an opponent, and can't be enchanted/equipped by
     * Auras or Equipment controlled by an opponent.
     */
    @SerialName("ProtectionFromEachOpponent")
    @Serializable
    data object ProtectionFromEachOpponent : KeywordAbility {
        override val keyword: Keyword = Keyword.PROTECTION_FROM_EACH_OPPONENT
        override val description: String = "Protection from each opponent"
    }

    // =========================================================================
    // Numeric Keywords
    // =========================================================================

    /**
     * Annihilator N.
     * "Annihilator 2" - Whenever this creature attacks, defending player sacrifices 2 permanents.
     */
    @SerialName("Annihilator")
    @Serializable
    data class Annihilator(val count: Int) : KeywordAbility {
        override val description: String = "Annihilator $count"
    }

    /**
     * Bushido N.
     * "Bushido 2" - Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.
     */
    @SerialName("Bushido")
    @Serializable
    data class Bushido(val count: Int) : KeywordAbility {
        override val description: String = "Bushido $count"
    }

    /**
     * Rampage N.
     * "Rampage 2" - Whenever this creature becomes blocked, it gets +2/+2 until end of turn
     * for each creature blocking it beyond the first.
     */
    @SerialName("Rampage")
    @Serializable
    data class Rampage(val count: Int) : KeywordAbility {
        override val description: String = "Rampage $count"
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

    /**
     * Absorb N.
     * "Absorb 2" - If a source would deal damage to this creature, prevent 2 of that damage.
     */
    @SerialName("Absorb")
    @Serializable
    data class Absorb(val count: Int) : KeywordAbility {
        override val description: String = "Absorb $count"
    }

    /**
     * Afflict N.
     * "Afflict 2" - Whenever this creature becomes blocked, defending player loses 2 life.
     */
    @SerialName("Afflict")
    @Serializable
    data class Afflict(val count: Int) : KeywordAbility {
        override val description: String = "Afflict $count"
    }

    /**
     * Toxic N.
     * "Toxic 1" - Players dealt combat damage by this creature also get N poison counters.
     */
    @SerialName("Toxic")
    @Serializable
    data class Toxic(val count: Int) : KeywordAbility {
        override val keyword: Keyword = Keyword.TOXIC
        override val description: String = "Toxic $count"
    }

    // =========================================================================
    // Vehicle/Artifact Keywords
    // =========================================================================

    /**
     * Crew N.
     * "Crew 3" - Tap any number of creatures you control with total power 3 or more:
     * This Vehicle becomes an artifact creature until end of turn.
     */
    @SerialName("Crew")
    @Serializable
    data class Crew(val power: Int) : KeywordAbility {
        override val description: String = "Crew $power"
    }

    /**
     * Modular N.
     * "Modular 2" - This creature enters the battlefield with 2 +1/+1 counters on it.
     * When it dies, you may put its +1/+1 counters on target artifact creature.
     */
    @SerialName("Modular")
    @Serializable
    data class Modular(val count: Int) : KeywordAbility {
        override val description: String = "Modular $count"
    }

    // =========================================================================
    // Counter-Based Keywords
    // =========================================================================

    /**
     * Fading N.
     * "Fading 3" - This permanent enters the battlefield with 3 fade counters on it.
     * At the beginning of your upkeep, remove a fade counter. If you can't, sacrifice it.
     */
    @SerialName("Fading")
    @Serializable
    data class Fading(val count: Int) : KeywordAbility {
        override val description: String = "Fading $count"
    }

    /**
     * Vanishing N.
     * "Vanishing 3" - This permanent enters the battlefield with 3 time counters on it.
     * At the beginning of your upkeep, remove a time counter. When the last is removed, sacrifice it.
     */
    @SerialName("Vanishing")
    @Serializable
    data class Vanishing(val count: Int) : KeywordAbility {
        override val description: String = "Vanishing $count"
    }

    /**
     * Renown N.
     * "Renown 2" - When this creature deals combat damage to a player, if it isn't renowned,
     * put 2 +1/+1 counters on it and it becomes renowned.
     */
    @SerialName("Renown")
    @Serializable
    data class Renown(val count: Int) : KeywordAbility {
        override val description: String = "Renown $count"
    }

    /**
     * Fabricate N.
     * "Fabricate 2" - When this creature enters the battlefield, put 2 +1/+1 counters on it,
     * or create 2 1/1 colorless Servo artifact creature tokens.
     */
    @SerialName("Fabricate")
    @Serializable
    data class Fabricate(val count: Int) : KeywordAbility {
        override val description: String = "Fabricate $count"
    }

    /**
     * Tribute N.
     * "Tribute 3" - As this creature enters the battlefield, an opponent may put 3 +1/+1 counters on it.
     */
    @SerialName("Tribute")
    @Serializable
    data class Tribute(val count: Int) : KeywordAbility {
        override val description: String = "Tribute $count"
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
     * Cycling with a mana cost.
     * "Cycling {2}" - {2}, Discard this card: Draw a card.
     */
    @SerialName("Cycling")
    @Serializable
    data class Cycling(val cost: ManaCost) : KeywordAbility {
        override val description: String = "Cycling $cost"
    }

    /**
     * Typecycling (e.g., Slivercycling, Wizardcycling).
     * "Slivercycling {3}" - {3}, Discard this card: Search your library for a Sliver card,
     * reveal it, put it into your hand, then shuffle.
     */
    @SerialName("Typecycling")
    @Serializable
    data class Typecycling(@SerialName("subtype") val type: String, val cost: ManaCost) : KeywordAbility {
        override val description: String = "${type}cycling $cost"
    }

    /**
     * Basic landcycling.
     * "Basic landcycling {1}{U}" - {1}{U}, Discard this card: Search your library for a basic land
     * card, reveal it, put it into your hand, then shuffle.
     *
     * Shares the typecycling infrastructure — only the search filter differs (any basic land card
     * rather than cards of a specific subtype).
     */
    @SerialName("BasicLandcycling")
    @Serializable
    data class BasicLandcycling(val cost: ManaCost) : KeywordAbility {
        override val description: String = "Basic landcycling $cost"
    }

    // =========================================================================
    // Kicker Variants
    // =========================================================================

    /**
     * Kicker with a mana cost.
     * "Kicker {2}{B}"
     */
    @SerialName("Kicker")
    @Serializable
    data class Kicker(val cost: ManaCost) : KeywordAbility {
        override val description: String = "Kicker $cost"
    }

    /**
     * Kicker with a non-mana additional cost (e.g., sacrifice a creature).
     * "Kicker—Sacrifice a creature."
     */
    @SerialName("KickerWithAdditionalCost")
    @Serializable
    data class KickerWithAdditionalCost(val cost: AdditionalCost) : KeywordAbility {
        override val description: String = "Kicker—${cost.description}"
    }

    /**
     * Multikicker with a mana cost.
     * "Multikicker {1}{W}"
     */
    @SerialName("Multikicker")
    @Serializable
    data class Multikicker(val cost: ManaCost) : KeywordAbility {
        override val description: String = "Multikicker $cost"
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
    // Offspring
    // =========================================================================

    /**
     * Offspring with a mana cost.
     * "Offspring {2}" - You may pay an additional {2} as you cast this spell.
     * If you do, when this creature enters, create a 1/1 token copy of it.
     *
     * Mechanically identical to Kicker in terms of cost payment. The engine
     * reuses the kicker infrastructure (wasKicked flag) for Offspring.
     */
    @SerialName("Offspring")
    @Serializable
    data class Offspring(val cost: ManaCost) : KeywordAbility {
        override val keyword: Keyword = Keyword.OFFSPRING
        override val description: String = "Offspring $cost"
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
        fun ward(cost: String): KeywordAbility = WardMana(ManaCost.parse(cost))

        /**
         * Create Ward with life cost.
         */
        fun wardLife(amount: Int): KeywordAbility = WardLife(amount)

        /**
         * Create Ward with discard cost.
         */
        fun wardDiscard(count: Int = 1, random: Boolean = false): KeywordAbility =
            WardDiscard(count, random)

        /**
         * Create Hexproof from a color.
         */
        fun hexproofFrom(color: Color): KeywordAbility = HexproofFromColor(color)

        /**
         * Create Protection from a color.
         */
        fun protectionFrom(color: Color): KeywordAbility = ProtectionFromColor(color)

        /**
         * Create Protection from multiple colors.
         */
        fun protectionFrom(vararg colors: Color): KeywordAbility =
            ProtectionFromColors(colors.toSet())

        /**
         * Create Protection from a creature subtype.
         */
        fun protectionFromSubtype(subtype: String): KeywordAbility =
            ProtectionFromCreatureSubtype(subtype)

        /**
         * Create Cycling with mana cost from string.
         */
        fun cycling(cost: String): KeywordAbility = Cycling(ManaCost.parse(cost))

        /**
         * Create Basic landcycling with mana cost from string.
         */
        fun basicLandcycling(cost: String): KeywordAbility = BasicLandcycling(ManaCost.parse(cost))

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
         * Create Offspring with mana cost from string.
         */
        fun offspring(cost: String): KeywordAbility = Offspring(ManaCost.parse(cost))

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
        fun toxic(count: Int): KeywordAbility = Toxic(count)
    }
}
