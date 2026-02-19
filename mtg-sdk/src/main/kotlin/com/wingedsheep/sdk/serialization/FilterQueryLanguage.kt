package com.wingedsheep.sdk.serialization

import kotlinx.serialization.json.*

/**
 * Compact query language for GameObjectFilter serialization.
 *
 * Converts between verbose filter JSON and compact query strings.
 *
 * ## Query Syntax
 *
 * Terms are space-separated (AND logic):
 * ```
 * "creature Goblin"       → IsCreature AND HasSubtype(Goblin)
 * "permanent Dragon"      → IsPermanent AND HasSubtype(Dragon)
 * ```
 *
 * ### Type predicates (lowercase):
 * `creature`, `land`, `artifact`, `enchantment`, `instant`, `sorcery`,
 * `permanent`, `nonland`, `noncreature`, `token`, `nontoken`, `basicland`,
 * `planeswalker`
 *
 * ### Subtypes (capitalized):
 * `Dragon`, `Goblin`, `Elf`, `Zombie`, `Warrior`, etc.
 *
 * ### OR operator:
 * `artifact|enchantment`  → Or(IsArtifact, IsEnchantment)
 *
 * ### Controller/Owner:
 * `ctrl:you`, `ctrl:opponent`, `own:you`, `own:opponent`
 *
 * ### Comparisons:
 * `mv>=6`, `mv<=3`, `mv=5`  (mana value)
 * `pow>=3`, `pow=2`          (power)
 * `tou>=4`, `tou<=2`         (toughness)
 *
 * ### State predicates:
 * `tapped`, `untapped`, `attacking`, `blocking`, `facedown`
 *
 * ### Keywords:
 * `kw:flying`, `kw:haste`, `kw:trample`
 *
 * ### Color:
 * `white`, `blue`, `black`, `red`, `green`, `colorless`, `multicolored`
 *
 * ### Name:
 * `name:Lightning Bolt` (note: name with spaces uses the rest of the token)
 */
object FilterQueryLanguage {

    // Type predicate names → SerialName
    private val TYPE_PREDICATES = mapOf(
        "creature" to "IsCreature",
        "land" to "IsLand",
        "artifact" to "IsArtifact",
        "enchantment" to "IsEnchantment",
        "instant" to "IsInstant",
        "sorcery" to "IsSorcery",
        "permanent" to "IsPermanent",
        "nonland" to "IsNonland",
        "noncreature" to "IsNoncreature",
        "token" to "IsToken",
        "nontoken" to "IsNontoken",
        "basicland" to "IsBasicLand",
        "planeswalker" to "IsPlaneswalker",
    )

    // Reverse: SerialName → query term
    private val TYPE_PREDICATE_NAMES = TYPE_PREDICATES.entries.associate { (k, v) -> v to k }

    // State predicate names → SerialName
    private val STATE_PREDICATES = mapOf(
        "tapped" to "IsTapped",
        "untapped" to "IsUntapped",
        "attacking" to "IsAttacking",
        "blocking" to "IsBlocking",
        "facedown" to "IsFaceDown",
    )
    private val STATE_PREDICATE_NAMES = STATE_PREDICATES.entries.associate { (k, v) -> v to k }

    // Controller/Owner predicates → SerialName
    private val CONTROLLER_PREDICATES = mapOf(
        "ctrl:you" to "ControlledByYou",
        "ctrl:opponent" to "ControlledByOpponent",
        "ctrl:any" to "ControlledByAny",
        "ctrl:target-opponent" to "ControlledByTargetOpponent",
        "ctrl:target-player" to "ControlledByTargetPlayer",
        "own:you" to "OwnedByYou",
        "own:opponent" to "OwnedByOpponent",
    )
    private val CONTROLLER_PREDICATE_NAMES = CONTROLLER_PREDICATES.entries.associate { (k, v) -> v to k }

    // Color names → Color enum value
    private val COLORS = mapOf(
        "white" to "WHITE",
        "blue" to "BLUE",
        "black" to "BLACK",
        "red" to "RED",
        "green" to "GREEN",
    )
    private val COLOR_NAMES = COLORS.entries.associate { (k, v) -> v to k }

    private val COMPARISON_REGEX = Regex("""(mv|pow|tou)(>=|<=|=)(\d+)""")

    /**
     * Try to format a GameObjectFilter JSON object as a compact query string.
     * Returns null if the filter is too complex for the query language.
     */
    fun formatFilter(obj: JsonObject): String? {
        val terms = mutableListOf<String>()

        val cardPredicates = obj["cardPredicates"]?.jsonArray ?: JsonArray(emptyList())
        val statePredicates = obj["statePredicates"]?.jsonArray ?: JsonArray(emptyList())
        val controllerPredicate = obj["controllerPredicate"]
        val matchAll = obj["matchAll"]?.jsonPrimitive?.booleanOrNull ?: true

        // Format card predicates
        for (pred in cardPredicates) {
            val term = formatCardPredicate(pred, matchAll) ?: return null
            terms.add(term)
        }

        // Format state predicates
        for (pred in statePredicates) {
            val term = formatStatePredicate(pred) ?: return null
            terms.add(term)
        }

        // Format controller predicate
        if (controllerPredicate != null) {
            val term = formatControllerPredicate(controllerPredicate) ?: return null
            terms.add(term)
        }

        // If matchAll=false with multiple card predicates (not wrapped in Or), we can't represent this
        if (!matchAll && cardPredicates.size > 1) return null

        return terms.joinToString(" ").ifEmpty { null }
    }

    private fun formatCardPredicate(element: JsonElement, matchAll: Boolean): String? {
        // Compact singleton string (already compacted by CompactJsonTransformer)
        if (element is JsonPrimitive && element.isString) {
            val name = element.content
            TYPE_PREDICATE_NAMES[name]?.let { return it }
            // Singleton predicates that aren't type predicates — can't express
            return null
        }

        if (element !is JsonObject) return null
        val type = element["type"]?.jsonPrimitive?.content ?: return null

        return when (type) {
            // Type predicates (already handled as singletons above, but just in case)
            in TYPE_PREDICATE_NAMES -> TYPE_PREDICATE_NAMES[type]

            // Subtype
            "HasSubtype" -> element["subtype"]?.jsonPrimitive?.content

            // Color
            "HasColor" -> {
                val color = element["color"]?.jsonPrimitive?.content
                COLOR_NAMES[color]
            }
            "IsColorless" -> "colorless"
            "IsMulticolored" -> "multicolored"
            "IsMonocolored" -> "monocolored"

            // Keyword
            "HasKeyword" -> {
                val kw = element["keyword"]?.jsonPrimitive?.content?.lowercase()
                if (kw != null) "kw:$kw" else null
            }

            // Mana value comparisons
            "ManaValueAtLeast" -> "mv>=${element["min"]?.jsonPrimitive?.int}"
            "ManaValueAtMost" -> "mv<=${element["max"]?.jsonPrimitive?.int}"
            "ManaValueEquals" -> "mv=${element["value"]?.jsonPrimitive?.int}"

            // Power comparisons
            "PowerAtLeast" -> "pow>=${element["min"]?.jsonPrimitive?.int}"
            "PowerAtMost" -> "pow<=${element["max"]?.jsonPrimitive?.int}"
            "PowerEquals" -> "pow=${element["value"]?.jsonPrimitive?.int}"

            // Toughness comparisons
            "ToughnessAtLeast" -> "tou>=${element["min"]?.jsonPrimitive?.int}"
            "ToughnessAtMost" -> "tou<=${element["max"]?.jsonPrimitive?.int}"
            "ToughnessEquals" -> "tou=${element["value"]?.jsonPrimitive?.int}"

            // Or — format as pipe-separated terms
            "Or" -> {
                val predicates = element["predicates"]?.jsonArray ?: return null
                val subTerms = predicates.mapNotNull { formatCardPredicate(it, true) }
                if (subTerms.size != predicates.size) return null
                subTerms.joinToString("|")
            }

            // Name
            "NameEquals" -> "name:${element["name"]?.jsonPrimitive?.content}"

            else -> null // Can't express this predicate
        }
    }

    private fun formatStatePredicate(element: JsonElement): String? {
        if (element is JsonPrimitive && element.isString) {
            return STATE_PREDICATE_NAMES[element.content]
        }
        if (element is JsonObject) {
            val type = element["type"]?.jsonPrimitive?.content
            return STATE_PREDICATE_NAMES[type]
        }
        return null
    }

    private fun formatControllerPredicate(element: JsonElement): String? {
        if (element is JsonPrimitive && element.isString) {
            return CONTROLLER_PREDICATE_NAMES[element.content]
        }
        if (element is JsonObject) {
            val type = element["type"]?.jsonPrimitive?.content
            return CONTROLLER_PREDICATE_NAMES[type]
        }
        return null
    }

    /**
     * Parse a query string into a GameObjectFilter JSON object (fully expanded form).
     */
    fun parseFilter(query: String): JsonObject {
        val terms = tokenize(query)
        val cardPredicates = mutableListOf<JsonElement>()
        val statePredicates = mutableListOf<JsonElement>()
        var controllerPredicate: JsonElement? = null
        var matchAll = true

        for (term in terms) {
            when {
                // OR expression
                term.contains("|") -> {
                    val orPredicates = term.split("|").map { parseSingleCardPredicate(it) }
                    cardPredicates.add(buildJsonObject {
                        put("type", "Or")
                        put("predicates", JsonArray(orPredicates))
                    })
                    matchAll = false
                }

                // Controller/Owner predicate
                term in CONTROLLER_PREDICATES -> {
                    controllerPredicate = buildJsonObject { put("type", CONTROLLER_PREDICATES[term]!!) }
                }

                // State predicate
                term in STATE_PREDICATES -> {
                    statePredicates.add(buildJsonObject { put("type", STATE_PREDICATES[term]!!) })
                }

                // Comparison (mv>=6, pow<=3, tou=4)
                COMPARISON_REGEX.matches(term) -> {
                    cardPredicates.add(parseComparison(term))
                }

                // Keyword
                term.startsWith("kw:") -> {
                    cardPredicates.add(buildJsonObject {
                        put("type", "HasKeyword")
                        put("keyword", term.removePrefix("kw:").uppercase())
                    })
                }

                // Color
                term in COLORS -> {
                    cardPredicates.add(buildJsonObject {
                        put("type", "HasColor")
                        put("color", COLORS[term]!!)
                    })
                }
                term == "colorless" -> cardPredicates.add(buildJsonObject { put("type", "IsColorless") })
                term == "multicolored" -> cardPredicates.add(buildJsonObject { put("type", "IsMulticolored") })
                term == "monocolored" -> cardPredicates.add(buildJsonObject { put("type", "IsMonocolored") })

                // Name
                term.startsWith("name:") -> {
                    cardPredicates.add(buildJsonObject {
                        put("type", "NameEquals")
                        put("name", term.removePrefix("name:"))
                    })
                }

                // Type predicate (lowercase)
                term in TYPE_PREDICATES -> {
                    cardPredicates.add(buildJsonObject { put("type", TYPE_PREDICATES[term]!!) })
                }

                // Subtype (capitalized word)
                term[0].isUpperCase() -> {
                    cardPredicates.add(buildJsonObject {
                        put("type", "HasSubtype")
                        put("subtype", term)
                    })
                }

                else -> error("Unknown filter query term: $term")
            }
        }

        return buildJsonObject {
            if (cardPredicates.isNotEmpty()) put("cardPredicates", JsonArray(cardPredicates))
            if (statePredicates.isNotEmpty()) put("statePredicates", JsonArray(statePredicates))
            if (controllerPredicate != null) put("controllerPredicate", controllerPredicate!!)
            if (!matchAll) put("matchAll", false)
        }
    }

    private fun parseSingleCardPredicate(term: String): JsonElement {
        // Type predicate
        TYPE_PREDICATES[term]?.let { return buildJsonObject { put("type", it) } }

        // Comparison
        if (COMPARISON_REGEX.matches(term)) return parseComparison(term)

        // Keyword
        if (term.startsWith("kw:")) return buildJsonObject {
            put("type", "HasKeyword")
            put("keyword", term.removePrefix("kw:").uppercase())
        }

        // Color
        COLORS[term]?.let { return buildJsonObject { put("type", "HasColor"); put("color", it) } }
        if (term == "colorless") return buildJsonObject { put("type", "IsColorless") }
        if (term == "multicolored") return buildJsonObject { put("type", "IsMulticolored") }

        // Subtype (capitalized)
        if (term[0].isUpperCase()) return buildJsonObject {
            put("type", "HasSubtype")
            put("subtype", term)
        }

        error("Unknown filter term in OR expression: $term")
    }

    private fun parseComparison(term: String): JsonObject {
        val match = COMPARISON_REGEX.matchEntire(term)!!
        val (field, op, valueStr) = match.destructured
        val value = valueStr.toInt()

        return when (field) {
            "mv" -> when (op) {
                ">=" -> buildJsonObject { put("type", "ManaValueAtLeast"); put("min", value) }
                "<=" -> buildJsonObject { put("type", "ManaValueAtMost"); put("max", value) }
                "=" -> buildJsonObject { put("type", "ManaValueEquals"); put("value", value) }
                else -> error("Unknown operator: $op")
            }
            "pow" -> when (op) {
                ">=" -> buildJsonObject { put("type", "PowerAtLeast"); put("min", value) }
                "<=" -> buildJsonObject { put("type", "PowerAtMost"); put("max", value) }
                "=" -> buildJsonObject { put("type", "PowerEquals"); put("value", value) }
                else -> error("Unknown operator: $op")
            }
            "tou" -> when (op) {
                ">=" -> buildJsonObject { put("type", "ToughnessAtLeast"); put("min", value) }
                "<=" -> buildJsonObject { put("type", "ToughnessAtMost"); put("max", value) }
                "=" -> buildJsonObject { put("type", "ToughnessEquals"); put("value", value) }
                else -> error("Unknown operator: $op")
            }
            else -> error("Unknown comparison field: $field")
        }
    }

    private fun tokenize(query: String): List<String> = query.trim().split(Regex("""\s+"""))

    /**
     * Check if a JSON object looks like a GameObjectFilter
     * (has cardPredicates, statePredicates, controllerPredicate, or matchAll keys).
     */
    fun isGameObjectFilter(obj: JsonObject): Boolean {
        return obj.containsKey("cardPredicates")
            || obj.containsKey("statePredicates")
            || obj.containsKey("controllerPredicate")
            || obj.containsKey("matchAll")
    }
}
