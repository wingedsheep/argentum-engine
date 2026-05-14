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
     * "Spend this mana only to cast spells with mana value 4 or greater."
     */
    @SerialName("SpellsMV4OrGreater")
    @Serializable
    data object SpellsMV4OrGreater : ManaRestriction {
        override val description: String =
            "Spend this mana only to cast spells with mana value 4 or greater"
    }

    /**
     * "Spend this mana only to cast a spell of the specified subtype
     * (or, when [creatureOnly] is false, also to activate an ability of a source of that subtype)."
     *
     * The [subtype] is baked at the moment the mana is added to the pool
     * (e.g., read from the source's ChosenCreatureTypeComponent), so the
     * restriction becomes self-contained and serializable.
     *
     * When [creatureOnly] is true, only creature spells of that subtype satisfy the
     * restriction — abilities of objects of the subtype don't (Cavern of Souls shape).
     * When false, the restriction also allows activated abilities of sources of the
     * subtype (Unclaimed Territory shape).
     *
     * Any rider side-effect of spending this mana (e.g. "the spell can't be countered")
     * is carried on the [com.wingedsheep.sdk.scripting.effects.ManaSpellRider] set
     * attached to the produced mana entry, not on this type.
     */
    @SerialName("SubtypeSpellsOrAbilitiesOnly")
    @Serializable
    data class SubtypeSpellsOrAbilitiesOnly(
        val subtype: String,
        val creatureOnly: Boolean = false
    ) : ManaRestriction {
        override val description: String = if (creatureOnly) {
            "Spend this mana only to cast a creature spell of the chosen type"
        } else {
            "Spend this mana only to cast a spell of the chosen type or activate an ability of a source of the chosen type"
        }
    }

    @SerialName("CastFromExileOnly")
    @Serializable
    data object CastFromExileOnly : ManaRestriction {
        override val description: String = "Spend this mana only to cast spells from exile"
    }

    /**
     * "Spend this mana only to cast an artifact spell."
     */
    @SerialName("ArtifactSpellsOnly")
    @Serializable
    data object ArtifactSpellsOnly : ManaRestriction {
        override val description: String = "Spend this mana only to cast an artifact spell"
    }

    /**
     * "Spend this mana only to activate abilities of artifact sources."
     *
     * An "artifact source" is any object with the card type artifact, in any zone
     * (battlefield, hand, graveyard, exile, library, command, stack).
     */
    @SerialName("ArtifactSourceAbilitiesOnly")
    @Serializable
    data object ArtifactSourceAbilitiesOnly : ManaRestriction {
        override val description: String = "Spend this mana only to activate abilities of artifact sources"
    }
}
