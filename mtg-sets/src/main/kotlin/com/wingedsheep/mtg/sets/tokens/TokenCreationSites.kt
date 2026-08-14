package com.wingedsheep.mtg.sets.tokens

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.serialization.CardSerialization
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Every token a card can create, read out of its serialised tree.
 *
 * Walking the JSON rather than the effect objects is what makes this total: a `CreateToken` nested
 * in a composite, a mode, a pipeline, a reflexive trigger, a granted ability or a
 * `ConvertCountersToTokens` is found the same as a top-level one, and a new effect type that wraps
 * token creation is picked up without touching this code.
 *
 * Shared by the token-art gap report and the corpus coverage test so the two can't disagree about
 * what "a token this card creates" means.
 */
object TokenCreationSites {

    /**
     * One token a card can create.
     *
     * @property tokenName Name the engine will give the token — the effect's explicit `name`, else
     *   its creature types joined, matching `CreateTokenExecutor`. Predefined tokens use their
     *   `tokenType` ("Treasure").
     * @property creatureTypes Empty for predefined (noncreature) tokens.
     * @property predefined True for `CreatePredefinedToken` (Treasure, Map, Clue, Role, …), which
     *   resolves art through `PredefinedTokens` rather than the generic creature-type table.
     * @property explicitImageUri Art hardcoded on the effect, when the card pins one. Present here
     *   so callers can report it as a migration candidate — art belongs on `MtgSet.tokenArt`.
     * @property chosenType True when the creature type is picked at resolution time (Riptide
     *   Replicator). Nothing is statically knowable about such a token's identity.
     */
    data class Site(
        val tokenName: String,
        val creatureTypes: List<String>,
        val power: Int?,
        val toughness: Int?,
        val colors: Set<com.wingedsheep.sdk.core.Color>,
        val predefined: Boolean,
        val explicitImageUri: String?,
        val chosenType: Boolean,
    )

    fun of(card: CardDefinition): List<Site> =
        nodes(card).mapNotNull(::toSite)

    private fun nodes(card: CardDefinition): List<JsonObject> {
        val found = mutableListOf<JsonObject>()
        fun walk(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    when ((element["type"] as? JsonPrimitive)?.contentOrNull) {
                        "CreateToken", "CreatePredefinedToken" -> found += element
                    }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(CardSerialization.json.encodeToJsonElement(CardDefinition.serializer(), card))
        return found
    }

    private fun toSite(node: JsonObject): Site? {
        val predefined = (node["type"] as? JsonPrimitive)?.contentOrNull == "CreatePredefinedToken"
        val explicit = node["imageUri"]?.jsonPrimitiveOrNull()?.contentOrNull

        if (predefined) {
            val tokenType = node["tokenType"]?.jsonPrimitiveOrNull()?.contentOrNull ?: return null
            return Site(
                tokenName = tokenType,
                creatureTypes = emptyList(),
                power = null,
                toughness = null,
                colors = emptySet(),
                predefined = true,
                explicitImageUri = explicit,
                chosenType = false,
            )
        }

        val creatureTypes = (node["creatureTypes"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        val colors = (node["colors"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .mapNotNullTo(mutableSetOf()) { name ->
                runCatching { com.wingedsheep.sdk.core.Color.valueOf(name) }.getOrNull()
            }
        return Site(
            tokenName = node["name"]?.jsonPrimitiveOrNull()?.contentOrNull
                ?: creatureTypes.joinToString(" "),
            creatureTypes = creatureTypes,
            power = node["power"]?.jsonPrimitiveOrNull()?.intOrNull,
            toughness = node["toughness"]?.jsonPrimitiveOrNull()?.intOrNull,
            colors = colors,
            predefined = false,
            explicitImageUri = explicit,
            chosenType = node["creatureTypesFromChoice"] != null,
        )
    }

    private fun JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive
}
