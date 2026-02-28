package com.wingedsheep.sdk.scripting.filters.unified

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import kotlinx.serialization.Serializable

/**
 * Filter for targeting game objects, with zone context.
 * Wraps GameObjectFilter and adds zone-specific targeting behavior.
 *
 * Unified approach for targeting game objects across all zones.
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
    val zone: Zone = Zone.BATTLEFIELD,
    val excludeSelf: Boolean = false
) {
    val description: String
        get() = buildDescription()

    private fun buildDescription(): String = buildString {
        if (excludeSelf) append("other ")
        append(baseFilter.description)
        if (zone != Zone.BATTLEFIELD) {
            append(" in ")
            append(zone.displayName)
        }
    }

    // =============================================================================
    // Pre-built Creature Targets (Battlefield)
    // =============================================================================

    companion object {
        /** Target any creature */
        val Creature = TargetFilter(GameObjectFilter.Companion.Creature)

        /** Target creature you control */
        val CreatureYouControl = TargetFilter(GameObjectFilter.Companion.Creature.youControl())

        /** Target creature an opponent controls */
        val CreatureOpponentControls = TargetFilter(GameObjectFilter.Companion.Creature.opponentControls())

        /** Target other creature (excluding source) */
        val OtherCreature = TargetFilter(GameObjectFilter.Companion.Creature, excludeSelf = true)

        /** Target other creature you control */
        val OtherCreatureYouControl = TargetFilter(GameObjectFilter.Companion.Creature.youControl(), excludeSelf = true)

        /** Target tapped creature */
        val TappedCreature = TargetFilter(GameObjectFilter.Companion.Creature.tapped())

        /** Target untapped creature */
        val UntappedCreature = TargetFilter(GameObjectFilter.Companion.Creature.untapped())

        /** Target attacking creature */
        val AttackingCreature = TargetFilter(GameObjectFilter.Companion.Creature.attacking())

        /** Target blocking creature */
        val BlockingCreature = TargetFilter(GameObjectFilter.Companion.Creature.blocking())

        /** Target attacking or blocking creature */
        val AttackingOrBlockingCreature = TargetFilter(GameObjectFilter.Companion.Creature.attackingOrBlocking())

        // =============================================================================
        // Pre-built Permanent Targets (Battlefield)
        // =============================================================================

        /** Target any permanent */
        val Permanent = TargetFilter(GameObjectFilter.Companion.Permanent)

        /** Target nonland permanent */
        val NonlandPermanent = TargetFilter(GameObjectFilter.Companion.NonlandPermanent)

        /** Target permanent you control */
        val PermanentYouControl = TargetFilter(GameObjectFilter.Companion.Permanent.youControl())

        /** Target nonland permanent an opponent controls */
        val NonlandPermanentOpponentControls = TargetFilter(GameObjectFilter.Companion.NonlandPermanent.opponentControls())

        /** Target artifact */
        val Artifact = TargetFilter(GameObjectFilter.Companion.Artifact)

        /** Target enchantment */
        val Enchantment = TargetFilter(GameObjectFilter.Companion.Enchantment)

        /** Target land */
        val Land = TargetFilter(GameObjectFilter.Companion.Land)

        /** Target planeswalker */
        val Planeswalker = TargetFilter(GameObjectFilter.Companion.Planeswalker)

        // =============================================================================
        // Pre-built Graveyard Targets
        // =============================================================================

        /** Target any card in a graveyard */
        val CardInGraveyard = TargetFilter(GameObjectFilter.Companion.Any, zone = Zone.GRAVEYARD)

        /** Target creature card in a graveyard */
        val CreatureInGraveyard = TargetFilter(GameObjectFilter.Companion.Creature, zone = Zone.GRAVEYARD)

        /** Target creature card in your graveyard */
        val CreatureInYourGraveyard = TargetFilter(GameObjectFilter.Companion.Creature.ownedByYou(), zone = Zone.GRAVEYARD)

        /** Target instant or sorcery card in a graveyard */
        val InstantOrSorceryInGraveyard = TargetFilter(GameObjectFilter.Companion.InstantOrSorcery, zone = Zone.GRAVEYARD)

        // =============================================================================
        // Pre-built Stack Targets
        // =============================================================================

        /** Target any spell on the stack */
        val SpellOnStack = TargetFilter(GameObjectFilter.Companion.Any, zone = Zone.STACK)

        /** Target creature spell on the stack */
        val CreatureSpellOnStack = TargetFilter(GameObjectFilter.Companion.Creature, zone = Zone.STACK)

        /** Target noncreature spell on the stack */
        val NoncreatureSpellOnStack = TargetFilter(GameObjectFilter.Companion.Noncreature, zone = Zone.STACK)

        /** Target instant or sorcery spell on the stack */
        val InstantOrSorcerySpellOnStack = TargetFilter(GameObjectFilter.Companion.InstantOrSorcery, zone = Zone.STACK)

        // =============================================================================
        // Permanent Targeting
        // =============================================================================

        /** Target permanent an opponent controls */
        val PermanentOpponentControls = TargetFilter(GameObjectFilter.Companion.Permanent.opponentControls())

        /** Target creature or land permanent */
        val CreatureOrLandPermanent = TargetFilter(GameObjectFilter.Companion.CreatureOrLand)

        /** Target noncreature permanent */
        val NoncreaturePermanent = TargetFilter(GameObjectFilter.Companion.NoncreaturePermanent)

        // =============================================================================
        // Spell Targeting (additional)
        // =============================================================================

        /** Target sorcery spell on the stack */
        val SorcerySpellOnStack = TargetFilter(GameObjectFilter.Companion.Sorcery, zone = Zone.STACK)

        /** Target creature or sorcery spell on the stack */
        val CreatureOrSorcerySpellOnStack = TargetFilter(GameObjectFilter.Companion.CreatureOrSorcery, zone = Zone.STACK)

        /** Target instant spell on the stack */
        val InstantSpellOnStack = TargetFilter(GameObjectFilter.Companion.Instant, zone = Zone.STACK)

        /** Target activated or triggered ability on the stack */
        val ActivatedOrTriggeredAbilityOnStack = TargetFilter(
            GameObjectFilter(cardPredicates = listOf(CardPredicate.IsActivatedOrTriggeredAbility)),
            zone = Zone.STACK
        )
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
