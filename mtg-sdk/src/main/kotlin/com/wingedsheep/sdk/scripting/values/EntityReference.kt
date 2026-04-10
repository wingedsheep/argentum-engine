package com.wingedsheep.sdk.scripting.values

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lightweight sealed interface representing "which entity to read a property from."
 *
 * Intentionally slim — not reusing [com.wingedsheep.sdk.scripting.targets.EffectTarget] which
 * has 15+ variants for effect resolution. EntityReference only needs the small set of
 * "read one entity's numeric property" use cases.
 */
@Serializable
sealed interface EntityReference {
    val description: String

    /** The ability's source permanent. */
    @SerialName("Source")
    @Serializable
    data object Source : EntityReference {
        override val description: String = "it"
    }

    /** A cast-time target by index. */
    @SerialName("Target")
    @Serializable
    data class Target(val index: Int = 0) : EntityReference {
        override val description: String = "target"
    }

    /** A creature sacrificed as cost, by index. */
    @SerialName("Sacrificed")
    @Serializable
    data class Sacrificed(val index: Int = 0) : EntityReference {
        override val description: String = "the sacrificed creature"
    }

    /** The entity that caused the trigger to fire. */
    @SerialName("Triggering")
    @Serializable
    data object Triggering : EntityReference {
        override val description: String = "the triggering creature"
    }

    /** A permanent tapped as cost, by index. */
    @SerialName("TappedAsCost")
    @Serializable
    data class TappedAsCost(val index: Int = 0) : EntityReference {
        override val description: String = "the tapped creature"
    }

    /** The entity being modified by a continuous effect during state projection. */
    @SerialName("AffectedEntity")
    @Serializable
    data object AffectedEntity : EntityReference {
        override val description: String = "it"
    }
}
