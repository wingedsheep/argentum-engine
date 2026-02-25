package com.wingedsheep.sdk.serialization

import kotlinx.serialization.json.*

/**
 * Transforms card JSON between verbose and compact forms.
 *
 * **Compact** (for export): Simplifies singleton polymorphic objects like
 * `{"type": "EntersBattlefield"}` to just `"EntersBattlefield"`.
 *
 * **Expand** (for loading): Reverses the compact transformation, restoring
 * strings in known polymorphic positions to `{"type": "..."}` objects.
 *
 * This transformation is applied at the CardExporter/CardLoader boundary,
 * keeping the core kotlinx.serialization infrastructure unchanged.
 */
object CompactJsonTransformer {

    /**
     * Keys whose values are exclusively DynamicAmount sealed types.
     * Integer primitives in these positions are expanded to `{"type": "Fixed", "amount": n}`
     * during loading (backward compatibility for pre-DynamicAmount JSON files).
     * Note: "amount" is NOT included because it is `Int` in many effects.
     */
    private val DYNAMIC_AMOUNT_INTEGER_KEYS = setOf(
        "powerModifier", "toughnessModifier", "dynamicPower", "dynamicToughness",
    )

    /**
     * Keys whose values are single polymorphic sealed-type objects.
     * Strings in these positions are expanded to `{"type": "..."}` during loading.
     */
    private val POLYMORPHIC_OBJECT_KEYS = setOf(
        // Effect / Trigger / Target
        "trigger", "effect", "target", "spellEffect",
        // Nested effects (composites, conditionals, reflexive triggers)
        "reflexiveEffect", "action", "ifTrue", "ifFalse",
        // Conditions
        "condition", "triggerCondition",
        // Costs and requirements
        "cost", "targetRequirement", "baseRequirement", "copyTargetRequirement",
        // Damage
        "damageType",
        // Filters and predicates
        "controllerPredicate", "statePredicate", "predicate",
        // Static ability references
        "ability",
        // Pipeline effect internals
        "source", "selection",
        // Player references (sealed interface, e.g., in AggregateBattlefield)
        "player",
        // DynamicAmount fields (sealed interface used for numeric expressions)
        "amount", "count", "amountSource", "powerModifier", "toughnessModifier",
        "perPlayerAmount", "dynamicPower", "dynamicToughness",
        // Compare condition operands (sealed interface DynamicAmount)
        "left", "right",
        // Divide operands (sealed interface DynamicAmount)
        "numerator", "denominator",
        // Event and Effect Parameters
        "recipient", "controller", "timing", "duration",
        "reductionSource", "copyCost", "repeatCondition",
        // Damage source override (EffectTarget sealed interface)
        "damageSource"
    )

    /**
     * Known singleton names for CounterTypeFilter. Used to disambiguate
     * CounterTypeFilter objects from standard String counter types.
     */
    private val COUNTER_FILTER_SINGLETONS = setOf(
        "CounterAny", "PlusOnePlusOne", "MinusOneMinusOne", "Loyalty"
    )

    /**
     * Keys whose values are arrays of polymorphic sealed-type objects.
     */
    private val POLYMORPHIC_ARRAY_KEYS = setOf(
        "cardPredicates", "effects", "costs",
        "targetRequirements", "staticAbilities",
        "predicates", "conditions", "statePredicates",
        "restrictions",
    )

    /**
     * Keys whose values are GameObjectFilter JSON objects that can be
     * compacted to query strings via [FilterQueryLanguage].
     */
    private val FILTER_KEYS = setOf(
        "filter", "baseFilter", "sourceFilter",
        "tokenFilter", "cardFilter", "matchFilter", "targetFilter"
    )

    /**
     * Compact a JSON element by replacing singleton polymorphic objects with strings.
     *
     * Rules:
     * - `{"type": "Foo"}` (single key "type") → `"Foo"`
     * - All other elements are recursed into unchanged
     */
    fun compact(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> {
            if (isSingletonObject(element)) {
                // Compact: {"type": "X"} → "X"
                element["type"]!!
            } else {
                // Recurse into all values, compacting filters to query strings
                JsonObject(element.mapValues { (key, v) -> compactValue(key, v) })
            }
        }
        is JsonArray -> JsonArray(element.map { compact(it) })
        is JsonPrimitive -> element
    }

    private fun compactValue(key: String, value: JsonElement): JsonElement {
        // Try to compact GameObjectFilter objects to query strings
        if (key in FILTER_KEYS && value is JsonObject && FilterQueryLanguage.isGameObjectFilter(value)) {
            // First compact the inner elements (e.g., singleton predicates)
            val compacted = compact(value) as JsonObject
            val query = FilterQueryLanguage.formatFilter(compacted)
            if (query != null) return JsonPrimitive(query)
            // If the query language can't represent it, fall through to normal compaction
            return compacted
        }
        return compact(value)
    }

    /**
     * Expand a JSON element by restoring compacted strings to polymorphic objects.
     *
     * Rules:
     * - String values under known polymorphic keys → `{"type": "string"}`
     * - String elements in known polymorphic arrays → `{"type": "string"}`
     * - ManaCost strings (containing `{`) are never expanded
     */
    fun expand(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> {
            JsonObject(element.mapValues { (key, value) ->
                when {
                    // Filter query string → expand to GameObjectFilter JSON
                    key in FILTER_KEYS && value is JsonPrimitive && value.isString ->
                        expand(FilterQueryLanguage.parseFilter(value.content))

                    // Special case for counterType due to name collision (String vs CounterTypeFilter)
                    key == "counterType" && value is JsonPrimitive && value.isString
                            && value.content in COUNTER_FILTER_SINGLETONS ->
                        buildJsonObject { put("type", value.content) }

                    // String value under a polymorphic key → expand to {"type": "..."}
                    key in POLYMORPHIC_OBJECT_KEYS && value is JsonPrimitive && value.isString
                            && !value.content.contains("{") ->
                        buildJsonObject { put("type", value.content) }

                    // Integer under a DynamicAmount key → expand to {"type": "Fixed", "amount": n}
                    // (backward compatibility for pre-DynamicAmount JSON files)
                    key in DYNAMIC_AMOUNT_INTEGER_KEYS && value is JsonPrimitive
                            && !value.isString && value.intOrNull != null ->
                        buildJsonObject { put("type", "Fixed"); put("amount", value.int) }

                    // Array under a polymorphic array key → expand string elements
                    key in POLYMORPHIC_ARRAY_KEYS && value is JsonArray ->
                        JsonArray(value.map { expandArrayElement(it) })

                    // Recurse into nested structures
                    else -> expand(value)
                }
            })
        }
        is JsonArray -> JsonArray(element.map { expand(it) })
        is JsonPrimitive -> element
    }

    private fun expandArrayElement(element: JsonElement): JsonElement = when {
        element is JsonPrimitive && element.isString && !element.content.contains("{") ->
            buildJsonObject { put("type", element.content) }
        else -> expand(element)
    }

    /**
     * Check if a JSON object is a singleton polymorphic object: exactly one key "type"
     * with a string value.
     */
    private fun isSingletonObject(obj: JsonObject): Boolean {
        return obj.size == 1
                && obj.containsKey("type")
                && obj["type"] is JsonPrimitive
                && (obj["type"] as JsonPrimitive).isString
    }
}
