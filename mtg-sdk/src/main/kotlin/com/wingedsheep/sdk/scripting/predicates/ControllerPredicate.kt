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

    /**
     * Controlled by the active player (the player whose turn it is). Useful for
     * "each player's upkeep, do X to permanents that player controls" patterns,
     * where the upkeep player is the active player (Temporal Distortion).
     */
    @SerialName("ControlledByActivePlayer")
    @Serializable
    data object ControlledByActivePlayer : ControllerPredicate {
        override val description: String = "the active player controls"
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

    /**
     * Owned by the targeted player (the spell/ability's "target player").
     *
     * The owner sibling of [ControlledByTargetPlayer]. Use this for the "all artifacts target
     * player **owns**" wording (Hurkyl's Recall, Drafna's Restoration): once control of a
     * permanent has changed, "owns" ≠ "controls", and the spell may target either player, so
     * neither [OwnedByYou]/[OwnedByOpponent] (fixed relative to the caster) nor
     * [ControlledByTargetPlayer] (control, not ownership) captures it. Matches against the
     * card's immutable `ownerId`, so it works for permanents on the battlefield regardless of
     * who controls them, and for cards in other zones (graveyard/exile) that have no controller.
     */
    @SerialName("OwnedByTargetPlayer")
    @Serializable
    data object OwnedByTargetPlayer : ControllerPredicate {
        override val description: String = "target player owns"
    }

    // =============================================================================
    // Composite / Logical Combinators
    // =============================================================================
    // Mirror StatePredicate's And/Or/Not. These let one filter express heterogeneous
    // controller/owner relationships ("you own but don't control", "you or the targeted
    // player controls") without falling back to the recursive anyOf union.

    @SerialName("ControllerAnd")
    @Serializable
    data class And(val predicates: List<ControllerPredicate>) : ControllerPredicate {
        override val description: String = predicates.joinToString(" and ") { it.description }
    }

    @SerialName("ControllerOr")
    @Serializable
    data class Or(val predicates: List<ControllerPredicate>) : ControllerPredicate {
        override val description: String = predicates.joinToString(" or ") { it.description }
    }

    @SerialName("ControllerNot")
    @Serializable
    data class Not(val predicate: ControllerPredicate) : ControllerPredicate {
        override val description: String = "not ${predicate.description}"
    }
}

/**
 * Structural fold for evaluating a [ControllerPredicate] tree against site-specific leaf
 * semantics. The combinators ([ControllerPredicate.And] / [ControllerPredicate.Or] /
 * [ControllerPredicate.Not]) recurse here; every leaf predicate is delegated to [leaf].
 *
 * The engine evaluates controller predicates in several contexts with different controller
 * notions (live projected state, zone-change last-known-controller snapshots, grant-provider
 * fast paths). Each site supplies only its own leaf semantics and shares the combinator
 * logic through this fold, so a composed predicate behaves consistently everywhere.
 *
 * **Unsupported leaves.** A site that can't evaluate a leaf kind returns `null` ("unknown"),
 * NOT `true` — unknowns propagate through the combinators with three-valued (Kleene) logic
 * and only resolve to "don't constrain" (match) at the root. This keeps "the site ignores
 * predicates it can't see" consistent under negation: with a boolean leaf fallback,
 * `Not(unsupported)` would flip the fail-open into rejecting everything.
 */
fun ControllerPredicate.evaluateWith(leaf: (ControllerPredicate) -> Boolean?): Boolean =
    evaluateOrUnknown(leaf) ?: true

private fun ControllerPredicate.evaluateOrUnknown(leaf: (ControllerPredicate) -> Boolean?): Boolean? = when (this) {
    is ControllerPredicate.And -> {
        val branches = predicates.map { it.evaluateOrUnknown(leaf) }
        if (false in branches) false else if (null in branches) null else true
    }
    is ControllerPredicate.Or -> {
        val branches = predicates.map { it.evaluateOrUnknown(leaf) }
        if (true in branches) true else if (null in branches) null else false
    }
    is ControllerPredicate.Not -> predicate.evaluateOrUnknown(leaf)?.let { !it }
    else -> leaf(this)
}
