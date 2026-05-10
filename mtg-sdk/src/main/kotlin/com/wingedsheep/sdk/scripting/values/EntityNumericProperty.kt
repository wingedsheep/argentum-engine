package com.wingedsheep.sdk.scripting.values

import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A numeric property that can be read from an entity.
 *
 * Richer than [CardNumericProperty] — handles parameterized properties (counter type)
 * and entity-specific properties (blocker count, attachment count) that only make sense
 * when reading from a specific entity rather than aggregating over a group.
 */
@Serializable
sealed interface EntityNumericProperty {
    val description: String

    @SerialName("Power")
    @Serializable
    data object Power : EntityNumericProperty {
        override val description: String = "power"
    }

    @SerialName("Toughness")
    @Serializable
    data object Toughness : EntityNumericProperty {
        override val description: String = "toughness"
    }

    @SerialName("ManaValue")
    @Serializable
    data object ManaValue : EntityNumericProperty {
        override val description: String = "mana value"
    }

    @SerialName("CounterCount")
    @Serializable
    data class CounterCount(val counterType: CounterTypeFilter) : EntityNumericProperty {
        override val description: String = "the number of ${counterType.description} counters"
    }

    @SerialName("AttachmentCount")
    @Serializable
    data object AttachmentCount : EntityNumericProperty {
        override val description: String = "the number of Auras and Equipment attached"
    }

    @SerialName("BlockerCount")
    @Serializable
    data object BlockerCount : EntityNumericProperty {
        override val description: String = "the number of creatures blocking"
    }

    /**
     * The number of distinct subtypes this entity has, read from projected state when
     * available (so layer-4 type-changing effects, including Changeling, are honored).
     *
     * Note: this counts every subtype string on the entity, not only creature types.
     * For creature cards without type-changing effects to non-creature subtypes (the
     * common case) this matches CR 205.3m's "creature types"; cards that gain artifact
     * or vehicle subtypes will be over-counted.
     */
    @SerialName("SubtypeCount")
    @Serializable
    data object SubtypeCount : EntityNumericProperty {
        override val description: String = "the number of its subtypes"
    }
}
