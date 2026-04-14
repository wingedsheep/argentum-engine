package com.wingedsheep.sdk.scripting.predicates

import com.wingedsheep.sdk.scripting.targets.EffectTarget
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

    /**
     * Controlled by the player referenced by an explicit [EffectTarget].
     *
     * Use this when a filter needs to scope to a specific named/bound player target
     * picked earlier in the same spell or ability — e.g., modal Commands where one
     * mode says "creatures target player controls". Keeps the target→filter wiring
     * explicit instead of relying on implicit "first player target" inference.
     */
    @SerialName("ControlledByReferencedPlayer")
    @Serializable
    data class ControlledByReferencedPlayer(val target: EffectTarget) : ControllerPredicate {
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
