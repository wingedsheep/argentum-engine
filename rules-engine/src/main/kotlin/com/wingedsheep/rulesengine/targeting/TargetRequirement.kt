package com.wingedsheep.rulesengine.targeting

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EntityId
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
    val filter: CreatureTargetFilter = CreatureTargetFilter.Any
) : TargetRequirement {
    override val description: String = buildString {
        if (optional) {
            append("up to ")
        } else if (minCount < count) {
            append("$minCount to ")
        }
        if (filter != CreatureTargetFilter.Any) {
            append(filter.description)
            append(" ")
        }
        append("target ")
        append(if (count == 1) "creature" else "$count creatures")
    }
}

/**
 * Filter for creature targeting restrictions.
 *
 * Filters are pure data - validation is handled by TargetValidator.
 */
@Serializable
sealed interface CreatureTargetFilter {
    val description: String

    @Serializable
    data object Any : CreatureTargetFilter {
        override val description: String = ""
    }

    @Serializable
    data object YouControl : CreatureTargetFilter {
        override val description: String = "creature you control"
    }

    @Serializable
    data object OpponentControls : CreatureTargetFilter {
        override val description: String = "creature an opponent controls"
    }

    @Serializable
    data object Attacking : CreatureTargetFilter {
        override val description: String = "attacking"
    }

    @Serializable
    data object Blocking : CreatureTargetFilter {
        override val description: String = "blocking"
    }

    @Serializable
    data object Tapped : CreatureTargetFilter {
        override val description: String = "tapped"
    }

    @Serializable
    data object Untapped : CreatureTargetFilter {
        override val description: String = "untapped"
    }

    @Serializable
    data class WithKeyword(val keyword: Keyword) : CreatureTargetFilter {
        override val description: String = keyword.name.lowercase().replace('_', ' ')
    }

    @Serializable
    data class WithoutKeyword(val keyword: Keyword) : CreatureTargetFilter {
        override val description: String = "without ${keyword.name.lowercase().replace('_', ' ')}"
    }

    @Serializable
    data class WithColor(val color: Color) : CreatureTargetFilter {
        override val description: String = color.displayName.lowercase()
    }

    @Serializable
    data class WithPowerAtMost(val maxPower: Int) : CreatureTargetFilter {
        override val description: String = "with power $maxPower or less"
    }

    @Serializable
    data class WithPowerAtLeast(val minPower: Int) : CreatureTargetFilter {
        override val description: String = "with power $minPower or greater"
    }

    @Serializable
    data class WithToughnessAtMost(val maxToughness: Int) : CreatureTargetFilter {
        override val description: String = "with toughness $maxToughness or less"
    }

    @Serializable
    data class WithSubtype(val subtype: Subtype) : CreatureTargetFilter {
        override val description: String = subtype.value
    }

    @Serializable
    data class And(val filters: List<CreatureTargetFilter>) : CreatureTargetFilter {
        override val description: String = filters.joinToString(" ") { it.description }
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
    val filter: PermanentTargetFilter = PermanentTargetFilter.Any
) : TargetRequirement {
    override val description: String = buildString {
        append("target ")
        if (filter != PermanentTargetFilter.Any) {
            append(filter.description)
            append(" ")
        }
        append(if (count == 1) "permanent" else "$count permanents")
    }
}

/**
 * Filter for permanent targeting restrictions.
 *
 * Filters are pure data - validation is handled by TargetValidator.
 */
@Serializable
sealed interface PermanentTargetFilter {
    val description: String

    @Serializable
    data object Any : PermanentTargetFilter {
        override val description: String = ""
    }

    @Serializable
    data object YouControl : PermanentTargetFilter {
        override val description: String = "permanent you control"
    }

    @Serializable
    data object OpponentControls : PermanentTargetFilter {
        override val description: String = "permanent an opponent controls"
    }

    @Serializable
    data object Creature : PermanentTargetFilter {
        override val description: String = "creature"
    }

    @Serializable
    data object Artifact : PermanentTargetFilter {
        override val description: String = "artifact"
    }

    @Serializable
    data object Enchantment : PermanentTargetFilter {
        override val description: String = "enchantment"
    }

    @Serializable
    data object Land : PermanentTargetFilter {
        override val description: String = "land"
    }

    @Serializable
    data object NonCreature : PermanentTargetFilter {
        override val description: String = "noncreature"
    }

    @Serializable
    data object NonLand : PermanentTargetFilter {
        override val description: String = "nonland"
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

// =============================================================================
// Card Targeting (other zones)
// =============================================================================

/**
 * Target card in a graveyard.
 */
@Serializable
data class TargetCardInGraveyard(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: GraveyardCardFilter = GraveyardCardFilter.Any
) : TargetRequirement {
    override val description: String = buildString {
        append("target ")
        if (filter != GraveyardCardFilter.Any) {
            append(filter.description)
            append(" ")
        }
        append("card in a graveyard")
    }
}

/**
 * Filter for graveyard card targeting.
 *
 * Filters are pure data - validation is handled by TargetValidator.
 */
@Serializable
sealed interface GraveyardCardFilter {
    val description: String

    @Serializable
    data object Any : GraveyardCardFilter {
        override val description: String = ""
    }

    @Serializable
    data object Creature : GraveyardCardFilter {
        override val description: String = "creature"
    }

    @Serializable
    data object Instant : GraveyardCardFilter {
        override val description: String = "instant"
    }

    @Serializable
    data object Sorcery : GraveyardCardFilter {
        override val description: String = "sorcery"
    }

    @Serializable
    data object InstantOrSorcery : GraveyardCardFilter {
        override val description: String = "instant or sorcery"
    }
}

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
    val filter: SpellTargetFilter = SpellTargetFilter.Any
) : TargetRequirement {
    override val description: String = buildString {
        append("target ")
        if (filter != SpellTargetFilter.Any) {
            append(filter.description)
            append(" ")
        }
        append("spell")
    }
}

/**
 * Filter for spell targeting.
 *
 * Filters are pure data - validation is handled by TargetValidator.
 */
@Serializable
sealed interface SpellTargetFilter {
    val description: String

    @Serializable
    data object Any : SpellTargetFilter {
        override val description: String = ""
    }

    @Serializable
    data object Creature : SpellTargetFilter {
        override val description: String = "creature"
    }

    @Serializable
    data object Noncreature : SpellTargetFilter {
        override val description: String = "noncreature"
    }

    @Serializable
    data object Instant : SpellTargetFilter {
        override val description: String = "instant"
    }

    @Serializable
    data object Sorcery : SpellTargetFilter {
        override val description: String = "sorcery"
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
