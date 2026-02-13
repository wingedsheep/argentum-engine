package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Subtype
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Zone Change Triggers
// =============================================================================

/**
 * Triggers when a permanent enters the battlefield.
 * "When [this creature] enters the battlefield..."
 */
@SerialName("EntersBattlefield")
@Serializable
data class OnEnterBattlefield(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "When this enters the battlefield"
    } else {
        "Whenever a permanent enters the battlefield"
    }
}

/**
 * Triggers when another creature you control enters the battlefield.
 * "Whenever another creature you control enters the battlefield..."
 */
@SerialName("OtherCreatureEnters")
@Serializable
data class OnOtherCreatureEnters(
    val youControlOnly: Boolean = true
) : Trigger {
    override val description: String = if (youControlOnly) {
        "Whenever another creature you control enters the battlefield"
    } else {
        "Whenever another creature enters the battlefield"
    }
}

/**
 * Triggers when a permanent leaves the battlefield.
 * "When [this creature] leaves the battlefield..."
 */
@SerialName("LeavesBattlefield")
@Serializable
data class OnLeavesBattlefield(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "When this leaves the battlefield"
    } else {
        "Whenever a permanent leaves the battlefield"
    }
}

/**
 * Triggers when a creature dies (goes to graveyard from battlefield).
 * "When [this creature] dies..."
 */
@SerialName("Death")
@Serializable
data class OnDeath(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "When this creature dies"
    } else {
        "Whenever a creature dies"
    }
}

/**
 * Triggers when another creature with a specific subtype you control dies.
 * "Whenever another Goblin you control dies..."
 */
@SerialName("OtherCreatureWithSubtypeDies")
@Serializable
data class OnOtherCreatureWithSubtypeDies(
    val subtype: Subtype,
    val youControlOnly: Boolean = true
) : Trigger {
    override val description: String = if (youControlOnly) {
        "Whenever another ${subtype.value} you control dies"
    } else {
        "Whenever another ${subtype.value} dies"
    }
}

/**
 * Triggers when another creature with a specific subtype enters the battlefield.
 * "Whenever another Elf enters the battlefield, put a +1/+1 counter on this creature."
 */
@SerialName("OtherCreatureWithSubtypeEnters")
@Serializable
data class OnOtherCreatureWithSubtypeEnters(
    val subtype: Subtype,
    val youControlOnly: Boolean = false
) : Trigger {
    override val description: String = if (youControlOnly) {
        "Whenever another ${subtype.value} you control enters the battlefield"
    } else {
        "Whenever another ${subtype.value} enters the battlefield"
    }
}

/**
 * Triggers when any creature with a specific subtype enters the battlefield.
 * "Whenever a Beast enters the battlefield, you may draw a card."
 */
@SerialName("CreatureWithSubtypeEnters")
@Serializable
data class OnCreatureWithSubtypeEnters(
    val subtype: Subtype,
    val youControlOnly: Boolean = false
) : Trigger {
    override val description: String = if (youControlOnly) {
        "Whenever a ${subtype.value} you control enters the battlefield"
    } else {
        "Whenever a ${subtype.value} enters the battlefield"
    }
}
