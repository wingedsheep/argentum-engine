package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.CardPredicate
import com.wingedsheep.sdk.scripting.CreatureFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.Zone

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
    val AnyCard: GameObjectFilter = GameObjectFilter.Any

    /**
     * Creature card.
     */
    val Creature: GameObjectFilter = GameObjectFilter.Creature

    /**
     * Land card.
     */
    val Land: GameObjectFilter = GameObjectFilter.Land

    /**
     * Basic land card.
     */
    val BasicLand: GameObjectFilter = GameObjectFilter.BasicLand

    /**
     * Plains card (for Gift of Estates, etc.).
     */
    val PlainsCard: GameObjectFilter = GameObjectFilter.Land.withSubtype("Plains")

    /**
     * Island card.
     */
    val IslandCard: GameObjectFilter = GameObjectFilter.Land.withSubtype("Island")

    /**
     * Swamp card.
     */
    val SwampCard: GameObjectFilter = GameObjectFilter.Land.withSubtype("Swamp")

    /**
     * Mountain card.
     */
    val MountainCard: GameObjectFilter = GameObjectFilter.Land.withSubtype("Mountain")

    /**
     * Forest card.
     */
    val ForestCard: GameObjectFilter = GameObjectFilter.Land.withSubtype("Forest")

    /**
     * Instant card.
     */
    val Instant: GameObjectFilter = GameObjectFilter.Instant

    /**
     * Sorcery card.
     */
    val Sorcery: GameObjectFilter = GameObjectFilter.Sorcery

    /**
     * Permanent card.
     */
    val Permanent: GameObjectFilter = GameObjectFilter.Permanent

    /**
     * Nonland permanent card.
     */
    val NonlandPermanent: GameObjectFilter = GameObjectFilter.NonlandPermanent

    /**
     * Card with a specific subtype.
     */
    fun WithSubtype(subtype: String): GameObjectFilter = GameObjectFilter.Any.withSubtype(subtype)

    /**
     * Card with a specific color.
     */
    fun WithColor(color: Color): GameObjectFilter = GameObjectFilter.Any.withColor(color)

    /**
     * Green creature card (for Natural Order).
     */
    val GreenCreature: GameObjectFilter = GameObjectFilter.Creature.withColor(Color.GREEN)

    /**
     * Card with mana value at most N.
     */
    fun ManaValueAtMost(max: Int): GameObjectFilter = GameObjectFilter.Any.manaValueAtMost(max)

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

    // =========================================================================
    // Unified Filters (NEW - composable predicate-based filtering)
    // =========================================================================

    /**
     * Unified filter namespace providing composable, predicate-based filters.
     *
     * Usage:
     * ```kotlin
     * // Simple type filter
     * Filters.Unified.creature
     *
     * // Creature with specific properties
     * Filters.Unified.creature.withColor(Color.BLACK).tapped()
     *
     * // Complex filter
     * Filters.Unified.creature.powerAtMost(2).opponentControls()
     * ```
     */
    object Unified {
        // Type filters
        val any: GameObjectFilter = GameObjectFilter.Any
        val creature: GameObjectFilter = GameObjectFilter.Creature
        val land: GameObjectFilter = GameObjectFilter.Land
        val basicLand: GameObjectFilter = GameObjectFilter.BasicLand
        val artifact: GameObjectFilter = GameObjectFilter.Artifact
        val enchantment: GameObjectFilter = GameObjectFilter.Enchantment
        val planeswalker: GameObjectFilter = GameObjectFilter.Planeswalker
        val instant: GameObjectFilter = GameObjectFilter.Instant
        val sorcery: GameObjectFilter = GameObjectFilter.Sorcery
        val permanent: GameObjectFilter = GameObjectFilter.Permanent
        val nonlandPermanent: GameObjectFilter = GameObjectFilter.NonlandPermanent
        val instantOrSorcery: GameObjectFilter = GameObjectFilter.InstantOrSorcery

        // Builder functions
        fun withColor(color: Color): GameObjectFilter = GameObjectFilter.Any.withColor(color)
        fun withSubtype(subtype: String): GameObjectFilter = GameObjectFilter.Any.withSubtype(subtype)
        fun withSubtype(subtype: Subtype): GameObjectFilter = GameObjectFilter.Any.withSubtype(subtype)
        fun withKeyword(keyword: Keyword): GameObjectFilter = GameObjectFilter.Any.withKeyword(keyword)
        fun manaValueAtMost(max: Int): GameObjectFilter = GameObjectFilter.Any.manaValueAtMost(max)
        fun manaValueAtLeast(min: Int): GameObjectFilter = GameObjectFilter.Any.manaValueAtLeast(min)
    }

    /**
     * Group filter namespace for mass effects (damage to all creatures, etc.).
     *
     * Usage:
     * ```kotlin
     * // All creatures
     * Filters.Group.allCreatures
     *
     * // Creatures you control
     * Filters.Group.creaturesYouControl
     *
     * // Other creatures with flying
     * Filters.Group.allCreatures.withKeyword(Keyword.FLYING).other()
     * ```
     */
    object Group {
        val allCreatures: GroupFilter = GroupFilter.AllCreatures
        val creaturesYouControl: GroupFilter = GroupFilter.AllCreaturesYouControl
        val creaturesOpponentsControl: GroupFilter = GroupFilter.AllCreaturesOpponentsControl
        val otherCreatures: GroupFilter = GroupFilter.AllOtherCreatures
        val otherCreaturesYouControl: GroupFilter = GroupFilter.OtherCreaturesYouControl
        val attackingCreatures: GroupFilter = GroupFilter.AttackingCreatures
        val blockingCreatures: GroupFilter = GroupFilter.BlockingCreatures
        val tappedCreatures: GroupFilter = GroupFilter.TappedCreatures
        val untappedCreatures: GroupFilter = GroupFilter.UntappedCreatures
        val allPermanents: GroupFilter = GroupFilter.AllPermanents
        val permanentsYouControl: GroupFilter = GroupFilter.AllPermanentsYouControl
        val allArtifacts: GroupFilter = GroupFilter.AllArtifacts
        val allEnchantments: GroupFilter = GroupFilter.AllEnchantments
        val allLands: GroupFilter = GroupFilter.AllLands

        // Builder for custom group filters
        fun creatures(builder: GameObjectFilter.() -> GameObjectFilter = { this }): GroupFilter =
            GroupFilter(GameObjectFilter.Creature.builder())

        fun permanents(builder: GameObjectFilter.() -> GameObjectFilter = { this }): GroupFilter =
            GroupFilter(GameObjectFilter.Permanent.builder())
    }

    /**
     * Target filter namespace for targeting effects.
     *
     * Usage:
     * ```kotlin
     * // Target creature
     * Filters.Target.creature
     *
     * // Target tapped creature
     * Filters.Target.tappedCreature
     *
     * // Target creature in graveyard
     * Filters.Target.creatureInGraveyard
     * ```
     */
    object Target {
        // Battlefield targets
        val creature: TargetFilter = TargetFilter.Creature
        val creatureYouControl: TargetFilter = TargetFilter.CreatureYouControl
        val creatureOpponentControls: TargetFilter = TargetFilter.CreatureOpponentControls
        val otherCreature: TargetFilter = TargetFilter.OtherCreature
        val tappedCreature: TargetFilter = TargetFilter.TappedCreature
        val untappedCreature: TargetFilter = TargetFilter.UntappedCreature
        val attackingCreature: TargetFilter = TargetFilter.AttackingCreature
        val blockingCreature: TargetFilter = TargetFilter.BlockingCreature
        val permanent: TargetFilter = TargetFilter.Permanent
        val nonlandPermanent: TargetFilter = TargetFilter.NonlandPermanent
        val artifact: TargetFilter = TargetFilter.Artifact
        val enchantment: TargetFilter = TargetFilter.Enchantment
        val land: TargetFilter = TargetFilter.Land
        val planeswalker: TargetFilter = TargetFilter.Planeswalker

        // Graveyard targets
        val cardInGraveyard: TargetFilter = TargetFilter.CardInGraveyard
        val creatureInGraveyard: TargetFilter = TargetFilter.CreatureInGraveyard
        val instantOrSorceryInGraveyard: TargetFilter = TargetFilter.InstantOrSorceryInGraveyard

        // Stack targets
        val spellOnStack: TargetFilter = TargetFilter.SpellOnStack
        val creatureSpellOnStack: TargetFilter = TargetFilter.CreatureSpellOnStack
        val noncreatureSpellOnStack: TargetFilter = TargetFilter.NoncreatureSpellOnStack

        // Builder for custom target filters
        fun creature(builder: GameObjectFilter.() -> GameObjectFilter): TargetFilter =
            TargetFilter(GameObjectFilter.Creature.builder())

        fun permanent(builder: GameObjectFilter.() -> GameObjectFilter): TargetFilter =
            TargetFilter(GameObjectFilter.Permanent.builder())

        fun inZone(zone: Zone, builder: GameObjectFilter.() -> GameObjectFilter = { this }): TargetFilter =
            TargetFilter(GameObjectFilter.Any.builder(), zone = zone)
    }
}
