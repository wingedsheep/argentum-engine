package com.wingedsheep.assay.grammar

import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.serialization.CardSerialization
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Renaming the **name that links a target requirement to the effect that reads it**, inside one
 * parsed clause.
 *
 * ## Why this exists
 *
 * Every rule in this grammar mints [Targets.SLOT] for the one target it declares, which is enough
 * right up to the point where a *line* declares two — "Destroy target land. ~ deals 13 damage to
 * target creature." Each clause parses on its own and each calls its slot `target`, so folding them
 * into one script would produce two requirements with one name and two effects reading it: a model
 * in which the second target cannot be referred to. [Steps.merge] used to refuse that fold outright
 * and say so in its KDoc; this is what closes it. [Targets.slot] has held the numbering since the
 * Legions band and had no caller.
 *
 * ## Why it goes through JSON
 *
 * An [com.wingedsheep.sdk.scripting.effects.Effect] is a deep sealed tree with no visitor, so there
 * is no typed way to rewrite one field wherever it occurs. The gate's own
 * `Differential.normalizeSlotNames` already renames slots over the serialized form for the same
 * reason, and this is that operation aimed the other way — one name to one name, rather than every
 * name to its position.
 *
 * Two things keep it honest rather than approximate:
 *
 * - **The keys are named, not guessed.** Only a `"id"` field and the `"name"` of an object whose
 *   discriminator is `BoundVariable` are slot names; a card name, a counter kind or a token's name
 *   is left alone even if a card were somehow called "target".
 * - **Every rename is verified by undoing it.** [rename] renames back and compares, so a lossy
 *   encode or a field the serializer drops fails closed to `null` instead of silently changing a
 *   model. A phrase never throws, so a serializer that refuses a value declines the line.
 */
internal object Slots {

    /** `BoundVariable`'s discriminator value under [CardSerialization]'s `classDiscriminator`. */
    private const val BOUND_VARIABLE = "BoundVariable"

    /**
     * [script] with every reference to slot [from] renamed to [to], or null when the rename cannot
     * be shown to be faithful.
     *
     * Identity when the two names are equal, which is the case for the *first* declaring clause of
     * every line — so a single-target line, which is all of them until now, does not touch this file
     * at all.
     */
    fun rename(script: CardScript, from: String, to: String): CardScript? {
        if (from == to) return script
        val renamed = map(script) { rewrite(it, from, to) } ?: return null
        val undone = map(renamed) { rewrite(it, to, from) } ?: return null
        return renamed.takeIf { undone == script }
    }

    /** Whether [script] refers to slot [name] anywhere — a clause reading a target it did not declare. */
    fun references(script: CardScript, name: String): Boolean {
        val tree = runCatching {
            CardSerialization.json.encodeToJsonElement(CardScript.serializer(), script)
        }.getOrNull() ?: return false
        return refers(tree, name)
    }

    private fun map(script: CardScript, f: (JsonElement) -> JsonElement): CardScript? = runCatching {
        val json = CardSerialization.json
        json.decodeFromJsonElement(
            CardScript.serializer(),
            f(json.encodeToJsonElement(CardScript.serializer(), script)),
        )
    }.getOrNull()

    private fun rewrite(element: JsonElement, from: String, to: String): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.mapValues { (key, value) ->
                if (isSlotName(element, key, value) && value.text() == from) JsonPrimitive(to)
                else rewrite(value, from, to)
            }
        )

        is JsonArray -> JsonArray(element.map { rewrite(it, from, to) })
        else -> element
    }

    private fun refers(element: JsonElement, name: String): Boolean = when (element) {
        is JsonObject -> element.any { (key, value) ->
            if (isSlotName(element, key, value)) value.text() == name else refers(value, name)
        }

        is JsonArray -> element.any { refers(it, name) }
        else -> false
    }

    /** The two places a slot name is written: a requirement declares one, a bound variable reads one. */
    private fun isSlotName(owner: JsonObject, key: String, value: JsonElement): Boolean =
        value.text() != null && when (key) {
            "id" -> true
            "name" -> owner["type"].text() == BOUND_VARIABLE
            else -> false
        }

    private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
}
