package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.CreatureFilter
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Facade object providing convenient access to filter types.
 *
 * Used for:
 * - Card filters in search effects
 * - Creature filters in static abilities
 * - Permanent filters for targeting
 *
 * Usage:
 * ```kotlin
 * Filters.Creature
 * Filters.BasicLand
 * Filters.CreaturesYouControl
 * ```
 */
object Filters {

    // =========================================================================
    // Card Filters (for search/library effects)
    // =========================================================================

    /**
     * Any card.
     */
    val AnyCard: CardFilter = CardFilter.AnyCard

    /**
     * Creature card.
     */
    val Creature: CardFilter = CardFilter.CreatureCard

    /**
     * Land card.
     */
    val Land: CardFilter = CardFilter.LandCard

    /**
     * Basic land card.
     */
    val BasicLand: CardFilter = CardFilter.BasicLandCard

    /**
     * Plains card (for Gift of Estates, etc.).
     */
    val PlainsCard: CardFilter = CardFilter.HasSubtype("Plains")

    /**
     * Island card.
     */
    val IslandCard: CardFilter = CardFilter.HasSubtype("Island")

    /**
     * Swamp card.
     */
    val SwampCard: CardFilter = CardFilter.HasSubtype("Swamp")

    /**
     * Mountain card.
     */
    val MountainCard: CardFilter = CardFilter.HasSubtype("Mountain")

    /**
     * Forest card.
     */
    val ForestCard: CardFilter = CardFilter.HasSubtype("Forest")

    /**
     * Instant card.
     */
    val Instant: CardFilter = CardFilter.InstantCard

    /**
     * Sorcery card.
     */
    val Sorcery: CardFilter = CardFilter.SorceryCard

    /**
     * Permanent card.
     */
    val Permanent: CardFilter = CardFilter.PermanentCard

    /**
     * Nonland permanent card.
     */
    val NonlandPermanent: CardFilter = CardFilter.NonlandPermanentCard

    /**
     * Card with a specific subtype.
     */
    fun WithSubtype(subtype: String): CardFilter = CardFilter.HasSubtype(subtype)

    /**
     * Card with a specific color.
     */
    fun WithColor(color: Color): CardFilter = CardFilter.HasColor(color)

    /**
     * Green creature card (for Natural Order).
     */
    val GreenCreature: CardFilter = CardFilter.And(listOf(
        CardFilter.CreatureCard,
        CardFilter.HasColor(Color.GREEN)
    ))

    /**
     * Card with mana value at most N.
     */
    fun ManaValueAtMost(max: Int): CardFilter = CardFilter.ManaValueAtMost(max)

    // =========================================================================
    // Creature Filters (for static abilities)
    // =========================================================================

    /**
     * All creatures.
     */
    val AllCreatures: CreatureFilter = CreatureFilter.All

    /**
     * Creatures you control.
     */
    val CreaturesYouControl: CreatureFilter = CreatureFilter.YouControl

    /**
     * Creatures opponents control.
     */
    val CreaturesOpponentsControl: CreatureFilter = CreatureFilter.OpponentsControl

    /**
     * Creatures with a specific keyword.
     */
    fun CreaturesWithKeyword(keyword: Keyword): CreatureFilter =
        CreatureFilter.WithKeyword(keyword)

    /**
     * Creatures without a specific keyword.
     */
    fun CreaturesWithoutKeyword(keyword: Keyword): CreatureFilter =
        CreatureFilter.WithoutKeyword(keyword)

    // =========================================================================
    // Static Targets (for equipment/auras)
    // =========================================================================

    /**
     * The creature this equipment/aura is attached to.
     */
    val AttachedCreature: StaticTarget = StaticTarget.AttachedCreature

    /**
     * The creature this equipment is equipped to.
     */
    val EquippedCreature: StaticTarget = StaticTarget.AttachedCreature

    /**
     * The creature this aura is enchanting.
     */
    val EnchantedCreature: StaticTarget = StaticTarget.AttachedCreature

    /**
     * The source permanent itself.
     */
    val Self: StaticTarget = StaticTarget.SourceCreature

    /**
     * The controller of the source.
     */
    val Controller: StaticTarget = StaticTarget.Controller

    /**
     * All creatures the controller owns.
     */
    val AllControlledCreatures: StaticTarget = StaticTarget.AllControlledCreatures
}
