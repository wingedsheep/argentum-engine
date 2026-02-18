package com.wingedsheep.engine.mechanics.text

import com.wingedsheep.engine.mechanics.layers.AffectsFilter
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.targeting.*

/**
 * Central utility for transforming creature type references in triggers, effects,
 * filters, and dynamic amounts based on a TextReplacementComponent.
 *
 * Used by Layer 3 text-changing effects (e.g., Artificial Evolution) to modify
 * how a permanent's abilities interpret creature type words.
 */
object SubtypeReplacer {

    /**
     * Replace creature type references in a TriggeredAbility.
     */
    fun replaceTriggeredAbility(
        ability: TriggeredAbility,
        component: TextReplacementComponent
    ): TriggeredAbility {
        val newTrigger = replaceTrigger(ability.trigger, component)
        val newEffect = replaceEffect(ability.effect, component)
        return if (newTrigger === ability.trigger && newEffect === ability.effect) {
            ability
        } else {
            ability.copy(trigger = newTrigger, effect = newEffect)
        }
    }

    /**
     * Replace creature type references in a trigger.
     */
    fun replaceTrigger(trigger: Trigger, component: TextReplacementComponent): Trigger {
        return when (trigger) {
            is OnOtherCreatureWithSubtypeEnters -> {
                val newSubtype = component.applyToSubtype(trigger.subtype)
                if (newSubtype == trigger.subtype) trigger
                else trigger.copy(subtype = newSubtype)
            }
            is OnOtherCreatureWithSubtypeDies -> {
                val newSubtype = component.applyToSubtype(trigger.subtype)
                if (newSubtype == trigger.subtype) trigger
                else trigger.copy(subtype = newSubtype)
            }
            is OnCreatureWithSubtypeEnters -> {
                val newSubtype = component.applyToSubtype(trigger.subtype)
                if (newSubtype == trigger.subtype) trigger
                else trigger.copy(subtype = newSubtype)
            }
            is OnCreatureWithSubtypeDealsCombatDamageToPlayer -> {
                val newSubtype = component.applyToSubtype(trigger.subtype)
                if (newSubtype == trigger.subtype) trigger
                else trigger.copy(subtype = newSubtype)
            }
            // All other triggers don't reference creature types
            else -> trigger
        }
    }

    /**
     * Replace creature type references in an effect tree.
     * Walks composite effects recursively.
     */
    fun replaceEffect(effect: Effect, component: TextReplacementComponent): Effect {
        return when (effect) {
            is GainLifeEffect -> {
                val newAmount = replaceDynamicAmount(effect.amount, component)
                if (newAmount === effect.amount) effect else effect.copy(amount = newAmount)
            }
            is DealDamageEffect -> {
                val newAmount = replaceDynamicAmount(effect.amount, component)
                if (newAmount === effect.amount) effect else effect.copy(amount = newAmount)
            }
            is ForEachInGroupEffect -> {
                val newFilter = replaceGroupFilter(effect.filter, component)
                val newInner = replaceEffect(effect.effect, component)
                if (newFilter === effect.filter && newInner === effect.effect) effect
                else effect.copy(filter = newFilter, effect = newInner)
            }
            is DrawCardsEffect -> {
                val newCount = replaceDynamicAmount(effect.count, component)
                if (newCount === effect.count) effect else effect.copy(count = newCount)
            }
            is CompositeEffect -> {
                val newEffects = effect.effects.map { replaceEffect(it, component) }
                if (newEffects.zip(effect.effects).all { (a, b) -> a === b }) effect
                else CompositeEffect(newEffects)
            }
            is ConditionalEffect -> {
                val newCondition = replaceCondition(effect.condition, component)
                val newEffect = replaceEffect(effect.effect, component)
                val newElseEffect = effect.elseEffect?.let { replaceEffect(it, component) }
                if (newCondition === effect.condition && newEffect === effect.effect && newElseEffect === effect.elseEffect) effect
                else effect.copy(condition = newCondition, effect = newEffect, elseEffect = newElseEffect)
            }
            is MayEffect -> {
                val newEffect = replaceEffect(effect.effect, component)
                if (newEffect === effect.effect) effect
                else effect.copy(effect = newEffect)
            }
            is GainControlByMostOfSubtypeEffect -> {
                val newSubtype = component.applyToSubtype(effect.subtype)
                if (newSubtype == effect.subtype) effect else effect.copy(subtype = newSubtype)
            }
            // Effects that don't contain creature type references pass through unchanged
            else -> effect
        }
    }

    /**
     * Replace creature type references in a DynamicAmount.
     */
    fun replaceDynamicAmount(amount: DynamicAmount, component: TextReplacementComponent): DynamicAmount {
        return when (amount) {
            is DynamicAmount.CountBattlefield -> {
                val newFilter = replaceGameObjectFilter(amount.filter, component)
                if (newFilter === amount.filter) amount else amount.copy(filter = newFilter)
            }
            is DynamicAmount.Count -> {
                val newFilter = replaceGameObjectFilter(amount.filter, component)
                if (newFilter === amount.filter) amount else amount.copy(filter = newFilter)
            }
            is DynamicAmount.Add -> {
                val newLeft = replaceDynamicAmount(amount.left, component)
                val newRight = replaceDynamicAmount(amount.right, component)
                if (newLeft === amount.left && newRight === amount.right) amount
                else DynamicAmount.Add(newLeft, newRight)
            }
            is DynamicAmount.Subtract -> {
                val newLeft = replaceDynamicAmount(amount.left, component)
                val newRight = replaceDynamicAmount(amount.right, component)
                if (newLeft === amount.left && newRight === amount.right) amount
                else DynamicAmount.Subtract(newLeft, newRight)
            }
            is DynamicAmount.Multiply -> {
                val newInner = replaceDynamicAmount(amount.amount, component)
                if (newInner === amount.amount) amount else amount.copy(amount = newInner)
            }
            is DynamicAmount.IfPositive -> {
                val newInner = replaceDynamicAmount(amount.amount, component)
                if (newInner === amount.amount) amount else DynamicAmount.IfPositive(newInner)
            }
            is DynamicAmount.Max -> {
                val newLeft = replaceDynamicAmount(amount.left, component)
                val newRight = replaceDynamicAmount(amount.right, component)
                if (newLeft === amount.left && newRight === amount.right) amount
                else DynamicAmount.Max(newLeft, newRight)
            }
            is DynamicAmount.Min -> {
                val newLeft = replaceDynamicAmount(amount.left, component)
                val newRight = replaceDynamicAmount(amount.right, component)
                if (newLeft === amount.left && newRight === amount.right) amount
                else DynamicAmount.Min(newLeft, newRight)
            }
            // Leaf amounts that don't contain filters
            else -> amount
        }
    }

    /**
     * Replace creature type references in a GameObjectFilter.
     */
    fun replaceGameObjectFilter(filter: GameObjectFilter, component: TextReplacementComponent): GameObjectFilter {
        val newPredicates = filter.cardPredicates.map { replaceCardPredicate(it, component) }
        if (newPredicates.zip(filter.cardPredicates).all { (a, b) -> a === b }) return filter
        return filter.copy(cardPredicates = newPredicates)
    }

    /**
     * Replace creature type references in a CardPredicate.
     */
    fun replaceCardPredicate(predicate: CardPredicate, component: TextReplacementComponent): CardPredicate {
        return when (predicate) {
            is CardPredicate.HasSubtype -> {
                val newSubtype = component.applyToSubtype(predicate.subtype)
                if (newSubtype == predicate.subtype) predicate
                else CardPredicate.HasSubtype(newSubtype)
            }
            is CardPredicate.NotSubtype -> {
                val newSubtype = component.applyToSubtype(predicate.subtype)
                if (newSubtype == predicate.subtype) predicate
                else CardPredicate.NotSubtype(newSubtype)
            }
            is CardPredicate.And -> {
                val newPreds = predicate.predicates.map { replaceCardPredicate(it, component) }
                if (newPreds.zip(predicate.predicates).all { (a, b) -> a === b }) predicate
                else CardPredicate.And(newPreds)
            }
            is CardPredicate.Or -> {
                val newPreds = predicate.predicates.map { replaceCardPredicate(it, component) }
                if (newPreds.zip(predicate.predicates).all { (a, b) -> a === b }) predicate
                else CardPredicate.Or(newPreds)
            }
            is CardPredicate.Not -> {
                val newInner = replaceCardPredicate(predicate.predicate, component)
                if (newInner === predicate.predicate) predicate
                else CardPredicate.Not(newInner)
            }
            // All other predicates don't reference creature types
            else -> predicate
        }
    }

    /**
     * Replace creature type references in a GroupFilter.
     */
    fun replaceGroupFilter(filter: GroupFilter, component: TextReplacementComponent): GroupFilter {
        val newBase = replaceGameObjectFilter(filter.baseFilter, component)
        return if (newBase === filter.baseFilter) filter else filter.copy(baseFilter = newBase)
    }

    /**
     * Replace creature type references in an AffectsFilter.
     */
    fun replaceAffectsFilter(filter: AffectsFilter, component: TextReplacementComponent): AffectsFilter {
        return when (filter) {
            is AffectsFilter.WithSubtype -> {
                val replaced = component.applyToCreatureType(filter.subtype)
                if (replaced == filter.subtype) filter else AffectsFilter.WithSubtype(replaced)
            }
            is AffectsFilter.OtherCreaturesWithSubtype -> {
                val replaced = component.applyToCreatureType(filter.subtype)
                if (replaced == filter.subtype) filter else AffectsFilter.OtherCreaturesWithSubtype(replaced)
            }
            else -> filter
        }
    }

    /**
     * Replace creature type references in a TargetFilter.
     */
    fun replaceTargetFilter(filter: TargetFilter, component: TextReplacementComponent): TargetFilter {
        val newBase = replaceGameObjectFilter(filter.baseFilter, component)
        return if (newBase === filter.baseFilter) filter else filter.copy(baseFilter = newBase)
    }

    /**
     * Replace creature type references in a TargetRequirement.
     */
    fun replaceTargetRequirement(requirement: TargetRequirement, component: TextReplacementComponent): TargetRequirement {
        return when (requirement) {
            is TargetCreature -> {
                val newFilter = replaceTargetFilter(requirement.filter, component)
                if (newFilter === requirement.filter) requirement else requirement.copy(filter = newFilter)
            }
            is TargetPermanent -> {
                val newFilter = replaceTargetFilter(requirement.filter, component)
                if (newFilter === requirement.filter) requirement else requirement.copy(filter = newFilter)
            }
            is TargetObject -> {
                val newFilter = replaceTargetFilter(requirement.filter, component)
                if (newFilter === requirement.filter) requirement else requirement.copy(filter = newFilter)
            }
            is TargetSpell -> {
                val newFilter = replaceTargetFilter(requirement.filter, component)
                if (newFilter === requirement.filter) requirement else requirement.copy(filter = newFilter)
            }
            is TargetOther -> {
                val newBase = replaceTargetRequirement(requirement.baseRequirement, component)
                if (newBase === requirement.baseRequirement) requirement else requirement.copy(baseRequirement = newBase)
            }
            // TargetPlayer, TargetOpponent, AnyTarget, TargetCreatureOrPlayer,
            // TargetCreatureOrPlaneswalker, TargetSpellOrPermanent don't have filters
            else -> requirement
        }
    }

    /**
     * Replace creature type references in an AbilityCost.
     */
    fun replaceAbilityCost(cost: AbilityCost, component: TextReplacementComponent): AbilityCost {
        return when (cost) {
            is AbilityCost.Sacrifice -> {
                val newFilter = replaceGameObjectFilter(cost.filter, component)
                if (newFilter === cost.filter) cost else cost.copy(filter = newFilter)
            }
            is AbilityCost.TapPermanents -> {
                val newFilter = replaceGameObjectFilter(cost.filter, component)
                if (newFilter === cost.filter) cost else cost.copy(filter = newFilter)
            }
            is AbilityCost.Discard -> {
                val newFilter = replaceGameObjectFilter(cost.filter, component)
                if (newFilter === cost.filter) cost else cost.copy(filter = newFilter)
            }
            is AbilityCost.ExileFromGraveyard -> {
                val newFilter = replaceGameObjectFilter(cost.filter, component)
                if (newFilter === cost.filter) cost else cost.copy(filter = newFilter)
            }
            is AbilityCost.Composite -> {
                val newCosts = cost.costs.map { replaceAbilityCost(it, component) }
                if (newCosts.zip(cost.costs).all { (a, b) -> a === b }) cost
                else AbilityCost.Composite(newCosts)
            }
            // Tap, Untap, Mana, PayLife, DiscardSelf, SacrificeSelf, Loyalty don't have filters
            else -> cost
        }
    }

    /**
     * Replace creature type references in a Condition.
     */
    fun replaceCondition(condition: Condition, component: TextReplacementComponent): Condition {
        return when (condition) {
            is APlayerControlsMostOfSubtype -> {
                val newSubtype = component.applyToSubtype(condition.subtype)
                if (newSubtype == condition.subtype) condition else APlayerControlsMostOfSubtype(newSubtype)
            }
            is ControlCreatureOfType -> {
                val newSubtype = component.applyToSubtype(condition.subtype)
                if (newSubtype == condition.subtype) condition else ControlCreatureOfType(newSubtype)
            }
            is GraveyardContainsSubtype -> {
                val newSubtype = component.applyToSubtype(condition.subtype)
                if (newSubtype == condition.subtype) condition else GraveyardContainsSubtype(newSubtype)
            }
            // All other conditions don't reference creature types
            else -> condition
        }
    }
}
