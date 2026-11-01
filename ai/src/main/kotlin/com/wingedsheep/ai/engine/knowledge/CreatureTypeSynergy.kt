package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.serialization.CardSerialization
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * The creature-type half of [SynergyAxes]: which creature types a card **is or makes** (it feeds
 * them) and which it **asks for** (it rewards them) — Goblin King asks for Goblins; every Goblin,
 * and every card that makes Goblin tokens, feeds it.
 *
 * "Asks for" is read off the card's structure, never its Oracle text: a creature type named by
 * any object filter anywhere in its abilities — the "other Goblins" a lord pumps, the "another Elf
 * you control" a trigger watches, the "Goblin spells" a cost reducer discounts, the "Goblin" a
 * sacrifice cost or a tutor wants. A creature-type predicate under a `Not` ("non-Goblin") doesn't
 * count, nor does one in a filter scoped to an opponent's objects ("Merfolk an opponent controls"),
 * nor a static that shrinks the type ("all Goblins get -1/-1"), which [SynergyStrength.punished] reads.
 *
 * The filters live anywhere in the ability tree (statics, triggers, costs, effects, targets), so
 * this walks the card's serialized script — the same canonical form `CardValidator` and the card
 * snapshots read — rather than enumerating every SDK type that can hold a filter. A hoser that names
 * a type with no controller ("destroy all Goblins") would read as asking for it; such cards are
 * rare enough that the miss is cheaper than a hand-kept list of effect types.
 */
internal object CreatureTypeSynergy {

    /** Creature types [card] asks for, read from the filters in its abilities. */
    fun rewarded(card: CardDefinition): Set<Subtype> {
        val names = mutableSetOf<String>()
        for (script in scripts(card)) collect(CardSerialization.json.encodeToJsonElement(CardScript.serializer(), script), names)
        return names.filter { it in CREATURE_TYPES }.mapTo(mutableSetOf(), ::Subtype)
    }

    /** Creature types [card] is (its type line, creature or kindred) or makes as tokens. */
    fun fed(card: CardDefinition): Set<Subtype> = buildSet {
        card.typeLine.subtypes.filterTo(this) { it.value in CREATURE_TYPES }
        for (script in scripts(card)) {
            val effects = listOfNotNull(script.spellEffect) +
                script.triggeredAbilities.map { it.effect } +
                script.activatedAbilities.filterNot { it.isManaAbility }.map { it.effect }
            for (effect in effects) for (leaf in EffectWalker.leaves(effect)) {
                if (leaf is CreateTokenEffect) leaf.creatureTypes.filter { it in CREATURE_TYPES }.mapTo(this, ::Subtype)
            }
        }
    }

    /** A changeling is every creature type, so it feeds every tribe. */
    fun everyType(card: CardDefinition): Boolean = Keyword.CHANGELING in card.keywords

    private fun scripts(card: CardDefinition): List<CardScript> = listOf(card.script) + card.cardFaces.map { it.script }

    private fun collect(element: JsonElement, out: MutableSet<String>) {
        when (element) {
            is JsonArray -> element.forEach { collect(it, out) }
            is JsonObject -> {
                val type = element.string("type")
                if (type == "Not") return
                // "All Goblins get -1/-1" names Goblins to shrink them: it punishes the tribe
                // ([SynergyStrength.punished]), it doesn't ask for it.
                if (type == "ModifyStats" && (element.int("powerBonus") ?: 0) + (element.int("toughnessBonus") ?: 0) < 0) return
                if ((element["controllerPredicate"] as? JsonObject)?.string("type") in OPPONENT_SCOPES) return
                when (type) {
                    "HasSubtype" -> element.string("subtype")?.let(out::add)
                    "HasAnyOfSubtypes" -> (element["subtypes"] as? JsonArray)
                        ?.mapNotNullTo(out) { (it as? JsonPrimitive)?.contentOrNull }
                }
                element.values.forEach { collect(it, out) }
            }
            else -> Unit
        }
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(key: String): Int? = string(key)?.toIntOrNull()

    private val CREATURE_TYPES: Set<String> = Subtype.ALL_CREATURE_TYPES.toSet()

    private val OPPONENT_SCOPES = setOf(
        "ControlledByOpponent", "OwnedByOpponent", "ControlledByDefendingPlayer", "ControlledByTargetOpponent",
    )
}
