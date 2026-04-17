package com.wingedsheep.sdk.scripting.targets

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
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
sealed interface TargetRequirement : TextReplaceable<TargetRequirement> {
    val description: String
    val count: Int get() = 1  // Maximum targets
    val minCount: Int get() = count  // Minimum targets (defaults to count)
    val optional: Boolean get() = false  // If true, minCount becomes 0
    /** Named identifier for this target requirement. When set, enables BoundVariable resolution. */
    val id: String? get() = null

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
    override val optional: Boolean = false,
    override val id: String? = null,
    private val descriptionOverride: String? = null
) : TargetRequirement {
    override val description: String = descriptionOverride
        ?: if (count == 1) "target player" else "target $count players"
    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement = this
}

/**
 * Target opponent only.
 */
@SerialName("TargetOpponent")
@Serializable
data class TargetOpponent(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = if (count == 1) "target opponent" else "target $count opponents"
    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement = this
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
    filter: TargetFilter = TargetFilter.Creature,
    id: String? = null
): TargetObject = TargetObject(count = count, minCount = minCount, optional = optional, filter = filter, id = id)

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
    filter: TargetFilter = TargetFilter.Permanent,
    id: String? = null
): TargetObject = TargetObject(count = count, optional = optional, filter = filter, id = id)

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
    override val minCount: Int = count,
    override val optional: Boolean = false,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = if (count == 1) "any target" else "$count targets"
    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement = this
}

/**
 * "Target creature or player" - classic burn spell targeting.
 */
@SerialName("TargetCreatureOrPlayer")
@Serializable
data class TargetCreatureOrPlayer(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = if (count == 1) "target creature or player" else "$count targets (creatures or players)"
    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement = this
}

/**
 * "Target opponent or planeswalker" - can target an opponent or any planeswalker.
 */
@SerialName("TargetOpponentOrPlaneswalker")
@Serializable
data class TargetOpponentOrPlaneswalker(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = if (count == 1) "target opponent or planeswalker" else "$count targets (opponents or planeswalkers)"
    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement = this
}

/**
 * "Target player or planeswalker" - can target any player or any planeswalker.
 */
@SerialName("TargetPlayerOrPlaneswalker")
@Serializable
data class TargetPlayerOrPlaneswalker(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = if (count == 1) "target player or planeswalker" else "$count targets (players or planeswalkers)"
    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement = this
}

/**
 * "Target creature or planeswalker" - modern burn spell targeting.
 */
@SerialName("TargetCreatureOrPlaneswalker")
@Serializable
data class TargetCreatureOrPlaneswalker(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = if (count == 1) "target creature or planeswalker" else "$count targets (creatures or planeswalkers)"
    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement = this
}

// =============================================================================
// Spell or Permanent Targeting
// =============================================================================

/**
 * "Target spell or permanent" - can target spells on the stack or permanents
 * on the battlefield.
 *
 * Used by text-changing effects like Artificial Evolution and bounce-to-library
 * effects like Swat Away ("target spell or creature").
 *
 * The [permanentFilter] restricts which permanents are valid. When null, any
 * permanent may be targeted. For "target spell or creature", pass
 * [GameObjectFilter.Creature].
 */
@SerialName("TargetSpellOrPermanent")
@Serializable
data class TargetSpellOrPermanent(
    override val count: Int = 1,
    override val optional: Boolean = false,
    override val id: String? = null,
    val permanentFilter: GameObjectFilter? = null
) : TargetRequirement {
    override val description: String = run {
        val permanentNoun = permanentFilter?.description ?: "permanent"
        if (count == 1) "target spell or $permanentNoun"
        else "$count target spells or ${permanentNoun}s"
    }
    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement {
        val newFilter = permanentFilter?.applyTextReplacement(replacer)
        return if (newFilter !== permanentFilter) copy(permanentFilter = newFilter) else this
    }
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
    filter: TargetFilter = TargetFilter.SpellOnStack,
    id: String? = null
): TargetObject = TargetObject(count = count, optional = optional, filter = filter, id = id)

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
    val filter: TargetFilter,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = if (id != null) {
        buildString {
            if (optional) append("up to ")
            else if (minCount < count) append("$minCount to ")
            append(id)
        }
    } else {
        buildString {
            if (optional) append("up to ")
            else if (minCount < count) append("$minCount to ")
            append("target ")
            append(if (count == 1) filter.description else "$count ${filter.description}s")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
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
    val excludeSourceId: EntityId? = null,
    override val id: String? = null
) : TargetRequirement {
    override val description: String = "target other ${baseRequirement.description}"
    override val count: Int = baseRequirement.count
    override val optional: Boolean = baseRequirement.optional

    override fun applyTextReplacement(replacer: TextReplacer): TargetRequirement {
        val newBase = baseRequirement.applyTextReplacement(replacer)
        return if (newBase !== baseRequirement) copy(baseRequirement = newBase) else this
    }
}

/**
 * Create a copy of this TargetRequirement with the given id set.
 * Used by the DSL to stamp an id onto requirements passed to target(name, requirement).
 */
fun TargetRequirement.withId(name: String): TargetRequirement = when (this) {
    is TargetPlayer -> copy(id = name)
    is TargetOpponent -> copy(id = name)
    is AnyTarget -> copy(id = name)
    is TargetCreatureOrPlayer -> copy(id = name)
    is TargetOpponentOrPlaneswalker -> copy(id = name)
    is TargetPlayerOrPlaneswalker -> copy(id = name)
    is TargetCreatureOrPlaneswalker -> copy(id = name)
    is TargetSpellOrPermanent -> copy(id = name)
    is TargetObject -> copy(id = name)
    is TargetOther -> copy(id = name)
}
