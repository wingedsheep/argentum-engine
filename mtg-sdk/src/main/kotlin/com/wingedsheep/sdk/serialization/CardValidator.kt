package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddMinusCountersEffect
import com.wingedsheep.sdk.scripting.effects.AnyPlayerMayPayEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.BlightEffect
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.ChangeCreatureTypeTextEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DestroyAtEndOfCombatEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ExileAndReplaceWithTokenEffect
import com.wingedsheep.sdk.scripting.effects.ExileUntilLeavesEffect
import com.wingedsheep.sdk.scripting.effects.FlipCoinEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.GainControlByMostOfSubtypeEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.LookAtFaceDownCreatureEffect
import com.wingedsheep.sdk.scripting.effects.LoseAllCreatureTypesEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.MustBeBlockedEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.PreventNextDamageEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.RemoveFromCombatEffect
import com.wingedsheep.sdk.scripting.effects.StoreCountEffect
import com.wingedsheep.sdk.scripting.effects.StoreResultEffect
import com.wingedsheep.sdk.scripting.effects.TapCreatureForEffectEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.TransformAllCreaturesEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.effects.TurnFaceDownEffect
import com.wingedsheep.sdk.scripting.effects.TurnFaceUpEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Post-deserialization validation for CardDefinition objects.
 *
 * Catches errors that the type system can't, such as:
 * - Creatures without stats
 * - Target index out of bounds
 * - Aura/Equipment consistency
 * - Orphaned targets
 */
object CardValidator {

    fun validate(card: CardDefinition): List<CardValidationError> {
        val errors = mutableListOf<CardValidationError>()

        validateCreatureStats(card, errors)
        validateTargetIndices(card, errors)
        validateAuraConsistency(card, errors)
        validateEquipmentConsistency(card, errors)
        validatePlaneswalkerLoyalty(card, errors)

        return errors
    }

    private fun validateCreatureStats(card: CardDefinition, errors: MutableList<CardValidationError>) {
        if (card.typeLine.isCreature && card.creatureStats == null) {
            errors.add(
                CardValidationError.MissingCreatureStats(
                    cardName = card.name,
                    message = "Creature '${card.name}' is missing power/toughness"
                )
            )
        }
    }

    private fun validateTargetIndices(card: CardDefinition, errors: MutableList<CardValidationError>) {
        val maxIndex = card.script.targetRequirements.size - 1

        // Check spell effect
        card.script.spellEffect?.let { effect ->
            collectContextTargetIndices(effect).forEach { index ->
                if (index > maxIndex) {
                    errors.add(
                        CardValidationError.InvalidTargetIndex(
                            cardName = card.name,
                            index = index,
                            maxIndex = maxIndex,
                            message = "Spell effect references target index $index but only ${maxIndex + 1} target(s) declared in '${card.name}'"
                        )
                    )
                }
            }
        }

        // Check triggered ability effects
        card.script.triggeredAbilities.forEach { ability ->
            val abilityMaxIndex = if (ability.targetRequirement != null) 0 else -1
            collectContextTargetIndices(ability.effect).forEach { index ->
                if (index > abilityMaxIndex) {
                    errors.add(
                        CardValidationError.InvalidTargetIndex(
                            cardName = card.name,
                            index = index,
                            maxIndex = abilityMaxIndex,
                            message = "Triggered ability effect references target index $index but only ${abilityMaxIndex + 1} target(s) declared in '${card.name}'"
                        )
                    )
                }
            }
        }
    }

    private fun validateAuraConsistency(card: CardDefinition, errors: MutableList<CardValidationError>) {
        if (card.script.auraTarget != null && !card.typeLine.isAura) {
            errors.add(
                CardValidationError.AuraMissingSubtype(
                    cardName = card.name,
                    message = "Card '${card.name}' has auraTarget but type line doesn't include Aura subtype"
                )
            )
        }
        if (card.typeLine.isAura && card.script.auraTarget == null) {
            errors.add(
                CardValidationError.AuraMissingTarget(
                    cardName = card.name,
                    message = "Card '${card.name}' has Aura subtype but no auraTarget defined in script"
                )
            )
        }
    }

    private fun validateEquipmentConsistency(card: CardDefinition, errors: MutableList<CardValidationError>) {
        if (card.equipCost != null && !card.typeLine.isEquipment) {
            errors.add(
                CardValidationError.EquipmentMissingSubtype(
                    cardName = card.name,
                    message = "Card '${card.name}' has equipCost but type line doesn't include Equipment subtype"
                )
            )
        }
    }

    private fun validatePlaneswalkerLoyalty(card: CardDefinition, errors: MutableList<CardValidationError>) {
        if (card.isPlaneswalker && card.startingLoyalty == null) {
            errors.add(
                CardValidationError.MissingPlaneswalkerLoyalty(
                    cardName = card.name,
                    message = "Planeswalker '${card.name}' is missing startingLoyalty"
                )
            )
        }
    }

    /**
     * Recursively collect all ContextTarget indices from an effect tree.
     */
    private fun collectContextTargetIndices(effect: Effect): List<Int> {
        val indices = mutableListOf<Int>()
        collectIndicesRecursive(effect, indices)
        return indices
    }

    private fun collectIndicesRecursive(effect: Effect, indices: MutableList<Int>) {
        when (effect) {
            is CompositeEffect -> effect.effects.forEach { collectIndicesRecursive(it, indices) }
            is ForEachTargetEffect -> effect.effects.forEach { collectIndicesRecursive(it, indices) }
            is MayEffect -> collectIndicesRecursive(effect.effect, indices)
            is ConditionalEffect -> {
                collectIndicesRecursive(effect.effect, indices)
                effect.elseEffect?.let { collectIndicesRecursive(it, indices) }
            }
            is OptionalCostEffect -> {
                collectIndicesRecursive(effect.cost, indices)
                collectIndicesRecursive(effect.ifPaid, indices)
                effect.ifNotPaid?.let { collectIndicesRecursive(it, indices) }
            }
            is ReflexiveTriggerEffect -> {
                collectIndicesRecursive(effect.action, indices)
                collectIndicesRecursive(effect.reflexiveEffect, indices)
            }
            is StoreResultEffect -> collectIndicesRecursive(effect.effect, indices)
            is StoreCountEffect -> collectIndicesRecursive(effect.effect, indices)
            is BlightEffect -> collectIndicesRecursive(effect.innerEffect, indices)
            is TapCreatureForEffectEffect -> collectIndicesRecursive(effect.innerEffect, indices)
            is MayPayManaEffect -> collectIndicesRecursive(effect.effect, indices)
            is ModalEffect -> effect.modes.forEach { collectIndicesRecursive(it.effect, indices) }
            is PayOrSufferEffect -> collectIndicesRecursive(effect.suffer, indices)
            is AnyPlayerMayPayEffect -> collectIndicesRecursive(effect.consequence, indices)
            is ForEachInGroupEffect -> collectIndicesRecursive(effect.effect, indices)
            is FlipCoinEffect -> {
                effect.wonEffect?.let { collectIndicesRecursive(it, indices) }
                effect.lostEffect?.let { collectIndicesRecursive(it, indices) }
            }
            else -> {
                // Check all EffectTarget fields for ContextTarget references
                collectTargetIndicesFromEffect(effect, indices)
            }
        }
    }

    /**
     * Extract ContextTarget indices from an effect's target fields via reflection-free approach.
     * This checks common patterns — effects that have a `target: EffectTarget` property.
     */
    private fun collectTargetIndicesFromEffect(effect: Effect, indices: MutableList<Int>) {
        // Use the effect's target field if accessible
        val target = when (effect) {
            is DealDamageEffect -> effect.target
            is ModifyStatsEffect -> effect.target
            is MoveToZoneEffect -> effect.target
            is TapUntapEffect -> effect.target
            is AddCountersEffect -> effect.target
            is RemoveCountersEffect -> effect.target
            is GrantKeywordUntilEndOfTurnEffect -> effect.target
            is RegenerateEffect -> effect.target
            is CantBeRegeneratedEffect -> effect.target
            is ExileUntilLeavesEffect -> effect.target
            is ExileAndReplaceWithTokenEffect -> effect.target
            is DestroyAtEndOfCombatEffect -> effect.target
            is MustBeBlockedEffect -> effect.target
            is PreventNextDamageEffect -> effect.target
            is RemoveFromCombatEffect -> effect.target
            is ForceSacrificeEffect -> effect.target
            is MarkExileOnDeathEffect -> effect.target
            is GainControlEffect -> effect.target
            is TurnFaceDownEffect -> effect.target
            is TurnFaceUpEffect -> effect.target
            is BecomeCreatureTypeEffect -> effect.target
            is ChangeCreatureTypeTextEffect -> effect.target
            is GrantTriggeredAbilityUntilEndOfTurnEffect -> effect.target
            is TransformAllCreaturesEffect -> effect.target
            is AddMinusCountersEffect -> effect.target
            is LoseAllCreatureTypesEffect -> effect.target
            is LookAtFaceDownCreatureEffect -> effect.target
            is TransformEffect -> effect.target
            is GainControlByMostOfSubtypeEffect -> effect.target
            else -> null
        }

        if (target is EffectTarget.ContextTarget) {
            indices.add(target.index)
        }
    }
}

/**
 * Validation errors found during card validation.
 */
sealed interface CardValidationError {
    val cardName: String
    val message: String

    data class MissingCreatureStats(
        override val cardName: String,
        override val message: String
    ) : CardValidationError

    data class InvalidTargetIndex(
        override val cardName: String,
        val index: Int,
        val maxIndex: Int,
        override val message: String
    ) : CardValidationError

    data class AuraMissingTarget(
        override val cardName: String,
        override val message: String
    ) : CardValidationError

    data class AuraMissingSubtype(
        override val cardName: String,
        override val message: String
    ) : CardValidationError

    data class EquipmentMissingSubtype(
        override val cardName: String,
        override val message: String
    ) : CardValidationError

    data class MissingPlaneswalkerLoyalty(
        override val cardName: String,
        override val message: String
    ) : CardValidationError
}
