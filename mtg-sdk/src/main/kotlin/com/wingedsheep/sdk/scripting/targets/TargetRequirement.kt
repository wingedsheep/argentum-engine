package com.wingedsheep.sdk.scripting.targets

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Defines what can be targeted by a spell or ability.
 * Each TargetRequirement specifies the valid targets and any restrictions.
 *
 * TargetRequirements are data objects - validation is handled by TargetValidator
 * which checks targets against GameState.
 *
 * Target count semantics:
 * - count = maximum number of targets
 * - minCount = minimum number of targets (defaults to count if not specified)
 * - optional = if true, minCount becomes 0 ("up to X" targets)
 *
 * Examples:
 * - "target creature": count=1, minCount=1
 * - "up to two target creatures": count=2, optional=true (minCount becomes 0)
 * - "one or two target creatures": count=2, minCount=1
 */
@Serializable
sealed interface TargetRequirement {
    val description: String
    val count: Int get() = 1  // Maximum targets
    val minCount: Int get() = count  // Minimum targets (defaults to count)
    val optional: Boolean get() = false  // If true, minCount becomes 0

    /** Effective minimum after considering optional flag */
    val effectiveMinCount: Int get() = if (optional) 0 else minCount
}

// =============================================================================
// Player Targeting
// =============================================================================

/**
 * Target player (any player).
 */
@SerialName("TargetPlayer")
@Serializable
data class TargetPlayer(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "target player" else "target $count players"
}

/**
 * Target opponent only.
 */
@SerialName("TargetOpponent")
@Serializable
data class TargetOpponent(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "target opponent" else "target $count opponents"
}

// =============================================================================
// Creature Targeting (factory function — returns TargetObject)
// =============================================================================

/**
 * Target creature (any creature on the battlefield).
 * Factory function that returns a TargetObject with appropriate defaults.
 */
fun TargetCreature(
    count: Int = 1,
    minCount: Int = count,
    optional: Boolean = false,
    filter: TargetFilter = TargetFilter.Creature
): TargetObject = TargetObject(count = count, minCount = minCount, optional = optional, filter = filter)

// =============================================================================
// Permanent Targeting (factory function — returns TargetObject)
// =============================================================================

/**
 * Target permanent (any permanent on the battlefield).
 * Factory function that returns a TargetObject with appropriate defaults.
 */
fun TargetPermanent(
    count: Int = 1,
    optional: Boolean = false,
    filter: TargetFilter = TargetFilter.Permanent
): TargetObject = TargetObject(count = count, optional = optional, filter = filter)

// =============================================================================
// Combined Targeting
// =============================================================================

/**
 * "Any target" - can target any creature, player, or planeswalker.
 */
@SerialName("AnyTarget")
@Serializable
data class AnyTarget(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "any target" else "$count targets"
}

/**
 * "Target creature or player" - classic burn spell targeting.
 */
@SerialName("TargetCreatureOrPlayer")
@Serializable
data class TargetCreatureOrPlayer(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "target creature or player" else "$count targets (creatures or players)"
}

/**
 * "Target creature or planeswalker" - modern burn spell targeting.
 */
@SerialName("TargetCreatureOrPlaneswalker")
@Serializable
data class TargetCreatureOrPlaneswalker(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "target creature or planeswalker" else "$count targets (creatures or planeswalkers)"
}

// =============================================================================
// Spell or Permanent Targeting
// =============================================================================

/**
 * "Target spell or permanent" - can target spells on the stack or permanents
 * on the battlefield.
 *
 * Used by text-changing effects like Artificial Evolution.
 */
@SerialName("TargetSpellOrPermanent")
@Serializable
data class TargetSpellOrPermanent(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "target spell or permanent" else "$count target spells or permanents"
}

// =============================================================================
// Card Targeting (other zones)
// =============================================================================

// =============================================================================
// Spell Targeting (factory function — returns TargetObject)
// =============================================================================

/**
 * Target spell on the stack.
 * Factory function that returns a TargetObject with appropriate defaults.
 */
fun TargetSpell(
    count: Int = 1,
    optional: Boolean = false,
    filter: TargetFilter = TargetFilter.SpellOnStack
): TargetObject = TargetObject(count = count, optional = optional, filter = filter)

// =============================================================================
// Generic Object Targeting
// =============================================================================

/**
 * Target any game object matching a filter.
 * Generalizes zone-specific targeting — the TargetFilter's zone field
 * determines which zone to look in.
 *
 * @param count Maximum number of targets
 * @param optional If true, allows 0 targets ("up to X" style targeting)
 * @param filter Determines what can be targeted and in which zone
 */
@SerialName("TargetObject")
@Serializable
data class TargetObject(
    override val count: Int = 1,
    override val minCount: Int = count,
    override val optional: Boolean = false,
    val filter: TargetFilter
) : TargetRequirement {
    override val description: String = buildString {
        if (optional) {
            append("up to ")
        } else if (minCount < count) {
            append("$minCount to ")
        }
        append("target ")
        append(if (count == 1) filter.description else "$count ${filter.description}s")
    }
}

// =============================================================================
// Special Targeting
// =============================================================================

/**
 * Target another target (for modal spells, redirection, etc.).
 * E.g., "Change the target of target spell with a single target"
 */
@SerialName("TargetOther")
@Serializable
data class TargetOther(
    val baseRequirement: TargetRequirement,
    val excludeSourceId: EntityId? = null
) : TargetRequirement {
    override val description: String = "target other ${baseRequirement.description}"
    override val count: Int = baseRequirement.count
    override val optional: Boolean = baseRequirement.optional
}
