package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
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
     * Simple keyword with no parameters.
     * Examples: Flying, Trample, Haste
     */
    @Serializable
    data class Simple(val keyword: Keyword) : KeywordAbility {
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
    @Serializable
    data class WardMana(val cost: ManaCost) : KeywordAbility {
        override val description: String = "Ward $cost"
    }

    /**
     * Ward with a life cost.
     * "Ward—Pay 2 life."
     */
    @Serializable
    data class WardLife(val amount: Int) : KeywordAbility {
        override val description: String = "Ward—Pay $amount life"
    }

    /**
     * Ward with a discard cost.
     * "Ward—Discard a card."
     */
    @Serializable
    data class WardDiscard(val count: Int = 1, val random: Boolean = false) : KeywordAbility {
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
    @Serializable
    data class WardSacrifice(val filter: GameObjectFilter) : KeywordAbility {
        override val description: String = "Ward—Sacrifice a ${filter.description}"
    }

    // =========================================================================
    // Protection Variants
    // =========================================================================

    /**
     * Protection from a color.
     * "Protection from blue"
     */
    @Serializable
    data class ProtectionFromColor(val color: Color) : KeywordAbility {
        override val description: String = "Protection from ${color.displayName.lowercase()}"
    }

    /**
     * Protection from multiple colors.
     * "Protection from white and from blue"
     */
    @Serializable
    data class ProtectionFromColors(val colors: Set<Color>) : KeywordAbility {
        override val description: String = "Protection from " +
                colors.joinToString(" and from ") { it.displayName.lowercase() }
    }

    /**
     * Protection from a card type.
     * "Protection from creatures"
     */
    @Serializable
    data class ProtectionFromCardType(val cardType: String) : KeywordAbility {
        override val description: String = "Protection from ${cardType.lowercase()}"
    }

    /**
     * Protection from everything.
     * "Protection from everything"
     */
    @Serializable
    data object ProtectionFromEverything : KeywordAbility {
        override val description: String = "Protection from everything"
    }

    // =========================================================================
    // Numeric Keywords
    // =========================================================================

    /**
     * Annihilator N.
     * "Annihilator 2" - Whenever this creature attacks, defending player sacrifices 2 permanents.
     */
    @Serializable
    data class Annihilator(val count: Int) : KeywordAbility {
        override val description: String = "Annihilator $count"
    }

    /**
     * Bushido N.
     * "Bushido 2" - Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.
     */
    @Serializable
    data class Bushido(val count: Int) : KeywordAbility {
        override val description: String = "Bushido $count"
    }

    /**
     * Rampage N.
     * "Rampage 2" - Whenever this creature becomes blocked, it gets +2/+2 until end of turn
     * for each creature blocking it beyond the first.
     */
    @Serializable
    data class Rampage(val count: Int) : KeywordAbility {
        override val description: String = "Rampage $count"
    }

    /**
     * Flanking.
     * "Flanking" - Whenever this creature becomes blocked by a creature without flanking,
     * the blocking creature gets -1/-1 until end of turn.
     */
    @Serializable
    data object Flanking : KeywordAbility {
        override val description: String = "Flanking"
    }

    /**
     * Absorb N.
     * "Absorb 2" - If a source would deal damage to this creature, prevent 2 of that damage.
     */
    @Serializable
    data class Absorb(val count: Int) : KeywordAbility {
        override val description: String = "Absorb $count"
    }

    /**
     * Afflict N.
     * "Afflict 2" - Whenever this creature becomes blocked, defending player loses 2 life.
     */
    @Serializable
    data class Afflict(val count: Int) : KeywordAbility {
        override val description: String = "Afflict $count"
    }

    // =========================================================================
    // Vehicle/Artifact Keywords
    // =========================================================================

    /**
     * Crew N.
     * "Crew 3" - Tap any number of creatures you control with total power 3 or more:
     * This Vehicle becomes an artifact creature until end of turn.
     */
    @Serializable
    data class Crew(val power: Int) : KeywordAbility {
        override val description: String = "Crew $power"
    }

    /**
     * Modular N.
     * "Modular 2" - This creature enters the battlefield with 2 +1/+1 counters on it.
     * When it dies, you may put its +1/+1 counters on target artifact creature.
     */
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
    @Serializable
    data class Fading(val count: Int) : KeywordAbility {
        override val description: String = "Fading $count"
    }

    /**
     * Vanishing N.
     * "Vanishing 3" - This permanent enters the battlefield with 3 time counters on it.
     * At the beginning of your upkeep, remove a time counter. When the last is removed, sacrifice it.
     */
    @Serializable
    data class Vanishing(val count: Int) : KeywordAbility {
        override val description: String = "Vanishing $count"
    }

    /**
     * Renown N.
     * "Renown 2" - When this creature deals combat damage to a player, if it isn't renowned,
     * put 2 +1/+1 counters on it and it becomes renowned.
     */
    @Serializable
    data class Renown(val count: Int) : KeywordAbility {
        override val description: String = "Renown $count"
    }

    /**
     * Fabricate N.
     * "Fabricate 2" - When this creature enters the battlefield, put 2 +1/+1 counters on it,
     * or create 2 1/1 colorless Servo artifact creature tokens.
     */
    @Serializable
    data class Fabricate(val count: Int) : KeywordAbility {
        override val description: String = "Fabricate $count"
    }

    /**
     * Tribute N.
     * "Tribute 3" - As this creature enters the battlefield, an opponent may put 3 +1/+1 counters on it.
     */
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
    @Serializable
    data class Affinity(val forType: CardType) : KeywordAbility {
        override val description: String = "Affinity for ${forType.displayName.lowercase()}s"
    }

    // =========================================================================
    // Cycling Variants
    // =========================================================================

    /**
     * Cycling with a mana cost.
     * "Cycling {2}" - {2}, Discard this card: Draw a card.
     */
    @Serializable
    data class Cycling(val cost: ManaCost) : KeywordAbility {
        override val description: String = "Cycling $cost"
    }

    /**
     * Typecycling (e.g., Slivercycling, Wizardcycling).
     * "Slivercycling {3}" - {3}, Discard this card: Search your library for a Sliver card,
     * reveal it, put it into your hand, then shuffle.
     */
    @Serializable
    data class Typecycling(val type: String, val cost: ManaCost) : KeywordAbility {
        override val description: String = "${type}cycling $cost"
    }

    // =========================================================================
    // Kicker Variants
    // =========================================================================

    /**
     * Kicker with a mana cost.
     * "Kicker {2}{B}"
     */
    @Serializable
    data class Kicker(val cost: ManaCost) : KeywordAbility {
        override val description: String = "Kicker $cost"
    }

    /**
     * Multikicker with a mana cost.
     * "Multikicker {1}{W}"
     */
    @Serializable
    data class Multikicker(val cost: ManaCost) : KeywordAbility {
        override val description: String = "Multikicker $cost"
    }

    // =========================================================================
    // Morph
    // =========================================================================

    /**
     * Morph with a mana cost.
     * "Morph {2}{U}" - You may cast this card face down as a 2/2 creature for {3}.
     * Turn it face up any time for its morph cost.
     */
    @Serializable
    data class Morph(val cost: ManaCost) : KeywordAbility {
        override val description: String = "Morph $cost"
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
         * Create Protection from a color.
         */
        fun protectionFrom(color: Color): KeywordAbility = ProtectionFromColor(color)

        /**
         * Create Protection from multiple colors.
         */
        fun protectionFrom(vararg colors: Color): KeywordAbility =
            ProtectionFromColors(colors.toSet())

        /**
         * Create Cycling with mana cost from string.
         */
        fun cycling(cost: String): KeywordAbility = Cycling(ManaCost.parse(cost))

        /**
         * Create Morph with mana cost from string.
         */
        fun morph(cost: String): KeywordAbility = Morph(ManaCost.parse(cost))
    }
}
