package com.wingedsheep.ai.engine.deck

import com.wingedsheep.ai.engine.producedColors
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import com.wingedsheep.sdk.serialization.CardSerialization
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * What a card does for a limited deck's colours, read off its structure: the colours it can turn
 * toward a coloured cost ([colors]), and whether it gets better the more colours the deck plays
 * ([rewardsColors]). The deckbuilder reads both to decide how many colours a pool should be built
 * in — the archetype tables carry a curated `fixing` list for a handful of sets, and this is what
 * every other set falls back to.
 *
 * Fixing:
 *  - a land: the colours it taps for; one that fetches a basic land (Evolving Wilds) fixes whichever
 *    basics the deck plays, so all five;
 *  - a nonland: a mana ability that makes two or more colours (Paradise Druid, Prophetic Prism), a
 *    search for a basic land (Rampant Growth), or Treasure. A mono-coloured dork (Llanowar Elves) is
 *    ramp, not fixing, and reads as nothing.
 *
 * The searches and Treasure are found in the serialized script — the canonical form the card
 * snapshots read — because they sit at any depth inside a composite, a trigger or a pipeline.
 */
object ManaFixing {

    private val ALL = Color.entries.toSet()

    /** The colours [card] can supply beyond its own basics; empty for a card that fixes nothing. */
    fun colors(card: CardDefinition): Set<Color> {
        if (card.typeLine.isBasicLand) return emptySet()
        val json by lazy { scripts(card).map { CardSerialization.json.encodeToJsonElement(CardScript.serializer(), it) } }
        if (card.typeLine.isLand) {
            basicSearch(json)?.let { return it }
            val manaAbilities = scripts(card).flatMap { it.activatedAbilities }.filter { it.isManaAbility }
            // No mana ability of its own: a land that sacrifices for a named basic (Riveteers
            // Overlook's Swamp, Mountain or Forest), read the way the heuristic builder reads it.
            if (manaAbilities.isEmpty()) return producedColors(card)
            val produced = card.typeLine.subtypes.mapNotNullTo(mutableSetOf()) { BASIC_TYPES[it.value] }
            manaAbilities.forEach { collectColors(it.effect, produced) }
            return produced
        }
        basicSearch(json)?.let { found -> return if (found.size >= 2) found else emptySet() }
        if (scripts(card).any(::makesOwnTreasure)) return ALL
        val produced = mutableSetOf<Color>()
        for (script in scripts(card)) for (ability in script.activatedAbilities) {
            if (ability.isManaAbility) collectColors(ability.effect, produced)
        }
        return if (produced.size >= 2) produced else emptySet()
    }

    /**
     * Whether [card] is a land that fixes by finding a basic — Evolving Wilds, Riveteers Overlook — and
     * so is only as good as the basics the deck plays for it to find.
     */
    fun fetchesBasic(card: CardDefinition): Boolean {
        if (!card.typeLine.isLand || card.typeLine.isBasicLand) return false
        if (basicSearch(scripts(card).map { CardSerialization.json.encodeToJsonElement(CardScript.serializer(), it) }) != null) return true
        return scripts(card).none { s -> s.activatedAbilities.any { it.isManaAbility } } && producedColors(card).isNotEmpty()
    }

    /**
     * Whether [card] scales with the number of colours its controller has: Vivid ("for each color
     * among permanents you control"), domain (basic land types among your lands) and converge
     * (colours of mana spent to cast it).
     */
    fun rewardsColors(card: CardDefinition): Boolean {
        if (Keyword.VIVID in card.keywords) return true
        return scripts(card).any { countsColors(CardSerialization.json.encodeToJsonElement(CardScript.serializer(), it)) }
    }

    private fun scripts(card: CardDefinition): List<CardScript> = listOf(card.script) + card.cardFaces.map { it.script }

    /**
     * The colours a mana ability makes for any spell. Mana restricted to some spells (a Bloomburrow
     * village's "spend this only to cast a creature spell of the chosen type") isn't counted.
     */
    private fun collectColors(effect: Effect, out: MutableSet<Color>) {
        when (effect) {
            is AddManaEffect -> if (effect.restriction == null) out.add(effect.color)
            is AddManaOfChoiceEffect -> if (effect.restriction == null) choiceColors(effect.colorSet, out)
            is AddDynamicManaEffect -> if (effect.restriction == null) out.addAll(effect.allowedColors)
            is CompositeEffect -> effect.effects.forEach { collectColors(it, out) }
            else -> Unit
        }
    }

    /**
     * Colours a "one mana of …" choice reaches every game. "The chosen color" is one colour fixed as
     * the permanent enters — Uncharted Haven is a basic of your choice, not a five-colour land — and
     * the state-dependent sets (colours among your permanents, a linked exiled card) promise nothing
     * a deck can be built on.
     */
    private fun choiceColors(colorSet: ManaColorSet, out: MutableSet<Color>) {
        when (colorSet) {
            is ManaColorSet.AnyColor, is ManaColorSet.CommanderIdentity -> out.addAll(ALL)
            is ManaColorSet.Specific -> out.addAll(colorSet.colors)
            is ManaColorSet.Union -> colorSet.members.forEach { choiceColors(it, out) }
            is ManaColorSet.ExceptChosenColor -> choiceColors(colorSet.base, out)
            else -> Unit
        }
    }

    private val BASIC_TYPES = mapOf(
        "Plains" to Color.WHITE, "Island" to Color.BLUE, "Swamp" to Color.BLACK, "Mountain" to Color.RED, "Forest" to Color.GREEN,
    )

    /**
     * The colours a basic-land search in [scripts] can find, or null when there is none: every colour
     * for "a basic land card" (Evolving Wilds, Rampant Growth), just the named ones for "a basic
     * Swamp, Mountain or Forest card" (Riveteers Overlook). A search is a `GatherCards` from the
     * library whose filter asks for a basic land.
     */
    private fun basicSearch(scripts: List<JsonElement>): Set<Color>? {
        var found: MutableSet<Color>? = null
        fun walk(element: JsonElement) {
            when (element) {
                is JsonArray -> element.forEach(::walk)
                is JsonObject -> {
                    val fromLibrary = element.string("type") == "GatherCards" &&
                        (element["source"] as? JsonObject)?.string("zone") == "Library"
                    if (fromLibrary && mentions(element, "IsBasicLand")) {
                        val named = mutableSetOf<String>().also { collectSubtypes(element, it) }.mapNotNull { BASIC_TYPES[it] }
                        (found ?: mutableSetOf<Color>().also { found = it }).addAll(named.ifEmpty { ALL })
                    }
                    element.values.forEach(::walk)
                }
                else -> Unit
            }
        }
        scripts.forEach(::walk)
        return found
    }

    private fun collectSubtypes(element: JsonElement, out: MutableSet<String>) {
        when (element) {
            is JsonArray -> element.forEach { collectSubtypes(it, out) }
            is JsonObject -> {
                when (element.string("type")) {
                    "HasSubtype" -> element.string("subtype")?.let(out::add)
                    "HasAnyOfSubtypes" -> (element["subtypes"] as? JsonArray)?.mapNotNullTo(out) { (it as? JsonPrimitive)?.contentOrNull }
                }
                element.values.forEach { collectSubtypes(it, out) }
            }
            else -> Unit
        }
    }

    /**
     * Treasure the card's controller can count on: made by the spell itself, an activated ability, or
     * a trigger on something entering the battlefield. A Treasure an opponent gets (Buy Your Silence)
     * or one dealt out by a combat-damage trigger (Professional Face-Breaker) is not fixing to build on.
     */
    private fun makesOwnTreasure(script: CardScript): Boolean {
        fun json(effect: Effect) = CardSerialization.json.encodeToJsonElement(Effect.serializer(), effect)
        val effects = listOfNotNull(script.spellEffect) +
            script.activatedAbilities.filterNot { it.isManaAbility }.map { it.effect } +
            script.triggeredAbilities.filter { ability ->
                "\"Battlefield\"" in CardSerialization.json.encodeToJsonElement(EventPattern.serializer(), ability.trigger).toString()
            }.map { it.effect }
        return effects.any { makesTreasure(json(it)) }
    }

    private fun makesTreasure(element: JsonElement): Boolean = when (element) {
        is JsonArray -> element.any(::makesTreasure)
        is JsonObject -> (element.string("type") == "CreatePredefinedToken" && element.string("tokenType") == "Treasure" &&
            element["controller"] == null) || element.values.any(::makesTreasure)
        else -> false
    }

    private fun countsColors(element: JsonElement): Boolean = when (element) {
        is JsonArray -> element.any(::countsColors)
        is JsonObject -> element.string("type") in COLOR_COUNT_TYPES ||
            element.string("aggregation") in COLOR_COUNT_AGGREGATIONS ||
            element.values.any(::countsColors)
        else -> false
    }

    private fun mentions(element: JsonElement, type: String): Boolean = when (element) {
        is JsonArray -> element.any { mentions(it, type) }
        is JsonObject -> element.string("type") == type || element.values.any { mentions(it, type) }
        else -> false
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    /** Converge's amount and Vivid's cost reduction. */
    private val COLOR_COUNT_TYPES = setOf("DistinctColorsManaSpent", "ColorsAmongPermanentsYouControl")

    /** Vivid's "colors among permanents you control" and domain's "basic land types among lands you control". */
    private val COLOR_COUNT_AGGREGATIONS = setOf("DISTINCT_COLORS", "DISTINCT_BASIC_LAND_SUBTYPES")
}
