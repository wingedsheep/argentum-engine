package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.*

/**
 * Facade object providing convenient access to Trigger types.
 *
 * Usage:
 * ```kotlin
 * Triggers.EntersBattlefield
 * Triggers.Dies
 * Triggers.Attacks
 * ```
 */
object Triggers {

    // =========================================================================
    // Zone Change Triggers
    // =========================================================================

    /**
     * When this permanent enters the battlefield.
     */
    val EntersBattlefield: Trigger = OnEnterBattlefield(selfOnly = true)

    /**
     * When any permanent enters the battlefield.
     */
    val AnyEntersBattlefield: Trigger = OnEnterBattlefield(selfOnly = false)

    /**
     * When another creature you control enters the battlefield.
     */
    val OtherCreatureEnters: Trigger = OnOtherCreatureEnters(youControlOnly = true)

    /**
     * When this permanent leaves the battlefield.
     */
    val LeavesBattlefield: Trigger = OnLeavesBattlefield(selfOnly = true)

    /**
     * When this creature dies.
     */
    val Dies: Trigger = OnDeath(selfOnly = true)

    /**
     * When any creature dies.
     */
    val AnyCreatureDies: Trigger = OnDeath(selfOnly = false)

    /**
     * When a creature is put into your graveyard from the battlefield.
     */
    val YourCreatureDies: Trigger = OnDeath(selfOnly = false, youControlOnly = true)

    /**
     * When this is put into the graveyard from the battlefield.
     */
    val PutIntoGraveyardFromBattlefield: Trigger = OnDeath(selfOnly = true)

    /**
     * When another creature with a specific subtype dies.
     */
    fun OtherCreatureWithSubtypeDies(subtype: Subtype): Trigger =
        OnOtherCreatureWithSubtypeDies(subtype, youControlOnly = true)

    // =========================================================================
    // Combat Triggers
    // =========================================================================

    /**
     * When this creature attacks.
     */
    val Attacks: Trigger = OnAttack(selfOnly = true)

    /**
     * When any creature attacks.
     */
    val AnyAttacks: Trigger = OnAttack(selfOnly = false)

    /**
     * When you attack (declare attackers).
     */
    val YouAttack: Trigger = OnYouAttack(minAttackers = 1)

    /**
     * When this creature blocks.
     */
    val Blocks: Trigger = OnBlock(selfOnly = true)

    /**
     * When this creature becomes blocked.
     */
    val BecomesBlocked: Trigger = OnBecomesBlocked(selfOnly = true)

    /**
     * When a creature you control becomes blocked.
     */
    val CreatureYouControlBecomesBlocked: Trigger = OnBecomesBlocked(selfOnly = false)

    /**
     * When this creature deals damage.
     */
    val DealsDamage: Trigger = OnDealsDamage(selfOnly = true)

    /**
     * When this creature deals combat damage.
     */
    val DealsCombatDamage: Trigger = OnDealsDamage(selfOnly = true, combatOnly = true)

    /**
     * When this creature deals combat damage to a player.
     */
    val DealsCombatDamageToPlayer: Trigger = OnDealsDamage(selfOnly = true, combatOnly = true, toPlayerOnly = true)

    /**
     * When this creature deals combat damage to a creature.
     */
    val DealsCombatDamageToCreature: Trigger = OnDealsDamage(selfOnly = true, combatOnly = true, toCreatureOnly = true)

    // =========================================================================
    // Phase/Step Triggers
    // =========================================================================

    /**
     * At the beginning of your upkeep.
     */
    val YourUpkeep: Trigger = OnUpkeep(controllerOnly = true)

    /**
     * At the beginning of each upkeep.
     */
    val EachUpkeep: Trigger = OnUpkeep(controllerOnly = false)

    /**
     * At the beginning of your end step.
     */
    val YourEndStep: Trigger = OnEndStep(controllerOnly = true)

    /**
     * At the beginning of each end step.
     */
    val EachEndStep: Trigger = OnEndStep(controllerOnly = false)

    /**
     * At the beginning of combat on your turn.
     */
    val BeginCombat: Trigger = OnBeginCombat(controllerOnly = true)

    /**
     * At the beginning of your first main phase.
     */
    val FirstMainPhase: Trigger = OnFirstMainPhase(controllerOnly = true)

    /**
     * At the beginning of enchanted creature's controller's upkeep.
     * Used for auras that grant "At the beginning of your upkeep" to the enchanted creature.
     */
    val EnchantedCreatureControllerUpkeep: Trigger = OnEnchantedCreatureControllerUpkeep

    // =========================================================================
    // Card Drawing Triggers
    // =========================================================================

    /**
     * Whenever you draw a card.
     */
    val YouDraw: Trigger = OnDraw(controllerOnly = true)

    /**
     * Whenever any player draws a card.
     */
    val AnyPlayerDraws: Trigger = OnDraw(controllerOnly = false)

    // =========================================================================
    // Spell Triggers
    // =========================================================================

    /**
     * Whenever you cast a spell.
     */
    val YouCastSpell: Trigger = OnSpellCast(controllerOnly = true)

    /**
     * Whenever you cast a creature spell.
     */
    val YouCastCreature: Trigger = OnSpellCast(controllerOnly = true, spellType = SpellTypeFilter.CREATURE)

    /**
     * Whenever you cast a noncreature spell.
     */
    val YouCastNoncreature: Trigger = OnSpellCast(controllerOnly = true, spellType = SpellTypeFilter.NONCREATURE)

    /**
     * Whenever you cast an instant or sorcery.
     */
    val YouCastInstantOrSorcery: Trigger = OnSpellCast(controllerOnly = true, spellType = SpellTypeFilter.INSTANT_OR_SORCERY)

    // =========================================================================
    // Damage Triggers
    // =========================================================================

    /**
     * Whenever this creature is dealt damage.
     */
    val TakesDamage: Trigger = OnDamageReceived(selfOnly = true)

    /**
     * Whenever a creature deals damage to this creature.
     */
    val DamagedByCreature: Trigger = OnDamagedByCreature(selfOnly = true)

    /**
     * Whenever a spell deals damage to this creature.
     */
    val DamagedBySpell: Trigger = OnDamagedBySpell(selfOnly = true)

    // =========================================================================
    // Tap/Untap Triggers
    // =========================================================================

    /**
     * Whenever this permanent becomes tapped.
     */
    val BecomesTapped: Trigger = OnBecomesTapped(selfOnly = true)

    /**
     * Whenever this permanent becomes untapped.
     */
    val BecomesUntapped: Trigger = OnBecomesUntapped(selfOnly = true)

    // =========================================================================
    // Cycling Triggers
    // =========================================================================

    /**
     * Whenever you cycle a card.
     */
    val YouCycle: Trigger = OnCycle(controllerOnly = true)

    /**
     * Whenever a player cycles a card.
     */
    val AnyPlayerCycles: Trigger = OnCycle(controllerOnly = false)

    // =========================================================================
    // Transform Triggers
    // =========================================================================

    /**
     * When this creature transforms.
     */
    val Transforms: Trigger = OnTransform(selfOnly = true)

    /**
     * When this creature transforms into its back face.
     */
    val TransformsToBack: Trigger = OnTransform(selfOnly = true, intoBackFace = true)

    /**
     * When this creature transforms into its front face.
     */
    val TransformsToFront: Trigger = OnTransform(selfOnly = true, intoBackFace = false)
}
