package com.wingedsheep.sdk.targeting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
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

/**
 * Filter for permanent targeting restrictions.
 *
 * Filters are pure data - validation is handled by TargetValidator.
 */
@Deprecated(
    "Use TargetFilter for targeting or GroupFilter for mass effects",
    level = DeprecationLevel.WARNING
)
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

    @Serializable
    data object CreatureOrLand : PermanentTargetFilter {
        override val description: String = "creature or land"
    }

    @Serializable
    data class WithColor(val color: Color) : PermanentTargetFilter {
        override val description: String = color.displayName.lowercase()
    }

    @Serializable
    data class WithSubtype(val subtype: Subtype) : PermanentTargetFilter {
        override val description: String = subtype.value
    }

    @Serializable
    data class And(val filters: List<PermanentTargetFilter>) : PermanentTargetFilter {
        override val description: String = filters.joinToString(" ") { it.description }
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

/**
 * Target card in a graveyard.
 *
 * @param count Maximum number of targets
 * @param optional If true, allows 0 targets ("up to X" style targeting)
 * @param filter Restrictions on which cards can be targeted.
 *        Use TargetFilter.CreatureInYourGraveyard or .ownedByYou() to restrict to your graveyard.
 */
@Serializable
data class TargetCardInGraveyard(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: TargetFilter = TargetFilter.CardInGraveyard
) : TargetRequirement {
    override val description: String = buildString {
        append("target ")
        val filterDesc = filter.description.takeIf { filter != TargetFilter.CardInGraveyard }
        if (filterDesc != null && filterDesc.isNotEmpty()) {
            append(filterDesc)
            append(" ")
        }
        append("card in a graveyard")
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

    @Serializable
    data class CreatureOrSorcery(val dummy: Unit = Unit) : SpellTargetFilter {
        override val description: String = "creature or sorcery"
    }

    /**
     * Target spell with a specific mana value.
     * "spell with mana value 2"
     */
    @Serializable
    data class WithManaValue(val manaValue: Int) : SpellTargetFilter {
        override val description: String = "spell with mana value $manaValue"
    }

    /**
     * Target spell with mana value at most N.
     * "spell with mana value 3 or less"
     */
    @Serializable
    data class WithManaValueAtMost(val manaValue: Int) : SpellTargetFilter {
        override val description: String = "spell with mana value $manaValue or less"
    }

    /**
     * Target spell with mana value at least N.
     * "spell with mana value 4 or greater"
     */
    @Serializable
    data class WithManaValueAtLeast(val manaValue: Int) : SpellTargetFilter {
        override val description: String = "spell with mana value $manaValue or greater"
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
