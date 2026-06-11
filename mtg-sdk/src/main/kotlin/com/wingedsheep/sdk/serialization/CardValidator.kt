package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AnyPlayerMayPayEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.ChangeCreatureTypeTextEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ExileUntilLeavesEffect
import com.wingedsheep.sdk.scripting.effects.FlipCoinEffect
import com.wingedsheep.sdk.scripting.effects.LoseGameEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.effects.GainControlByMostEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.LookAtFaceDownEffect
import com.wingedsheep.sdk.scripting.effects.LoseAllCreatureTypesEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.MustBeBlockedEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.RemoveDamageShieldEffect
import com.wingedsheep.sdk.scripting.effects.RemoveFromCombatEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.effects.TurnFaceDownEffect
import com.wingedsheep.sdk.scripting.effects.TurnFaceUpEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Post-deserialization validation for CardDefinition objects.
 *
 * Catches errors that the type system can't, such as:
 * - Creatures without stats
 * - Target index out of bounds
 * - Aura/Equipment consistency
 * - Orphaned targets
 * - SuccessCriterion.Auto on action shapes it can't infer
 */
object CardValidator {

    fun validate(card: CardDefinition): List<CardValidationError> {
        val errors = mutableListOf<CardValidationError>()

        validateCreatureStats(card, errors)
        validateTargetIndices(card, errors)
        validateAuraConsistency(card, errors)
        validateEquipmentConsistency(card, errors)
        validatePlaneswalkerLoyalty(card, errors)
        validateSuccessCriteria(card, errors)

        return errors
    }

    /**
     * Every `Gate.DoAction` whose criterion is [SuccessCriterion.Auto] must wrap an action
     * shape Auto can actually infer ([SuccessCriterion.Auto.canInfer]). Unknown shapes used
     * to fall through to "it happened" (fail-open); now they are a card-load error asking
     * for an explicit criterion.
     *
     * The card is scanned via its serialized JSON tree rather than a hand-maintained
     * effect-container walk, so `DoAction` gates are found wherever they sit — spell
     * effects, triggered/activated abilities, modes, faces, granted abilities — including
     * containers added after this validator was written.
     */
    private fun validateSuccessCriteria(card: CardDefinition, errors: MutableList<CardValidationError>) {
        val tree = CardSerialization.json.encodeToJsonElement(CardDefinition.serializer(), card)
        forEachJsonObject(tree) { obj ->
            if (obj["type"]?.jsonPrimitive?.contentOrNull != "Gate.DoAction") return@forEachJsonObject
            // encodeDefaults=false: an absent successCriterion field IS the Auto default.
            val criterion = obj["successCriterion"]
            val isAuto = criterion == null ||
                (criterion as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull == "SuccessCriterion.Auto"
            if (!isAuto) return@forEachJsonObject

            val actionJson = obj["action"] ?: return@forEachJsonObject
            val action = CardSerialization.json.decodeFromJsonElement(Effect.serializer(), actionJson)
            if (!SuccessCriterion.Auto.canInfer(action)) {
                errors.add(
                    CardValidationError.UninferableSuccessCriterion(
                        cardName = card.name,
                        message = "'${card.name}' has an \"if you do\" gate (Gate.DoAction) whose action " +
                            "('${action.description}') has no shape SuccessCriterion.Auto can infer success from " +
                            "(terminal collection move to a zone, or a single move of Self). " +
                            "Specify an explicit criterion (SuccessCriterion.Always / CollectionNonEmpty)."
                    )
                )
            }
        }
    }

    /** Depth-first visit of every [JsonObject] in [element], including nested arrays. */
    private fun forEachJsonObject(element: JsonElement, visit: (JsonObject) -> Unit) {
        when (element) {
            is JsonObject -> {
                visit(element)
                element.values.forEach { forEachJsonObject(it, visit) }
            }
            is JsonArray -> element.forEach { forEachJsonObject(it, visit) }
            else -> {}
        }
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
            is GatedEffect -> {
                when (val gate = effect.gate) {
                    is Gate.MayPay -> collectIndicesRecursive(gate.cost, indices)
                    is Gate.DoAction -> collectIndicesRecursive(gate.action, indices)
                    is Gate.MayDecide -> {}
                    is Gate.WhenCondition -> {}
                    is Gate.MayPayX -> {}
                }
                collectIndicesRecursive(effect.then, indices)
                effect.otherwise?.let { collectIndicesRecursive(it, indices) }
            }
            is ReflexiveTriggerEffect -> {
                collectIndicesRecursive(effect.action, indices)
                collectIndicesRecursive(effect.reflexiveEffect, indices)
            }
            is ModalEffect -> effect.modes.forEach { collectIndicesRecursive(it.effect, indices) }
            is PayOrSufferEffect -> collectIndicesRecursive(effect.suffer, indices)
            is AnyPlayerMayPayEffect -> {
                effect.consequence?.let { collectIndicesRecursive(it, indices) }
                effect.consequenceIfNonePaid?.let { collectIndicesRecursive(it, indices) }
            }
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
            is GrantKeywordEffect -> effect.target
            is RegenerateEffect -> effect.target
            is RemoveDamageShieldEffect -> effect.target
            is CantBeRegeneratedEffect -> effect.target
            is ExileUntilLeavesEffect -> effect.target
            is MustBeBlockedEffect -> effect.target
            is RemoveFromCombatEffect -> effect.target
            is ForceSacrificeEffect -> effect.target
            is MarkExileOnDeathEffect -> effect.target
            is GainControlEffect -> effect.target
            is TurnFaceDownEffect -> effect.target
            is TurnFaceUpEffect -> effect.target
            is BecomeCreatureTypeEffect -> effect.target
            is ChangeCreatureTypeTextEffect -> effect.target
            is GrantTriggeredAbilityEffect -> effect.target
            is LoseAllCreatureTypesEffect -> effect.target
            is LookAtFaceDownEffect -> effect.target
            is TransformEffect -> effect.target
            is GainControlByMostEffect -> effect.target
            is SacrificeTargetEffect -> effect.target
            is LoseGameEffect -> effect.target
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

    data class UninferableSuccessCriterion(
        override val cardName: String,
        override val message: String
    ) : CardValidationError
}
