package com.wingedsheep.sdk.scripting.effects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a spending restriction on mana produced by an ability.
 * When mana has a restriction, it can only be spent to cast spells that satisfy the restriction.
 * Restricted mana is spent preferentially when the spell is eligible.
 */
@Serializable
sealed interface ManaRestriction {
    val description: String

    /**
     * "Spend this mana only to cast instant or sorcery spells."
     */
    @SerialName("InstantOrSorceryOnly")
    @Serializable
    data object InstantOrSorceryOnly : ManaRestriction {
        override val description: String = "Spend this mana only to cast instant or sorcery spells"
    }

    /**
     * "Spend this mana only to cast kicked spells."
     */
    @SerialName("KickedSpellsOnly")
    @Serializable
    data object KickedSpellsOnly : ManaRestriction {
        override val description: String = "Spend this mana only to cast kicked spells"
    }

    /**
     * "Spend this mana only to cast creature spells with mana value 4 or greater
     * or creature spells with {X} in their mana costs."
     */
    @SerialName("CreatureMV4OrXCost")
    @Serializable
    data object CreatureMV4OrXCost : ManaRestriction {
        override val description: String =
            "Spend this mana only to cast creature spells with mana value 4 or greater or creature spells with {X} in their mana costs"
    }

    /**
     * "Spend this mana only to cast creature spells."
     */
    @SerialName("CreatureSpellsOnly")
    @Serializable
    data object CreatureSpellsOnly : ManaRestriction {
        override val description: String = "Spend this mana only to cast creature spells"
    }

    /**
     * "Spend this mana only to cast a spell of the specified subtype
     * or to activate an ability of a source of that subtype."
     *
     * The [subtype] is baked at the moment the mana is added to the pool
     * (e.g., read from the source's ChosenCreatureTypeComponent), so the
     * restriction becomes self-contained and serializable.
     */
    @SerialName("SubtypeSpellsOrAbilitiesOnly")
    @Serializable
    data class SubtypeSpellsOrAbilitiesOnly(val subtype: String) : ManaRestriction {
        override val description: String =
            "Spend this mana only to cast a spell of the chosen type or activate an ability of a source of the chosen type"
    }
}
