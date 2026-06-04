package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Damage Effects
// =============================================================================

/**
 * Deal damage to a target.
 * Supports both fixed amounts and dynamic amounts (e.g., X value, creature count).
 *
 * Examples:
 * - Lightning Bolt: DealDamageEffect(3, target)
 * - Blaze: DealDamageEffect(DynamicAmount.XValue, target)
 * - Final Strike: DealDamageEffect(DynamicAmounts.sacrificedPower(), target)
 */
@SerialName("DealDamage")
@Serializable
data class DealDamageEffect(
    val amount: DynamicAmount,
    val target: EffectTarget,
    val cantBePrevented: Boolean = false,
    /**
     * Optional override for the damage source. When null the engine attributes the damage to the
     * resolving spell/ability's source (the trigger's source for triggered abilities, the spell
     * itself for instants/sorceries). For LTB triggers on tokens, the source has already been
     * SBA-swept (CR 704.5d) by the time the trigger resolves — the engine reads it via
     * last-known-information, so leaving this null on a token's LTB damage clause works
     * (e.g. Munitions' "When this token leaves the battlefield, it deals 2 damage to any target").
     */
    val damageSource: EffectTarget? = null
) : Effect {
    /** Convenience constructor for fixed amounts */
    constructor(amount: Int, target: EffectTarget, cantBePrevented: Boolean = false, damageSource: EffectTarget? = null)
        : this(DynamicAmount.Fixed(amount), target, cantBePrevented, damageSource)

    override val description: String = buildString {
        if (damageSource != null) {
            append("${damageSource.description} deals ${amount.description} damage to ${target.description}")
        } else {
            append("Deal ${amount.description} damage to ${target.description}")
        }
        if (cantBePrevented) append(". This damage can't be prevented")
    }

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int): String = buildString {
        val resolved = resolver(amount)
        if (damageSource != null) {
            append("${damageSource.description} deals $resolved damage to ${target.description}")
        } else {
            append("Deal $resolved damage to ${target.description}")
        }
        if (cantBePrevented) append(". This damage can't be prevented")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
}

/**
 * Deal damage to multiple targets, dividing the total as you choose.
 * Used for cards like Forked Lightning ("4 damage divided among 1-3 targets").
 *
 * [totalDamage] is the fixed total. [dynamicTotal], when non-null, is evaluated at resolution and
 * overrides [totalDamage] — used by effects whose total is computed when they resolve (Ureni, the
 * Song Unending: "X damage divided as you choose ..., where X is the number of lands you control").
 * The set of targets among which the total is divided comes from the ability's target requirement
 * (e.g. `TargetObject(unlimited = true, filter = CreatureOrPlaneswalker opponent controls)`), so
 * "any number of target" forms divide the total only among the targets actually chosen.
 */
@SerialName("DividedDamage")
@Serializable
data class DividedDamageEffect(
    val totalDamage: Int,
    val minTargets: Int = 1,
    val maxTargets: Int = 3,
    val dynamicTotal: com.wingedsheep.sdk.scripting.values.DynamicAmount? = null
) : Effect {
    override val description: String =
        if (dynamicTotal != null)
            "Deal damage equal to ${dynamicTotal.description} divided as you choose among the targets"
        else
            "Deal $totalDamage damage divided as you choose among $minTargets to $maxTargets target creatures"
}

/**
 * Two creatures fight — each deals damage equal to its power to the other.
 * Used for fight abilities like Contested Cliffs and the fight keyword action.
 *
 * @property target1 First creature (e.g., Beast you control)
 * @property target2 Second creature (e.g., creature opponent controls)
 */
@SerialName("Fight")
@Serializable
data class FightEffect(
    val target1: EffectTarget,
    val target2: EffectTarget
) : Effect {
    override val description: String = "${target1.description} fights ${target2.description}"
}

/**
 * Deal damage to a target for each entity from a tracked collection that is still in a given zone.
 * Used for Dragonhawk-style "deal 2 damage to each opponent for each of those cards that are still exiled."
 *
 * At definition time, [collectionName] references a pipeline collection (e.g., "exiledCards").
 * When a delayed trigger is created, [CreateDelayedTriggerExecutor] resolves the collection
 * to concrete [entityIds] so the delayed trigger can check zone membership without the original context.
 *
 * @property entityIds Concrete entity IDs to check (populated at delayed trigger creation time)
 * @property collectionName Pipeline collection name to resolve into entityIds (used at definition time)
 * @property zone The zone to check for remaining entities
 * @property damagePerEntity Damage dealt per entity still in the zone
 * @property target Who receives the damage (e.g., PlayerRef(Player.EachOpponent))
 * @property damageSource Optional override for the damage source
 */
@SerialName("DealDamagePerEntityInZone")
@Serializable
data class DealDamagePerEntityInZoneEffect(
    val entityIds: List<EntityId> = emptyList(),
    val collectionName: String? = null,
    val zone: Zone = Zone.EXILE,
    val damagePerEntity: Int = 1,
    val target: EffectTarget = EffectTarget.PlayerRef(Player.EachOpponent),
    val damageSource: EffectTarget? = null
) : Effect {
    override val description: String =
        "Deal $damagePerEntity damage to ${target.description} for each card still in ${zone.name.lowercase()}"
}
