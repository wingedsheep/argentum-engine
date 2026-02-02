package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword

/**
 * Conversion utilities between old filter types and the new unified filter architecture.
 *
 * These functions allow gradual migration from:
 * - CardFilter -> GameObjectFilter
 * - CountFilter -> GameObjectFilter
 * - CreatureTargetFilter (scripting) -> TargetFilter
 * - CreatureDamageFilter -> GroupFilter
 * - CreatureGroupFilter -> GroupFilter
 * - PlayerReference -> Player
 * - ZoneReference -> Zone
 */
object FilterConversions {

    // =============================================================================
    // CardFilter -> GameObjectFilter
    // =============================================================================

    /**
     * Convert CardFilter to GameObjectFilter.
     */
    fun CardFilter.toGameObjectFilter(): GameObjectFilter = when (this) {
        CardFilter.AnyCard -> GameObjectFilter.Any
        CardFilter.CreatureCard -> GameObjectFilter.Creature
        CardFilter.LandCard -> GameObjectFilter.Land
        CardFilter.BasicLandCard -> GameObjectFilter.BasicLand
        CardFilter.SorceryCard -> GameObjectFilter.Sorcery
        CardFilter.InstantCard -> GameObjectFilter.Instant
        CardFilter.PermanentCard -> GameObjectFilter.Permanent
        CardFilter.NonlandPermanentCard -> GameObjectFilter.NonlandPermanent
        is CardFilter.HasSubtype -> GameObjectFilter.Any.withSubtype(subtype)
        is CardFilter.HasColor -> GameObjectFilter.Any.withColor(color)
        is CardFilter.ManaValueAtMost -> GameObjectFilter.Any.manaValueAtMost(maxManaValue)
        is CardFilter.And -> filters.map { it.toGameObjectFilter() }
            .reduceOrNull { acc, filter -> acc.and(filter) } ?: GameObjectFilter.Any
        is CardFilter.Or -> filters.map { it.toGameObjectFilter() }
            .reduceOrNull { acc, filter -> acc.or(filter) } ?: GameObjectFilter.Any
        is CardFilter.Not -> GameObjectFilter(
            cardPredicates = listOf(CardPredicate.Not(filter.toGameObjectFilter().cardPredicates.firstOrNull() ?: CardPredicate.IsCreature))
        )
    }

    // =============================================================================
    // CountFilter -> GameObjectFilter
    // =============================================================================

    /**
     * Convert CountFilter to GameObjectFilter.
     */
    fun CountFilter.toGameObjectFilter(): GameObjectFilter = when (this) {
        CountFilter.Any -> GameObjectFilter.Any
        CountFilter.Creatures -> GameObjectFilter.Creature
        CountFilter.TappedCreatures -> GameObjectFilter.Creature.tapped()
        CountFilter.UntappedCreatures -> GameObjectFilter.Creature.untapped()
        CountFilter.Lands -> GameObjectFilter.Land
        CountFilter.AttackingCreatures -> GameObjectFilter.Creature.attacking()
        is CountFilter.LandType -> GameObjectFilter.Land.withSubtype(landType)
        is CountFilter.CreatureColor -> GameObjectFilter.Creature.withColor(color)
        is CountFilter.CardColor -> GameObjectFilter.Any.withColor(color)
        is CountFilter.HasSubtype -> GameObjectFilter.Any.withSubtype(subtype)
        is CountFilter.And -> filters.map { it.toGameObjectFilter() }
            .reduceOrNull { acc, filter -> acc.and(filter) } ?: GameObjectFilter.Any
        is CountFilter.Or -> filters.map { it.toGameObjectFilter() }
            .reduceOrNull { acc, filter -> acc.or(filter) } ?: GameObjectFilter.Any
    }

    // =============================================================================
    // CreatureTargetFilter (scripting) -> TargetFilter
    // =============================================================================

    /**
     * Convert scripting CreatureTargetFilter to TargetFilter.
     */
    fun CreatureTargetFilter.toTargetFilter(): TargetFilter = when (this) {
        CreatureTargetFilter.Any -> TargetFilter.Creature
        CreatureTargetFilter.WithFlying -> TargetFilter.Creature.withKeyword(Keyword.FLYING)
        CreatureTargetFilter.WithoutFlying -> TargetFilter.Creature.withoutKeyword(Keyword.FLYING)
        CreatureTargetFilter.Tapped -> TargetFilter.TappedCreature
        CreatureTargetFilter.Untapped -> TargetFilter.UntappedCreature
        CreatureTargetFilter.Attacking -> TargetFilter.AttackingCreature
        CreatureTargetFilter.Nonblack -> TargetFilter.Creature.notColor(Color.BLACK)
    }

    // =============================================================================
    // CreatureDamageFilter -> GroupFilter
    // =============================================================================

    /**
     * Convert CreatureDamageFilter to GroupFilter.
     */
    fun CreatureDamageFilter.toGroupFilter(): GroupFilter = when (this) {
        CreatureDamageFilter.All -> GroupFilter.AllCreatures
        CreatureDamageFilter.Attacking -> GroupFilter.AttackingCreatures
        is CreatureDamageFilter.WithKeyword -> GroupFilter.AllCreatures.withKeyword(keyword)
        is CreatureDamageFilter.WithoutKeyword -> GroupFilter.AllCreatures.withoutKeyword(keyword)
        is CreatureDamageFilter.OfColor -> GroupFilter.AllCreatures.withColor(color)
        is CreatureDamageFilter.NotOfColor -> GroupFilter.AllCreatures.notColor(color)
    }

    // =============================================================================
    // CreatureGroupFilter -> GroupFilter
    // =============================================================================

    /**
     * Convert CreatureGroupFilter to GroupFilter.
     */
    fun CreatureGroupFilter.toGroupFilter(): GroupFilter = when (this) {
        CreatureGroupFilter.All -> GroupFilter.AllCreatures
        CreatureGroupFilter.AllYouControl -> GroupFilter.AllCreaturesYouControl
        CreatureGroupFilter.AllOpponentsControl -> GroupFilter.AllCreaturesOpponentsControl
        CreatureGroupFilter.AllOther -> GroupFilter.AllOtherCreatures
        CreatureGroupFilter.OtherTappedYouControl -> GroupFilter(
            GameObjectFilter.Creature.youControl().tapped(),
            excludeSelf = true
        )
        CreatureGroupFilter.NonWhite -> GroupFilter.AllCreatures.notColor(Color.WHITE)
        is CreatureGroupFilter.ColorYouControl -> GroupFilter.AllCreaturesYouControl.withColor(color)
        is CreatureGroupFilter.WithKeywordYouControl -> GroupFilter.AllCreaturesYouControl.withKeyword(keyword)
        is CreatureGroupFilter.NotColor -> GroupFilter.AllCreatures.notColor(excludedColor)
    }

    // =============================================================================
    // PlayerReference -> Player
    // =============================================================================

    /**
     * Convert PlayerReference to Player.
     */
    @Suppress("DEPRECATION")
    fun PlayerReference.toPlayer(): Player = when (this) {
        PlayerReference.You -> Player.You
        PlayerReference.Opponent -> Player.Opponent
        PlayerReference.TargetOpponent -> Player.TargetOpponent
        PlayerReference.TargetPlayer -> Player.TargetPlayer
        PlayerReference.Each -> Player.Each
    }

    // =============================================================================
    // ZoneReference -> Zone
    // =============================================================================

    /**
     * Convert ZoneReference to Zone.
     */
    @Suppress("DEPRECATION")
    fun ZoneReference.toZone(): Zone = when (this) {
        ZoneReference.Hand -> Zone.Hand
        ZoneReference.Battlefield -> Zone.Battlefield
        ZoneReference.Graveyard -> Zone.Graveyard
        ZoneReference.Library -> Zone.Library
        ZoneReference.Exile -> Zone.Exile
    }

    // =============================================================================
    // SpellFilter -> GameObjectFilter (for stack filtering)
    // =============================================================================

    /**
     * Convert SpellFilter to GameObjectFilter.
     */
    fun SpellFilter.toGameObjectFilter(): GameObjectFilter = when (this) {
        SpellFilter.AnySpell -> GameObjectFilter.Any
        SpellFilter.CreatureSpell -> GameObjectFilter.Creature
        SpellFilter.NonCreatureSpell -> GameObjectFilter.Noncreature
        SpellFilter.SorcerySpell -> GameObjectFilter.Sorcery
        SpellFilter.InstantSpell -> GameObjectFilter.Instant
        is SpellFilter.CreatureOrSorcery -> GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsSorcery))
            )
        )
    }

    // =============================================================================
    // Reverse conversions (for interop during migration)
    // =============================================================================

    /**
     * Convert GameObjectFilter back to CardFilter (best effort).
     * Note: Not all GameObjectFilter capabilities map to CardFilter.
     */
    fun GameObjectFilter.toCardFilter(): CardFilter {
        // Simple case: single card predicate
        val cardPredicate = cardPredicates.firstOrNull()
        return when (cardPredicate) {
            null -> CardFilter.AnyCard
            CardPredicate.IsCreature -> CardFilter.CreatureCard
            CardPredicate.IsLand -> CardFilter.LandCard
            CardPredicate.IsBasicLand -> CardFilter.BasicLandCard
            CardPredicate.IsInstant -> CardFilter.InstantCard
            CardPredicate.IsSorcery -> CardFilter.SorceryCard
            CardPredicate.IsPermanent -> CardFilter.PermanentCard
            is CardPredicate.HasColor -> CardFilter.HasColor(cardPredicate.color)
            is CardPredicate.HasSubtype -> CardFilter.HasSubtype(cardPredicate.subtype.value)
            is CardPredicate.ManaValueAtMost -> CardFilter.ManaValueAtMost(cardPredicate.max)
            else -> CardFilter.AnyCard
        }
    }

    /**
     * Convert Player back to PlayerReference (best effort).
     */
    @Suppress("DEPRECATION")
    fun Player.toPlayerReference(): PlayerReference = when (this) {
        Player.You -> PlayerReference.You
        Player.Opponent, Player.EachOpponent -> PlayerReference.Opponent
        Player.TargetOpponent -> PlayerReference.TargetOpponent
        Player.TargetPlayer, Player.Any -> PlayerReference.TargetPlayer
        Player.Each -> PlayerReference.Each
        is Player.ContextPlayer -> PlayerReference.TargetPlayer
        is Player.ControllerOf -> PlayerReference.Opponent
        is Player.OwnerOf -> PlayerReference.Opponent
    }

    /**
     * Convert Zone back to ZoneReference (best effort).
     */
    @Suppress("DEPRECATION")
    fun Zone.toZoneReference(): ZoneReference = when (this) {
        Zone.Hand -> ZoneReference.Hand
        Zone.Battlefield -> ZoneReference.Battlefield
        Zone.Graveyard -> ZoneReference.Graveyard
        Zone.Library -> ZoneReference.Library
        Zone.Exile -> ZoneReference.Exile
        Zone.Stack -> ZoneReference.Battlefield // Stack doesn't exist in ZoneReference
        Zone.Command -> ZoneReference.Exile // Command doesn't exist in ZoneReference
    }
}

// =============================================================================
// Extension functions for convenience
// =============================================================================

/** Convert this CardFilter to GameObjectFilter */
fun CardFilter.toUnified(): GameObjectFilter = FilterConversions.run { toGameObjectFilter() }

/** Convert this CountFilter to GameObjectFilter */
fun CountFilter.toUnified(): GameObjectFilter = FilterConversions.run { toGameObjectFilter() }

/** Convert this CreatureTargetFilter to TargetFilter */
fun CreatureTargetFilter.toUnified(): TargetFilter = FilterConversions.run { toTargetFilter() }

/** Convert this CreatureDamageFilter to GroupFilter */
fun CreatureDamageFilter.toUnified(): GroupFilter = FilterConversions.run { toGroupFilter() }

/** Convert this CreatureGroupFilter to GroupFilter */
fun CreatureGroupFilter.toUnified(): GroupFilter = FilterConversions.run { toGroupFilter() }

/** Convert this SpellFilter to GameObjectFilter */
fun SpellFilter.toUnified(): GameObjectFilter = FilterConversions.run { toGameObjectFilter() }

/** Convert this PlayerReference to Player */
@Suppress("DEPRECATION")
fun PlayerReference.toUnified(): Player = FilterConversions.run { toPlayer() }

/** Convert this ZoneReference to Zone */
@Suppress("DEPRECATION")
fun ZoneReference.toUnified(): Zone = FilterConversions.run { toZone() }
