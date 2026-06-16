package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
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
 * - Aura/Equipment consistency
 * - SuccessCriterion.Auto on action shapes it can't infer
 * - Structural dataflow mistakes — see [CardLinter]: pipeline-variable reads with no writer,
 *   target indices / `BoundVariable` names that don't resolve against the owning ability's
 *   requirements, choice-slot reads with no declaration, unused stores.
 */
object CardValidator {

    fun validate(card: CardDefinition): List<CardValidationError> {
        val errors = mutableListOf<CardValidationError>()

        validateCreatureStats(card, errors)
        validateAuraConsistency(card, errors)
        validateEquipmentConsistency(card, errors)
        validatePlaneswalkerLoyalty(card, errors)
        validateSuccessCriteria(card, errors)
        errors.addAll(CardLinter.lint(card))

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
}

/**
 * Severity of a validation finding. [ERROR]s are structural mistakes that make part of the card
 * a silent no-op (the corpus gate fails on them); [WARNING]s are legal-but-suspicious patterns
 * (cross-resolution reads, unused stores) surfaced for review without failing the build.
 */
enum class LintSeverity { ERROR, WARNING }

/**
 * Validation errors found during card validation.
 */
sealed interface CardValidationError {
    val cardName: String
    val message: String
    val severity: LintSeverity get() = LintSeverity.ERROR

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

    /** A pipeline-variable read whose name is written nowhere on the card — a typo / silent no-op. */
    data class UnresolvedPipelineRead(
        override val cardName: String,
        override val message: String
    ) : CardValidationError

    /** A read whose only writer comes later in the same resolution's pipeline. */
    data class PipelineReadBeforeWrite(
        override val cardName: String,
        override val message: String
    ) : CardValidationError {
        override val severity: LintSeverity get() = LintSeverity.WARNING
    }

    /** A read satisfied only by a writer in a different ability's resolution. */
    data class CrossScopePipelineRead(
        override val cardName: String,
        override val message: String
    ) : CardValidationError {
        override val severity: LintSeverity get() = LintSeverity.WARNING
    }

    /** An explicitly named store that no step on the card ever reads. */
    data class OrphanPipelineWrite(
        override val cardName: String,
        override val message: String
    ) : CardValidationError {
        override val severity: LintSeverity get() = LintSeverity.WARNING
    }

    /** A `BoundVariable` name that matches no target-requirement id in the owning ability. */
    data class UnknownTargetBinding(
        override val cardName: String,
        override val message: String
    ) : CardValidationError

    /** A choice-slot read (`CastChoiceMade`, `HasChosenColor`, …) with no declaring ability. */
    data class UndeclaredChoiceSlotRead(
        override val cardName: String,
        override val message: String
    ) : CardValidationError

    /** A `SourceChosenModeIs` id that matches no declared `EntersWithChoice` mode option. */
    data class UnknownModeId(
        override val cardName: String,
        override val message: String
    ) : CardValidationError

    /**
     * A string field whose name follows the pipeline-variable naming conventions but whose
     * `(type, field)` pair is not classified in [CardLinter]'s dataflow registry — classify
     * it (READ/WRITE + namespace) or list it as a known non-dataflow field.
     */
    data class UnclassifiedDataflowField(
        override val cardName: String,
        override val message: String
    ) : CardValidationError

    /**
     * A `TargetChooser.Opponent` ("… of an opponent's choice") target requirement in a context
     * that doesn't route the selection to an opponent. Only activated abilities honor the chooser
     * today; anywhere else (a spell, a triggered ability, a kicker target) the controller would
     * silently pick the target instead. Fail at card load rather than mis-resolve.
     */
    data class UnsupportedOpponentChooser(
        override val cardName: String,
        override val message: String
    ) : CardValidationError

    /**
     * An `EntityMatches` condition naming an entity role the `ConditionEvaluator` doesn't
     * dispatch (anything outside Self / EnchantedPermanent / EnchantedCreature / EquippedCreature /
     * ContextTarget / TriggeringEntity). The evaluator answers `false` for such a role, so the
     * condition would be a silent constant — fail at card load instead.
     */
    data class UnsupportedEntityMatchesRole(
        override val cardName: String,
        override val message: String
    ) : CardValidationError
}
