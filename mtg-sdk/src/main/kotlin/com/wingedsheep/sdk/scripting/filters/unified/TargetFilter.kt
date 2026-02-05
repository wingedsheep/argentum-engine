package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import kotlinx.serialization.Serializable

/**
 * Filter for targeting game objects, with zone context.
 * Wraps GameObjectFilter and adds zone-specific targeting behavior.
 *
 * This replaces CreatureTargetFilter, PermanentTargetFilter, GraveyardCardFilter,
 * and SpellTargetFilter with a unified approach.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * // Target any creature
 * TargetFilter.Creature
 *
 * // Target tapped creature
 * TargetFilter.TappedCreature
 *
 * // Target creature in graveyard
 * TargetFilter.CreatureInGraveyard
 *
 * // Target spell on stack
 * TargetFilter.SpellOnStack
 *
 * // Custom filter: target black creature you control
 * TargetFilter(GameObjectFilter.Creature.withColor(Color.BLACK).youControl())
 * ```
 */
@Serializable
data class TargetFilter(
    val baseFilter: GameObjectFilter,
    val zone: Zone = Zone.Battlefield,
    val excludeSelf: Boolean = false
) {
    val description: String
        get() = buildDescription()

    private fun buildDescription(): String = buildString {
        if (excludeSelf) append("other ")
        append(baseFilter.description)
        if (zone != Zone.Battlefield) {
            append(" in ")
            append(zone.description)
        }
    }

    // =============================================================================
    // Pre-built Creature Targets (Battlefield)
    // =============================================================================

    companion object {
        /** Target any creature */
        val Creature = TargetFilter(GameObjectFilter.Creature)

        /** Target creature you control */
        val CreatureYouControl = TargetFilter(GameObjectFilter.Creature.youControl())

        /** Target creature an opponent controls */
        val CreatureOpponentControls = TargetFilter(GameObjectFilter.Creature.opponentControls())

        /** Target other creature (excluding source) */
        val OtherCreature = TargetFilter(GameObjectFilter.Creature, excludeSelf = true)

        /** Target other creature you control */
        val OtherCreatureYouControl = TargetFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)

        /** Target tapped creature */
        val TappedCreature = TargetFilter(GameObjectFilter.Creature.tapped())

        /** Target untapped creature */
        val UntappedCreature = TargetFilter(GameObjectFilter.Creature.untapped())

        /** Target attacking creature */
        val AttackingCreature = TargetFilter(GameObjectFilter.Creature.attacking())

        /** Target blocking creature */
        val BlockingCreature = TargetFilter(GameObjectFilter.Creature.blocking())

        /** Target attacking or blocking creature */
        val AttackingOrBlockingCreature = TargetFilter(GameObjectFilter.Creature.attackingOrBlocking())

        // =============================================================================
        // Pre-built Permanent Targets (Battlefield)
        // =============================================================================

        /** Target any permanent */
        val Permanent = TargetFilter(GameObjectFilter.Permanent)

        /** Target nonland permanent */
        val NonlandPermanent = TargetFilter(GameObjectFilter.NonlandPermanent)

        /** Target permanent you control */
        val PermanentYouControl = TargetFilter(GameObjectFilter.Permanent.youControl())

        /** Target nonland permanent an opponent controls */
        val NonlandPermanentOpponentControls = TargetFilter(GameObjectFilter.NonlandPermanent.opponentControls())

        /** Target artifact */
        val Artifact = TargetFilter(GameObjectFilter.Artifact)

        /** Target enchantment */
        val Enchantment = TargetFilter(GameObjectFilter.Enchantment)

        /** Target land */
        val Land = TargetFilter(GameObjectFilter.Land)

        /** Target planeswalker */
        val Planeswalker = TargetFilter(GameObjectFilter.Planeswalker)

        // =============================================================================
        // Pre-built Graveyard Targets
        // =============================================================================

        /** Target any card in a graveyard */
        val CardInGraveyard = TargetFilter(GameObjectFilter.Any, zone = Zone.Graveyard)

        /** Target creature card in a graveyard */
        val CreatureInGraveyard = TargetFilter(GameObjectFilter.Creature, zone = Zone.Graveyard)

        /** Target creature card in your graveyard */
        val CreatureInYourGraveyard = TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.Graveyard)

        /** Target instant or sorcery card in a graveyard */
        val InstantOrSorceryInGraveyard = TargetFilter(GameObjectFilter.InstantOrSorcery, zone = Zone.Graveyard)

        // =============================================================================
        // Pre-built Stack Targets
        // =============================================================================

        /** Target any spell on the stack */
        val SpellOnStack = TargetFilter(GameObjectFilter.Any, zone = Zone.Stack)

        /** Target creature spell on the stack */
        val CreatureSpellOnStack = TargetFilter(GameObjectFilter.Creature, zone = Zone.Stack)

        /** Target noncreature spell on the stack */
        val NoncreatureSpellOnStack = TargetFilter(GameObjectFilter.Noncreature, zone = Zone.Stack)

        /** Target instant or sorcery spell on the stack */
        val InstantOrSorcerySpellOnStack = TargetFilter(GameObjectFilter.InstantOrSorcery, zone = Zone.Stack)

        // =============================================================================
        // Pre-built for PermanentTargetFilter migration
        // =============================================================================

        /** Target permanent an opponent controls */
        val PermanentOpponentControls = TargetFilter(GameObjectFilter.Permanent.opponentControls())

        /** Target creature or land permanent */
        val CreatureOrLandPermanent = TargetFilter(GameObjectFilter.CreatureOrLand)

        /** Target noncreature permanent */
        val NoncreaturePermanent = TargetFilter(GameObjectFilter.NoncreaturePermanent)

        // =============================================================================
        // Pre-built for SpellTargetFilter migration
        // =============================================================================

        /** Target sorcery spell on the stack */
        val SorcerySpellOnStack = TargetFilter(GameObjectFilter.Sorcery, zone = Zone.Stack)

        /** Target creature or sorcery spell on the stack */
        val CreatureOrSorcerySpellOnStack = TargetFilter(GameObjectFilter.CreatureOrSorcery, zone = Zone.Stack)

        /** Target instant spell on the stack */
        val InstantSpellOnStack = TargetFilter(GameObjectFilter.Instant, zone = Zone.Stack)
    }

    // =============================================================================
    // Fluent Builder Methods (delegates to GameObjectFilter)
    // =============================================================================

    /** Add color requirement */
    fun withColor(color: Color) = copy(baseFilter = baseFilter.withColor(color))

    /** Exclude color */
    fun notColor(color: Color) = copy(baseFilter = baseFilter.notColor(color))

    /** Add subtype requirement */
    fun withSubtype(subtype: Subtype) = copy(baseFilter = baseFilter.withSubtype(subtype))

    /** Add subtype by string */
    fun withSubtype(subtype: String) = copy(baseFilter = baseFilter.withSubtype(subtype))

    /** Add keyword requirement */
    fun withKeyword(keyword: Keyword) = copy(baseFilter = baseFilter.withKeyword(keyword))

    /** Exclude keyword */
    fun withoutKeyword(keyword: Keyword) = copy(baseFilter = baseFilter.withoutKeyword(keyword))

    /** Mana value at most */
    fun manaValueAtMost(max: Int) = copy(baseFilter = baseFilter.manaValueAtMost(max))

    /** Mana value at least */
    fun manaValueAtLeast(min: Int) = copy(baseFilter = baseFilter.manaValueAtLeast(min))

    /** Power at most */
    fun powerAtMost(max: Int) = copy(baseFilter = baseFilter.powerAtMost(max))

    /** Power at least */
    fun powerAtLeast(min: Int) = copy(baseFilter = baseFilter.powerAtLeast(min))

    /** Toughness at most */
    fun toughnessAtMost(max: Int) = copy(baseFilter = baseFilter.toughnessAtMost(max))

    /** Must be tapped */
    fun tapped() = copy(baseFilter = baseFilter.tapped())

    /** Must be untapped */
    fun untapped() = copy(baseFilter = baseFilter.untapped())

    /** Must be attacking */
    fun attacking() = copy(baseFilter = baseFilter.attacking())

    /** Must be controlled by you */
    fun youControl() = copy(baseFilter = baseFilter.youControl())

    /** Must be controlled by opponent */
    fun opponentControls() = copy(baseFilter = baseFilter.opponentControls())

    /** Must be owned by you (for cards in graveyards/exile) */
    fun ownedByYou() = copy(baseFilter = baseFilter.ownedByYou())

    /** Must be owned by opponent (for cards in graveyards/exile) */
    fun ownedByOpponent() = copy(baseFilter = baseFilter.ownedByOpponent())

    /** Exclude the source permanent */
    fun other() = copy(excludeSelf = true)

    /** Target in a different zone */
    fun inZone(zone: Zone) = copy(zone = zone)
}
