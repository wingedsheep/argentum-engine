package com.wingedsheep.sdk.scripting.triggers

import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of trigger conditions.
 * Triggers define WHEN an ability fires.
 *
 * Trigger implementations are organized in the triggers/ subdirectory:
 * - ZoneChangeTriggers.kt - Enter/leave battlefield, death triggers
 * - CombatTriggers.kt - Attack, block, damage triggers
 * - PhaseTriggers.kt - Upkeep, end step, combat phase triggers
 * - SpellTriggers.kt - Draw, spell cast triggers
 * - StateTriggers.kt - Tap/untap, transform, morph triggers
 */
@Serializable
sealed interface Trigger {
    /** Human-readable description of the trigger condition */
    val description: String
}