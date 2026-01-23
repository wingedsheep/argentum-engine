package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.targeting.*

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
    val CreatureYouControl: TargetRequirement = TargetCreature(filter = CreatureTargetFilter.YouControl)

    /**
     * Target creature an opponent controls.
     */
    val CreatureOpponentControls: TargetRequirement = TargetCreature(filter = CreatureTargetFilter.OpponentControls)

    /**
     * Target attacking creature.
     */
    val AttackingCreature: TargetRequirement = TargetCreature(filter = CreatureTargetFilter.Attacking)

    /**
     * Target blocking creature.
     */
    val BlockingCreature: TargetRequirement = TargetCreature(filter = CreatureTargetFilter.Blocking)

    /**
     * Target tapped creature.
     */
    val TappedCreature: TargetRequirement = TargetCreature(filter = CreatureTargetFilter.Tapped)

    /**
     * Target creature with a specific keyword.
     */
    fun CreatureWithKeyword(keyword: Keyword): TargetRequirement =
        TargetCreature(filter = CreatureTargetFilter.WithKeyword(keyword))

    /**
     * Target creature with a specific color.
     */
    fun CreatureWithColor(color: Color): TargetRequirement =
        TargetCreature(filter = CreatureTargetFilter.WithColor(color))

    /**
     * Target creature with power at most N.
     */
    fun CreatureWithPowerAtMost(maxPower: Int): TargetRequirement =
        TargetCreature(filter = CreatureTargetFilter.WithPowerAtMost(maxPower))

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
    val NonlandPermanent: TargetRequirement = TargetPermanent(filter = PermanentTargetFilter.NonLand)

    /**
     * Target artifact.
     */
    val Artifact: TargetRequirement = TargetPermanent(filter = PermanentTargetFilter.Artifact)

    /**
     * Target enchantment.
     */
    val Enchantment: TargetRequirement = TargetPermanent(filter = PermanentTargetFilter.Enchantment)

    /**
     * Target land.
     */
    val Land: TargetRequirement = TargetPermanent(filter = PermanentTargetFilter.Land)

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
    val CardInGraveyard: TargetRequirement = TargetCardInGraveyard()

    /**
     * Target creature card in a graveyard.
     */
    val CreatureCardInGraveyard: TargetRequirement =
        TargetCardInGraveyard(filter = GraveyardCardFilter.Creature)

    /**
     * Target instant or sorcery card in a graveyard.
     */
    val InstantOrSorceryInGraveyard: TargetRequirement =
        TargetCardInGraveyard(filter = GraveyardCardFilter.InstantOrSorcery)

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
    val CreatureSpell: TargetRequirement = TargetSpell(filter = SpellTargetFilter.Creature)

    /**
     * Target noncreature spell.
     */
    val NoncreatureSpell: TargetRequirement = TargetSpell(filter = SpellTargetFilter.Noncreature)

    /**
     * Target spell with mana value N or less.
     */
    fun SpellWithManaValueAtMost(manaValue: Int): TargetRequirement =
        TargetSpell(filter = SpellTargetFilter.WithManaValueAtMost(manaValue))
}
