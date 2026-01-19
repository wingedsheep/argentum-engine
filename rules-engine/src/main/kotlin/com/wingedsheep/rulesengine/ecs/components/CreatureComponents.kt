package com.wingedsheep.rulesengine.ecs.components

 import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.Component
import kotlinx.serialization.Serializable

/**
 * Creature-specific components for power/toughness and keywords.
 */

/**
 * Power and toughness for creatures.
 *
 * Tracks both base values (from card definition) and modifiers (from effects).
 * Does NOT include counters - those are handled separately by CountersComponent.
 *
 * The final P/T calculation is:
 * - Power = basePower + powerModifier + plusOnePlusOneCounters
 * - Toughness = baseToughness + toughnessModifier + plusOnePlusOneCounters - minusOneMinusOneCounters
 *
 * @property basePower Power from the card definition
 * @property baseToughness Toughness from the card definition
 * @property powerModifier Temporary power modifiers from effects (e.g., Giant Growth)
 * @property toughnessModifier Temporary toughness modifiers from effects
 */
@Serializable
data class PTComponent(
    val basePower: Int,
    val baseToughness: Int,
    val powerModifier: Int = 0,
    val toughnessModifier: Int = 0
) : Component {
    /**
     * Current power (base + modifier, not including counters).
     */
    val currentPower: Int get() = basePower + powerModifier

    /**
     * Current toughness (base + modifier, not including counters).
     */
    val currentToughness: Int get() = baseToughness + toughnessModifier

    /**
     * Apply a power/toughness modification.
     */
    fun modify(power: Int = 0, toughness: Int = 0): PTComponent =
        copy(
            powerModifier = powerModifier + power,
            toughnessModifier = toughnessModifier + toughness
        )

    /**
     * Clear all modifiers (for set-to effects).
     */
    fun clearModifiers(): PTComponent =
        copy(powerModifier = 0, toughnessModifier = 0)

    companion object {
        /**
         * Create a PTComponent from base stats.
         */
        fun of(power: Int, toughness: Int): PTComponent =
            PTComponent(basePower = power, baseToughness = toughness)
    }
}

/**
 * Keyword modifications for a card.
 *
 * Tracks keywords that have been added or removed from the card's base keywords
 * (defined in CardDefinition). Also tracks temporary keywords that last until
 * end of turn.
 *
 * The effective keywords are:
 * (baseKeywords + added + temporary) - removed
 *
 * @property added Keywords permanently added to this card
 * @property removed Keywords permanently removed from this card
 * @property temporary Keywords added until end of turn
 */
@Serializable
data class KeywordsComponent(
    val added: Set<Keyword> = emptySet(),
    val removed: Set<Keyword> = emptySet(),
    val temporary: Set<Keyword> = emptySet()
) : Component {
    /**
     * Calculate effective keywords given base keywords from definition.
     */
    fun effectiveKeywords(baseKeywords: Set<Keyword>): Set<Keyword> =
        (baseKeywords + added + temporary) - removed

    /**
     * Add a permanent keyword.
     */
    fun addKeyword(keyword: Keyword): KeywordsComponent =
        copy(added = added + keyword)

    /**
     * Remove a keyword permanently.
     */
    fun removeKeyword(keyword: Keyword): KeywordsComponent =
        copy(removed = removed + keyword)

    /**
     * Add a temporary keyword (until end of turn).
     */
    fun addTemporaryKeyword(keyword: Keyword): KeywordsComponent =
        copy(temporary = temporary + keyword)

    /**
     * Clear all temporary keywords (at end of turn).
     */
    fun clearTemporary(): KeywordsComponent =
        copy(temporary = emptySet())

    /**
     * Check if any modifications exist.
     */
    val hasModifications: Boolean
        get() = added.isNotEmpty() || removed.isNotEmpty() || temporary.isNotEmpty()

    companion object {
        val EMPTY = KeywordsComponent()
    }
}

/**
 * Represents what a permanent has protection from.
 *
 * Protection prevents:
 * - Damage from sources with the characteristic
 * - Enchanting/Equipping by objects with the characteristic
 * - Blocking by creatures with the characteristic
 * - Targeting by spells/abilities with the characteristic
 *
 * This sealed interface covers the common protection types in MTG.
 */
@Serializable
sealed interface ProtectionFrom {
    /**
     * Protection from a specific color (e.g., "Protection from Green").
     */
    @Serializable
    data class FromColor(val color: Color) : ProtectionFrom

    /**
     * Protection from a specific card type (e.g., "Protection from artifacts").
     */
    @Serializable
    data class FromCardType(val type: CardType) : ProtectionFrom

    /**
     * Protection from a specific creature type (e.g., "Protection from Goblins").
     */
    @Serializable
    data class FromCreatureType(val subtype: Subtype) : ProtectionFrom

    /**
     * Protection from everything (e.g., Progenitus).
     */
    @Serializable
    data object FromEverything : ProtectionFrom

    /**
     * Protection from multicolored (e.g., "Protection from multicolored").
     */
    @Serializable
    data object FromMulticolored : ProtectionFrom

    /**
     * Protection from monocolored (e.g., "Protection from monocolored").
     */
    @Serializable
    data object FromMonocolored : ProtectionFrom
}

/**
 * Component that tracks what a permanent has protection from.
 *
 * A permanent can have multiple protections simultaneously
 * (e.g., "Protection from white and from blue").
 *
 * Usage:
 * - Add protection: `container.with(ProtectionComponent(setOf(ProtectionFrom.FromColor(Color.GREEN))))`
 * - Check protection: `container.get<ProtectionComponent>()?.protections?.any { it matches source }`
 *
 * @property protections Set of protection characteristics
 */
@Serializable
data class ProtectionComponent(
    val protections: Set<ProtectionFrom>
) : Component {
    /**
     * Add additional protection.
     */
    fun addProtection(protection: ProtectionFrom): ProtectionComponent =
        copy(protections = protections + protection)

    /**
     * Remove a protection.
     */
    fun removeProtection(protection: ProtectionFrom): ProtectionComponent =
        copy(protections = protections - protection)

    /**
     * Check if this has protection from a specific color.
     */
    fun hasProtectionFromColor(color: Color): Boolean =
        protections.any { protection ->
            when (protection) {
                is ProtectionFrom.FromColor -> protection.color == color
                is ProtectionFrom.FromEverything -> true
                else -> false
            }
        }

    /**
     * Check if this has protection from a specific card type.
     */
    fun hasProtectionFromCardType(type: CardType): Boolean =
        protections.any { protection ->
            when (protection) {
                is ProtectionFrom.FromCardType -> protection.type == type
                is ProtectionFrom.FromEverything -> true
                else -> false
            }
        }

    /**
     * Check if this has protection from a specific creature type.
     */
    fun hasProtectionFromCreatureType(subtype: Subtype): Boolean =
        protections.any { protection ->
            when (protection) {
                is ProtectionFrom.FromCreatureType -> protection.subtype == subtype
                is ProtectionFrom.FromEverything -> true
                else -> false
            }
        }

    /**
     * Check if this has protection from everything.
     */
    fun hasProtectionFromEverything(): Boolean =
        protections.any { it is ProtectionFrom.FromEverything }

    /**
     * Check if this has protection from a source with the given colors.
     * Handles multicolored and monocolored protection.
     */
    fun hasProtectionFromColors(colors: Set<Color>): Boolean {
        if (colors.isEmpty()) return false

        return protections.any { protection ->
            when (protection) {
                is ProtectionFrom.FromColor -> protection.color in colors
                is ProtectionFrom.FromMulticolored -> colors.size > 1
                is ProtectionFrom.FromMonocolored -> colors.size == 1
                is ProtectionFrom.FromEverything -> true
                else -> false
            }
        }
    }

    companion object {
        /**
         * Create a protection component for a single color.
         */
        fun fromColor(color: Color): ProtectionComponent =
            ProtectionComponent(setOf(ProtectionFrom.FromColor(color)))

        /**
         * Create a protection component for multiple colors.
         */
        fun fromColors(vararg colors: Color): ProtectionComponent =
            ProtectionComponent(colors.map { ProtectionFrom.FromColor(it) }.toSet())

        /**
         * Create a protection component from everything.
         */
        fun fromEverything(): ProtectionComponent =
            ProtectionComponent(setOf(ProtectionFrom.FromEverything))
    }
}
