package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Subtype
import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of trigger conditions.
 * Triggers define WHEN an ability fires.
 */
@Serializable
sealed interface Trigger {
    /** Human-readable description of the trigger condition */
    val description: String
}

// =============================================================================
// Zone Change Triggers
// =============================================================================

/**
 * Triggers when a permanent enters the battlefield.
 * "When [this creature] enters the battlefield..."
 */
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

// =============================================================================
// Card Drawing Triggers
// =============================================================================

/**
 * Triggers when a player draws a card.
 * "Whenever you draw a card..." or "Whenever a player draws a card..."
 */
@Serializable
data class OnDraw(
    val controllerOnly: Boolean = true
) : Trigger {
    override val description: String = if (controllerOnly) {
        "Whenever you draw a card"
    } else {
        "Whenever a player draws a card"
    }
}

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
 * Triggers when this creature blocks or becomes blocked.
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

// =============================================================================
// Phase/Step Triggers
// =============================================================================

/**
 * Triggers at the beginning of upkeep.
 * "At the beginning of your upkeep..." or "At the beginning of each upkeep..."
 */
@Serializable
data class OnUpkeep(
    val controllerOnly: Boolean = true
) : Trigger {
    override val description: String = if (controllerOnly) {
        "At the beginning of your upkeep"
    } else {
        "At the beginning of each upkeep"
    }
}

/**
 * Triggers at the beginning of the end step.
 * "At the beginning of your end step..."
 */
@Serializable
data class OnEndStep(
    val controllerOnly: Boolean = true
) : Trigger {
    override val description: String = if (controllerOnly) {
        "At the beginning of your end step"
    } else {
        "At the beginning of each end step"
    }
}

/**
 * Triggers at the beginning of combat.
 * "At the beginning of combat on your turn..."
 */
@Serializable
data class OnBeginCombat(
    val controllerOnly: Boolean = true
) : Trigger {
    override val description: String = if (controllerOnly) {
        "At the beginning of combat on your turn"
    } else {
        "At the beginning of each combat"
    }
}

// =============================================================================
// Damage Triggers
// =============================================================================

/**
 * Triggers when this creature is dealt damage.
 * "Whenever [this creature] is dealt damage..."
 */
@Serializable
data class OnDamageReceived(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "Whenever this creature is dealt damage"
    } else {
        "Whenever a creature is dealt damage"
    }
}

// =============================================================================
// Spell Triggers
// =============================================================================

/**
 * Triggers when a spell is cast.
 * "Whenever you cast a spell..." or "Whenever a player casts a spell..."
 */
@Serializable
data class OnSpellCast(
    val controllerOnly: Boolean = true,
    val spellType: SpellTypeFilter = SpellTypeFilter.ANY,
    val manaValueAtLeast: Int? = null,
    val manaValueAtMost: Int? = null,
    val manaValueEquals: Int? = null
) : Trigger {
    override val description: String = buildString {
        append(if (controllerOnly) "Whenever you cast " else "Whenever a player casts ")
        append(when (spellType) {
            SpellTypeFilter.ANY -> "a spell"
            SpellTypeFilter.CREATURE -> "a creature spell"
            SpellTypeFilter.NONCREATURE -> "a noncreature spell"
            SpellTypeFilter.INSTANT_OR_SORCERY -> "an instant or sorcery spell"
        })
        manaValueEquals?.let { append(" with mana value $it") }
        manaValueAtLeast?.let { append(" with mana value $it or greater") }
        manaValueAtMost?.let { append(" with mana value $it or less") }
    }
}

@Serializable
enum class SpellTypeFilter {
    ANY,
    CREATURE,
    NONCREATURE,
    INSTANT_OR_SORCERY
}

// =============================================================================
// Main Phase Triggers
// =============================================================================

/**
 * Triggers at the beginning of the first main phase.
 * "At the beginning of your first main phase..."
 *
 * This is distinct from generic main phase triggers - it only triggers
 * on the pre-combat main phase, not the post-combat main phase.
 */
@Serializable
data class OnFirstMainPhase(
    val controllerOnly: Boolean = true
) : Trigger {
    override val description: String = if (controllerOnly) {
        "At the beginning of your first main phase"
    } else {
        "At the beginning of each player's first main phase"
    }
}

// =============================================================================
// Tap/Untap Triggers
// =============================================================================

/**
 * Triggers when a permanent becomes tapped.
 * "Whenever this creature becomes tapped..."
 */
@Serializable
data class OnBecomesTapped(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "Whenever this creature becomes tapped"
    } else {
        "Whenever a creature becomes tapped"
    }
}

/**
 * Triggers when a permanent becomes untapped.
 * "Whenever this creature becomes untapped..."
 */
@Serializable
data class OnBecomesUntapped(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "Whenever this creature becomes untapped"
    } else {
        "Whenever a creature becomes untapped"
    }
}

// =============================================================================
// Transform Triggers
// =============================================================================

/**
 * Triggers when a permanent transforms.
 * "When this creature transforms..."
 *
 * Can be filtered to only trigger when transforming into a specific face.
 */
@Serializable
data class OnTransform(
    val selfOnly: Boolean = true,
    val intoBackFace: Boolean? = null  // null = any transform, true = only when transforming to back, false = only when transforming to front
) : Trigger {
    override val description: String = buildString {
        append(if (selfOnly) "When this creature transforms" else "Whenever a permanent transforms")
        when (intoBackFace) {
            true -> append(" into its back face")
            false -> append(" into its front face")
            null -> { /* any transform */ }
        }
    }
}
