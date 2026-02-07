package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

// =============================================================================
// Combat Triggers
// =============================================================================

/**
 * Triggers when this creature attacks.
 * "Whenever [this creature] attacks..."
 */
@Serializable
data class OnAttack(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "Whenever this creature attacks"
    } else {
        "Whenever a creature attacks"
    }
}

/**
 * Triggers when you attack (declare attackers).
 * "Whenever you attack..."
 *
 * This triggers once per combat when you declare one or more attackers,
 * regardless of how many creatures attack. Different from OnAttack which
 * triggers for each creature that attacks.
 */
@Serializable
data class OnYouAttack(
    val minAttackers: Int = 1
) : Trigger {
    override val description: String = if (minAttackers > 1) {
        "Whenever you attack with $minAttackers or more creatures"
    } else {
        "Whenever you attack"
    }
}

/**
 * Triggers when this creature blocks.
 * "Whenever [this creature] blocks..."
 */
@Serializable
data class OnBlock(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "Whenever this creature blocks"
    } else {
        "Whenever a creature blocks"
    }
}

/**
 * Triggers when this creature becomes blocked.
 * "Whenever [this creature] becomes blocked..."
 *
 * This fires for the ATTACKER when it is assigned one or more blockers.
 * Different from OnBlock which fires for the BLOCKER.
 */
@Serializable
data class OnBecomesBlocked(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "Whenever this creature becomes blocked"
    } else {
        "Whenever a creature becomes blocked"
    }
}

/**
 * Triggers when this creature deals damage.
 * "Whenever [this creature] deals damage..."
 */
@Serializable
data class OnDealsDamage(
    val selfOnly: Boolean = true,
    val combatOnly: Boolean = false,
    val toPlayerOnly: Boolean = false
) : Trigger {
    override val description: String = buildString {
        append(if (selfOnly) "Whenever this creature deals " else "Whenever a creature deals ")
        if (combatOnly) append("combat ")
        append("damage")
        if (toPlayerOnly) append(" to a player")
    }
}

/**
 * Triggers when a creature deals damage to this permanent's controller.
 * "Whenever a creature deals damage to you..."
 *
 * Unlike OnDealsDamage (which is checked on the damage SOURCE creature),
 * this trigger is checked on the permanent that observes the damage.
 * The triggering entity ID is set to the damage SOURCE creature.
 */
@Serializable
data object OnCreatureDealsDamageToYou : Trigger {
    override val description: String = "Whenever a creature deals damage to you"
}
