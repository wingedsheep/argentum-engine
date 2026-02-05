package com.wingedsheep.sdk.targeting

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TargetFilter
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
@Serializable
data class TargetOpponent(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "target opponent" else "target $count opponents"
}

// =============================================================================
// Creature Targeting
// =============================================================================

/**
 * Target creature (any creature on the battlefield).
 *
 * @param count Maximum number of targets
 * @param minCount Minimum number of targets (for "one or two" style targeting)
 * @param optional If true, allows 0 targets ("up to X" style targeting)
 * @param filter Restrictions on which creatures can be targeted
 */
@Serializable
data class TargetCreature(
    override val count: Int = 1,
    override val minCount: Int = count,
    override val optional: Boolean = false,
    val filter: TargetFilter = TargetFilter.Creature
) : TargetRequirement {
    override val description: String = buildString {
        if (optional) {
            append("up to ")
        } else if (minCount < count) {
            append("$minCount to ")
        }
        val filterDesc = filter.description.takeIf { filter != TargetFilter.Creature }
        if (filterDesc != null && filterDesc.isNotEmpty()) {
            append(filterDesc)
            append(" ")
        }
        append("target ")
        append(if (count == 1) "creature" else "$count creatures")
    }
}

// =============================================================================
// Permanent Targeting
// =============================================================================

/**
 * Target permanent (any permanent on the battlefield).
 */
@Serializable
data class TargetPermanent(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: TargetFilter = TargetFilter.Permanent
) : TargetRequirement {
    override val description: String = buildString {
        if (optional) append("up to ")
        append("target ")
        if (filter != TargetFilter.Permanent) {
            append(filter.description)
            append(" ")
        }
        append(if (count == 1) "permanent" else "$count permanents")
    }
}

// =============================================================================
// Combined Targeting
// =============================================================================

/**
 * "Any target" - can target any creature, player, or planeswalker.
 */
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
@Serializable
data class TargetCreatureOrPlaneswalker(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "target creature or planeswalker" else "$count targets (creatures or planeswalkers)"
}

// =============================================================================
// Card Targeting (other zones)
// =============================================================================

// =============================================================================
// Spell Targeting (on stack)
// =============================================================================

/**
 * Target spell on the stack.
 */
@Serializable
data class TargetSpell(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: TargetFilter = TargetFilter.SpellOnStack
) : TargetRequirement {
    override val description: String = buildString {
        append("target ")
        if (filter != TargetFilter.SpellOnStack) {
            append(filter.baseFilter.description)
            append(" ")
        }
        append("spell")
    }
}

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
@Serializable
data class TargetObject(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: TargetFilter
) : TargetRequirement {
    override val description: String = buildString {
        if (optional) append("up to ")
        append("target ")
        append(filter.description)
    }
}

// =============================================================================
// Special Targeting
// =============================================================================

/**
 * Target another target (for modal spells, redirection, etc.).
 * E.g., "Change the target of target spell with a single target"
 */
@Serializable
data class TargetOther(
    val baseRequirement: TargetRequirement,
    val excludeSourceId: EntityId? = null
) : TargetRequirement {
    override val description: String = "target other ${baseRequirement.description}"
    override val count: Int = baseRequirement.count
    override val optional: Boolean = baseRequirement.optional
}
