package com.wingedsheep.sdk.scripting.predicates

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Predicates for matching controller/owner relationships.
 * These predicates check who controls or owns a permanent.
 *
 * ControllerPredicates are composed into GameObjectFilter for use in effects, targeting, and counting.
 */
@Serializable
sealed interface ControllerPredicate {
    val description: String

    // =============================================================================
    // Controller Predicates
    // =============================================================================

    /** Controlled by the ability's controller */
    @SerialName("ControlledByYou")
    @Serializable
    data object ControlledByYou : ControllerPredicate {
        override val description: String = "you control"
    }

    /** Controlled by an opponent of the ability's controller */
    @SerialName("ControlledByOpponent")
    @Serializable
    data object ControlledByOpponent : ControllerPredicate {
        override val description: String = "an opponent controls"
    }

    /** Controlled by any player (no restriction) */
    @SerialName("ControlledByAny")
    @Serializable
    data object ControlledByAny : ControllerPredicate {
        override val description: String = ""
    }

    /** Controlled by the targeted opponent */
    @SerialName("ControlledByTargetOpponent")
    @Serializable
    data object ControlledByTargetOpponent : ControllerPredicate {
        override val description: String = "target opponent controls"
    }

    /** Controlled by the targeted player */
    @SerialName("ControlledByTargetPlayer")
    @Serializable
    data object ControlledByTargetPlayer : ControllerPredicate {
        override val description: String = "target player controls"
    }

    // =============================================================================
    // Owner Predicates
    // =============================================================================

    /** Owned by the ability's controller */
    @SerialName("OwnedByYou")
    @Serializable
    data object OwnedByYou : ControllerPredicate {
        override val description: String = "you own"
    }

    /** Owned by an opponent */
    @SerialName("OwnedByOpponent")
    @Serializable
    data object OwnedByOpponent : ControllerPredicate {
        override val description: String = "an opponent owns"
    }
}
