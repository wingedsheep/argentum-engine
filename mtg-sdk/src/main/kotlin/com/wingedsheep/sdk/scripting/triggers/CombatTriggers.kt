package com.wingedsheep.sdk.scripting.triggers

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.triggers.Trigger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Combat Triggers
// =============================================================================

/**
 * Triggers when this creature attacks.
 * "Whenever [this creature] attacks..."
 */
@SerialName("Attack")
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
@SerialName("YouAttack")
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
@SerialName("Block")
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
@SerialName("BecomesBlocked")
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
@SerialName("DealsDamage")
@Serializable
data class OnDealsDamage(
    val selfOnly: Boolean = true,
    val combatOnly: Boolean = false,
    val toPlayerOnly: Boolean = false,
    val toCreatureOnly: Boolean = false
) : Trigger {
    override val description: String = buildString {
        append(if (selfOnly) "Whenever this creature deals " else "Whenever a creature deals ")
        if (combatOnly) append("combat ")
        append("damage")
        if (toPlayerOnly) append(" to a player")
        if (toCreatureOnly) append(" to a creature")
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
@SerialName("CreatureDealsDamageToYou")
@Serializable
data object OnCreatureDealsDamageToYou : Trigger {
    override val description: String = "Whenever a creature deals damage to you"
}

/**
 * Triggers when any creature with the specified subtype deals combat damage to a player.
 * "Whenever a [subtype] deals combat damage to a player..."
 *
 * This is checked on an OBSERVER permanent (e.g., Cabal Slaver), not on the
 * creature dealing damage. The triggering player ID is set to the damaged player.
 */
@SerialName("CreatureWithSubtypeDealsCombatDamageToPlayer")
@Serializable
data class OnCreatureWithSubtypeDealsCombatDamageToPlayer(
    val subtype: Subtype
) : Trigger {
    override val description: String = "Whenever a ${subtype.value} deals combat damage to a player"
}

/**
 * Triggers when a creature deals damage to this permanent.
 * "Whenever a creature deals damage to [this creature]..."
 *
 * The triggering entity ID is set to the damage SOURCE creature,
 * enabling retaliation effects (e.g., Tephraderm deals damage back).
 */
@SerialName("DamagedByCreature")
@Serializable
data class OnDamagedByCreature(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "Whenever a creature deals damage to this creature"
    } else {
        "Whenever a creature deals damage to a creature"
    }
}

/**
 * Triggers when a spell (instant or sorcery) deals damage to this permanent.
 * "Whenever a spell deals damage to [this creature]..."
 *
 * The triggering entity ID is set to the damage SOURCE spell entity,
 * enabling effects that reference the spell's controller (e.g., Tephraderm).
 */
@SerialName("DamagedBySpell")
@Serializable
data class OnDamagedBySpell(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "Whenever a spell deals damage to this creature"
    } else {
        "Whenever a spell deals damage to a creature"
    }
}
