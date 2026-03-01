package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlayer
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.targets.TargetSpell

/**
 * Facade object providing convenient access to TargetRequirement types.
 *
 * Usage:
 * ```kotlin
 * Targets.Creature
 * Targets.Any
 * Targets.Player
 * ```
 */
object Targets {

    // =========================================================================
    // Player Targeting
    // =========================================================================

    /**
     * Target any player.
     */
    val Player: TargetRequirement = TargetPlayer()

    /**
     * Target opponent.
     */
    val Opponent: TargetRequirement = TargetOpponent()

    /**
     * All players (for symmetric effects).
     */
    val AllPlayers: TargetRequirement = TargetPlayer()  // Engine handles "each player"

    // =========================================================================
    // Creature Targeting
    // =========================================================================

    /**
     * Target creature.
     */
    val Creature: TargetRequirement = TargetCreature()

    /**
     * Target creature you control.
     */
    val CreatureYouControl: TargetRequirement = TargetCreature(filter = TargetFilter.CreatureYouControl)

    /**
     * Target creature an opponent controls.
     */
    val CreatureOpponentControls: TargetRequirement = TargetCreature(filter = TargetFilter.CreatureOpponentControls)

    /**
     * Target attacking creature.
     */
    val AttackingCreature: TargetRequirement = TargetCreature(filter = TargetFilter.AttackingCreature)

    /**
     * Target blocking creature.
     */
    val BlockingCreature: TargetRequirement = TargetCreature(filter = TargetFilter.BlockingCreature)

    /**
     * Target tapped creature.
     */
    val TappedCreature: TargetRequirement = TargetCreature(filter = TargetFilter.TappedCreature)

    /**
     * Target creature with a specific keyword.
     */
    fun CreatureWithKeyword(keyword: Keyword): TargetRequirement =
        TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.withKeyword(keyword)))

    /**
     * Target creature with a specific color.
     */
    fun CreatureWithColor(color: Color): TargetRequirement =
        TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.withColor(color)))

    /**
     * Target creature with power at most N.
     */
    fun CreatureWithPowerAtMost(maxPower: Int): TargetRequirement =
        TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.powerAtMost(maxPower)))

    /**
     * Target up to N creatures.
     */
    fun UpToCreatures(count: Int): TargetRequirement =
        TargetCreature(count = count, optional = true)

    // =========================================================================
    // Permanent Targeting
    // =========================================================================

    /**
     * Target permanent.
     */
    val Permanent: TargetRequirement = TargetPermanent()

    /**
     * Target nonland permanent.
     */
    val NonlandPermanent: TargetRequirement = TargetPermanent(filter = TargetFilter.NonlandPermanent)

    /**
     * Target artifact.
     */
    val Artifact: TargetRequirement = TargetPermanent(filter = TargetFilter.Artifact)

    /**
     * Target enchantment.
     */
    val Enchantment: TargetRequirement = TargetPermanent(filter = TargetFilter.Enchantment)

    /**
     * Target land.
     */
    val Land: TargetRequirement = TargetPermanent(filter = TargetFilter.Land)

    /**
     * Target permanent an opponent controls.
     */
    val PermanentOpponentControls: TargetRequirement = TargetPermanent(filter = TargetFilter.PermanentOpponentControls)

    // =========================================================================
    // Combined Targeting
    // =========================================================================

    /**
     * Any target (creature, player, or planeswalker).
     */
    val Any: TargetRequirement = AnyTarget()

    /**
     * Target creature or player.
     */
    val CreatureOrPlayer: TargetRequirement = TargetCreatureOrPlayer()

    /**
     * Target creature or planeswalker.
     */
    val CreatureOrPlaneswalker: TargetRequirement = TargetCreatureOrPlaneswalker()

    // =========================================================================
    // Graveyard Targeting
    // =========================================================================

    /**
     * Target card in a graveyard.
     */
    val CardInGraveyard: TargetRequirement = TargetObject(filter = TargetFilter.CardInGraveyard)

    /**
     * Target creature card in a graveyard (any player's graveyard).
     */
    val CreatureCardInGraveyard: TargetRequirement =
        TargetObject(filter = TargetFilter.CreatureInGraveyard)

    /**
     * Target creature card in YOUR graveyard.
     * Used for cards like Breath of Life, Zombify, etc.
     */
    val CreatureCardInYourGraveyard: TargetRequirement =
        TargetObject(filter = TargetFilter.CreatureInYourGraveyard)

    /**
     * Target instant or sorcery card in a graveyard.
     */
    val InstantOrSorceryInGraveyard: TargetRequirement =
        TargetObject(filter = TargetFilter.InstantOrSorceryInGraveyard)

    // =========================================================================
    // Spell Targeting
    // =========================================================================

    /**
     * Target spell.
     */
    val Spell: TargetRequirement = TargetSpell()

    /**
     * Target creature spell.
     */
    val CreatureSpell: TargetRequirement = TargetSpell(filter = TargetFilter.CreatureSpellOnStack)

    /**
     * Target noncreature spell.
     */
    val NoncreatureSpell: TargetRequirement = TargetSpell(filter = TargetFilter.NoncreatureSpellOnStack)

    /**
     * Target creature or sorcery spell.
     */
    val CreatureOrSorcerySpell: TargetRequirement = TargetSpell(filter = TargetFilter.CreatureOrSorcerySpellOnStack)

    /**
     * Target instant or sorcery spell.
     */
    val InstantOrSorcerySpell: TargetRequirement = TargetSpell(filter = TargetFilter.InstantOrSorcerySpellOnStack)

    /**
     * Target spell with mana value N or less.
     */
    fun SpellWithManaValueAtMost(manaValue: Int): TargetRequirement =
        TargetSpell(filter = TargetFilter.SpellOnStack.manaValueAtMost(manaValue))

    /**
     * Target spell with mana value N or greater.
     */
    fun SpellWithManaValueAtLeast(manaValue: Int): TargetRequirement =
        TargetSpell(filter = TargetFilter.SpellOnStack.manaValueAtLeast(manaValue))

    /**
     * Target activated or triggered ability on the stack.
     */
    val ActivatedOrTriggeredAbility: TargetRequirement = TargetObject(
        filter = TargetFilter.ActivatedOrTriggeredAbilityOnStack
    )

    /**
     * Target spell or ability with a single target.
     * The single-target restriction is enforced at resolution time by the executor.
     */
    val SpellOrAbilityWithSingleTarget: TargetRequirement = TargetObject(
        filter = TargetFilter.SpellOrAbilityOnStack
    )

    // =========================================================================
    // Unified Target Filters (NEW - composable predicate-based targeting)
    // =========================================================================

    /**
     * Unified target filter namespace providing composable, predicate-based targeting.
     *
     * These filters use the new unified filter architecture. They can be used
     * for effect targeting where you need to specify what kind of objects an
     * effect should target.
     *
     * Usage:
     * ```kotlin
     * // Simple creature target
     * Targets.Unified.creature
     *
     * // Tapped creature target
     * Targets.Unified.tappedCreature
     *
     * // Custom: black creature with power 2 or less
     * Targets.Unified.creature { withColor(Color.BLACK).powerAtMost(2) }
     *
     * // Card in graveyard
     * Targets.Unified.creatureInGraveyard
     * ```
     */
    object Unified {
        // Battlefield creature targets
        val creature: TargetFilter = TargetFilter.Creature
        val creatureYouControl: TargetFilter = TargetFilter.CreatureYouControl
        val creatureOpponentControls: TargetFilter = TargetFilter.CreatureOpponentControls
        val otherCreature: TargetFilter = TargetFilter.OtherCreature
        val otherCreatureYouControl: TargetFilter = TargetFilter.OtherCreatureYouControl
        val tappedCreature: TargetFilter = TargetFilter.TappedCreature
        val untappedCreature: TargetFilter = TargetFilter.UntappedCreature
        val attackingCreature: TargetFilter = TargetFilter.AttackingCreature
        val blockingCreature: TargetFilter = TargetFilter.BlockingCreature
        val attackingOrBlockingCreature: TargetFilter = TargetFilter.AttackingOrBlockingCreature

        // Battlefield permanent targets
        val permanent: TargetFilter = TargetFilter.Permanent
        val permanentYouControl: TargetFilter = TargetFilter.PermanentYouControl
        val nonlandPermanent: TargetFilter = TargetFilter.NonlandPermanent
        val nonlandPermanentOpponentControls: TargetFilter = TargetFilter.NonlandPermanentOpponentControls
        val artifact: TargetFilter = TargetFilter.Artifact
        val enchantment: TargetFilter = TargetFilter.Enchantment
        val land: TargetFilter = TargetFilter.Land
        val planeswalker: TargetFilter = TargetFilter.Planeswalker

        // Graveyard targets
        val cardInGraveyard: TargetFilter = TargetFilter.CardInGraveyard
        val creatureInGraveyard: TargetFilter = TargetFilter.CreatureInGraveyard
        val instantOrSorceryInGraveyard: TargetFilter = TargetFilter.InstantOrSorceryInGraveyard

        // Stack targets
        val spell: TargetFilter = TargetFilter.SpellOnStack
        val creatureSpell: TargetFilter = TargetFilter.CreatureSpellOnStack
        val noncreatureSpell: TargetFilter = TargetFilter.NoncreatureSpellOnStack
        val instantOrSorcerySpell: TargetFilter = TargetFilter.InstantOrSorcerySpellOnStack

        // Builder for custom creature filters
        fun creature(builder: GameObjectFilter.() -> GameObjectFilter): TargetFilter =
            TargetFilter(GameObjectFilter.Creature.builder())

        // Builder for custom permanent filters
        fun permanent(builder: GameObjectFilter.() -> GameObjectFilter): TargetFilter =
            TargetFilter(GameObjectFilter.Permanent.builder())

        // Target in specific zone
        fun inGraveyard(builder: GameObjectFilter.() -> GameObjectFilter = { this }): TargetFilter =
            TargetFilter(GameObjectFilter.Any.builder(), zone = Zone.GRAVEYARD)

        fun onStack(builder: GameObjectFilter.() -> GameObjectFilter = { this }): TargetFilter =
            TargetFilter(GameObjectFilter.Any.builder(), zone = Zone.STACK)

        fun inExile(builder: GameObjectFilter.() -> GameObjectFilter = { this }): TargetFilter =
            TargetFilter(GameObjectFilter.Any.builder(), zone = Zone.EXILE)
    }
}
