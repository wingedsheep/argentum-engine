package com.wingedsheep.sdk.scripting.filters.unified

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ObjectFilterBuilder
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Where a [GroupFilter] applies. Most filters scan the battlefield (`Battlefield`),
 * but some refer to a specific permanent relative to the source — the source itself
 * (`Self`), the creature this Aura/Equipment is attached to (`AttachedTo`), the source
 * together with the creature it's soulbond-paired with (`SoulbondPair`), or a
 * pre-bound entity (`Specific`).
 */
@Serializable
sealed interface Scope {
    @SerialName("Battlefield")
    @Serializable
    data object Battlefield : Scope

    @SerialName("AttachedTo")
    @Serializable
    data object AttachedTo : Scope

    @SerialName("Self")
    @Serializable
    data object Self : Scope

    /**
     * The source **and** the creature it is soulbond-paired with (CR 702.95b) — the "both
     * creatures" / "each of those creatures" of a soulbond payoff (Lightning Mauler's "as long as
     * this creature is paired with another creature, both creatures have haste").
     *
     * Resolves to the empty set while the source is unpaired, which is what makes the payoff's
     * "as long as … paired" clause self-enforcing: no separate condition gate is needed, and the
     * bonus switches off the instant CR 702.95e breaks the pair.
     */
    @SerialName("SoulbondPair")
    @Serializable
    data object SoulbondPair : Scope

    @SerialName("Specific")
    @Serializable
    data class Specific(val entityId: EntityId) : Scope
}

/**
 * Filter for selecting groups of permanents (for mass effects).
 * Used for effects like "all creatures", "creatures you control", etc.
 *
 * This replaces CreatureGroupFilter, CreatureDamageFilter, and similar types
 * with a unified approach.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * // All creatures
 * GroupFilter.AllCreatures
 *
 * // All creatures you control
 * GroupFilter.AllCreaturesYouControl
 *
 * // Other creatures you control (excluding source)
 * GroupFilter.OtherCreaturesYouControl
 *
 * // Custom: all tapped creatures with flying
 * GroupFilter(GameObjectFilter.Creature.tapped().withKeyword(Keyword.FLYING))
 *
 * // All nonwhite creatures opponents control
 * GroupFilter(GameObjectFilter.Creature.notColor(Color.WHITE).opponentControls())
 * ```
 */
@Serializable
data class GroupFilter(
    val baseFilter: GameObjectFilter,
    val excludeSelf: Boolean = false,
    /**
     * When non-null, additionally filters entities by the creature subtype stored
     * in `EffectContext.chosenValues[chosenSubtypeKey]` at resolution time.
     * Used for "creatures of the chosen type" patterns with pipeline effects.
     */
    val chosenSubtypeKey: String? = null,
    /**
     * When true, excludes the spell/ability's first chosen target from the group. Use this for
     * "each OTHER X with the same controller" relative to a *targeted* permanent (as opposed to
     * [excludeSelf], which excludes the resolving source). Example: Fear, Fire, Foes! — "1 damage
     * to each other creature with the same controller" excludes the target creature itself.
     */
    val excludeTarget: Boolean = false,
    /**
     * When true, excludes the trigger's *triggering entity* from the group — the [GroupFilter]
     * counterpart of [TargetFilter.excludeTriggeringEntity]. Use for "each other X" relative to the
     * object the event is about rather than the source or a target. Example: Kusari-Gama — "deals
     * that much damage to each other creature defending player controls" leaves out the blocking
     * creature the equipped creature damaged (a damage trigger binds the recipient as its
     * triggering entity).
     */
    val excludeTriggeringEntity: Boolean = false,
    /**
     * Where this filter applies. Defaults to scanning the battlefield. Use
     * [Scope.Self] for "this creature", [Scope.AttachedTo] for "enchanted/equipped
     * creature", or [Scope.Specific] for a bound entity. When non-Battlefield,
     * [baseFilter] / [excludeSelf] are ignored by the projection layer.
     */
    val scope: Scope = Scope.Battlefield
) : TextReplaceable<GroupFilter>, ObjectFilterBuilder<GroupFilter> {
    val description: String
        get() = buildDescription()

    private fun buildDescription(): String = when (scope) {
        is Scope.Self -> "this creature"
        is Scope.AttachedTo -> "enchanted/equipped creature"
        is Scope.SoulbondPair -> "this creature and the creature it's paired with"
        is Scope.Specific -> "the chosen permanent"
        is Scope.Battlefield -> buildString {
            append("all ")
            if (excludeSelf || excludeTarget || excludeTriggeringEntity) append("other ")
            append(baseFilter.description)
            if (!baseFilter.description.endsWith("s")) {
                append("s")  // Pluralize simple types
            }
        }
    }

    // =============================================================================
    // Pre-built Creature Groups
    // =============================================================================

    companion object {
        /** All creatures on the battlefield */
        val AllCreatures = GroupFilter(GameObjectFilter.Creature)

        /** All creatures you control */
        val AllCreaturesYouControl = GroupFilter(GameObjectFilter.Creature.youControl())

        /** All creatures opponents control */
        val AllCreaturesOpponentsControl = GroupFilter(GameObjectFilter.Creature.opponentControls())

        /** All other creatures (excluding source) */
        val AllOtherCreatures = GroupFilter(GameObjectFilter.Creature, excludeSelf = true)

        /** All other creatures you control */
        val OtherCreaturesYouControl = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)

        /** All other tapped creatures you control (for effects like Adept Watershaper) */
        val OtherTappedCreaturesYouControl = GroupFilter(GameObjectFilter.Creature.youControl().tapped(), excludeSelf = true)

        /** All attacking creatures */
        val AttackingCreatures = GroupFilter(GameObjectFilter.Creature.attacking())

        /** All blocking creatures */
        val BlockingCreatures = GroupFilter(GameObjectFilter.Creature.blocking())

        /** All tapped creatures */
        val TappedCreatures = GroupFilter(GameObjectFilter.Creature.tapped())

        /** All untapped creatures */
        val UntappedCreatures = GroupFilter(GameObjectFilter.Creature.untapped())

        // =============================================================================
        // Pre-built Permanent Groups
        // =============================================================================

        /** All permanents */
        val AllPermanents = GroupFilter(GameObjectFilter.Permanent)

        /** All permanents you control */
        val AllPermanentsYouControl = GroupFilter(GameObjectFilter.Permanent.youControl())

        /** All nonland permanents */
        val AllNonlandPermanents = GroupFilter(GameObjectFilter.NonlandPermanent)

        /** All artifacts */
        val AllArtifacts = GroupFilter(GameObjectFilter.Artifact)

        /** All enchantments */
        val AllEnchantments = GroupFilter(GameObjectFilter.Enchantment)

        /** All planeswalkers you control */
        val PlaneswalkersYouControl = GroupFilter(GameObjectFilter.Planeswalker.youControl())

        /** All lands */
        val AllLands = GroupFilter(GameObjectFilter.Land)

        // =============================================================================
        // Convenience Builders for Common Destroy-All Patterns
        // =============================================================================

        /** All lands with a specific subtype (e.g., "Destroy all Islands") */
        fun allLandsWithSubtype(subtype: Subtype) = GroupFilter(GameObjectFilter.Land.withSubtype(subtype))

        /** All creatures with a specific subtype — the adjectival form (e.g., "Goblin creatures get +3/+0") */
        fun allCreaturesWithSubtype(subtype: String) = GroupFilter(GameObjectFilter.Creature.withSubtype(subtype))

        /**
         * All permanents with a specific subtype — the bare-noun form (e.g., "Destroy all Goblins",
         * "Bats you control get +1/+0"). A bare creature-type noun names every permanent with that
         * subtype, not only creatures; [allCreaturesWithSubtype] is for "<Type> creatures" only.
         */
        fun allPermanentsWithSubtype(subtype: String) = GroupFilter(GameObjectFilter.Permanent.withSubtype(subtype))

        /**
         * All creatures of the creature type chosen at resolution time.
         * The chosen type is read from `EffectContext.chosenValues[key]`.
         *
         * Pair with `ChooseOptionEffect(CREATURE_TYPE, storeAs = key)` upstream in a pipeline.
         */
        fun ChosenSubtypeCreatures(key: String = "chosenCreatureType", excludeSelf: Boolean = false) =
            GroupFilter(GameObjectFilter.Creature, excludeSelf = excludeSelf, chosenSubtypeKey = key)

        // =============================================================================
        // Scope-based factories (replaces former StaticTarget cases)
        // =============================================================================

        /** "This creature" — the source permanent itself. */
        fun source() = GroupFilter(GameObjectFilter.Permanent, scope = Scope.Self)

        /** "Enchanted/equipped creature" — the creature this Aura/Equipment is attached to. */
        fun attachedCreature() = GroupFilter(GameObjectFilter.Permanent, scope = Scope.AttachedTo)

        /**
         * "Both creatures" of a soulbond pair — the source plus the creature it's paired with
         * (CR 702.95b), and nothing at all while the source is unpaired. The affected set for
         * every soulbond payoff static.
         */
        fun soulbondPair() = GroupFilter(GameObjectFilter.Permanent, scope = Scope.SoulbondPair)

        /** A specific pre-bound entity. */
        fun specific(entityId: EntityId) =
            GroupFilter(GameObjectFilter.Permanent, scope = Scope.Specific(entityId))
    }

    // =============================================================================
    // Builders — the predicate builders come from [ObjectFilterBuilder]; these are GroupFilter's own
    // =============================================================================

    override fun mapObjectFilter(transform: (GameObjectFilter) -> GameObjectFilter) =
        copy(baseFilter = transform(baseFilter))

    /** Exclude the source permanent */
    fun other() = copy(excludeSelf = true)

    /** Exclude the spell/ability's first chosen target (for "each other X" relative to a target) */
    fun otherThanTarget() = copy(excludeTarget = true)

    /** Exclude the trigger's triggering entity (for "each other X" relative to the event's object) */
    fun otherThanTriggeringEntity() = copy(excludeTriggeringEntity = true)

    override fun applyTextReplacement(replacer: TextReplacer): GroupFilter {
        val newBase = baseFilter.applyTextReplacement(replacer)
        return if (newBase !== baseFilter) copy(baseFilter = newBase) else this
    }
}
