package com.wingedsheep.mtg.sets

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File

/**
 * Field-level verification of every *registered* The Lord of the Rings: Tales of Middle-earth
 * card against authoritative Scryfall data.
 *
 * Mirrors [com.wingedsheep.mtg.sets.ArnCardFieldVerificationTest] (which checks Arabian Nights),
 * but here we additionally verify **power / toughness / loyalty** at the request of the field
 * audit. The test checks the **actual compiled [CardDefinition]s** the engine loads — the real
 * source of truth for play — against the committed Scryfall dump, field by field:
 *
 *   name, mana_cost, color_identity, type_line, oracle_text, power, toughness, loyalty,
 *   rarity, collector_number, artist, flavor_text, image_uris
 *
 * Each of our cards is matched to a Scryfall printing by collector number (with a Scryfall-id
 * fallback derived from the image URI). A field is a problem when it differs from Scryfall and
 * is not legitimately empty on both sides.
 */
class LtrCardFieldVerificationTest : FunSpec({

    test("LTR: every registered card matches authoritative Scryfall on all requested fields") {
        val dump = Json.parseToJsonElement(ltrDumpFile().readText()).jsonObject
        val scryfall = dump["data"]!!.jsonArray.map { it.jsonObject }
        val byCollector = scryfall.associateBy { it.str("collector_number") }
        val byId = scryfall.associateBy { it.str("id") }

        val cards = MtgSetCatalog.requireByCode("LTR").cards.sortedBy { it.name }
        val problems = mutableListOf<String>()

        for (card in cards) {
            val cn = card.metadata.collectorNumber
            val a = byCollector[cn] ?: byId[card.scryfallId()]
            if (a == null) {
                problems += "${card.name} (cn=$cn): no authoritative Scryfall match"
                continue
            }

            check(problems, card, "name", card.name, a.str("name"))
            check(problems, card, "mana_cost", card.manaCost.toString(), a.str("mana_cost") ?: "")
            check(problems, card, "color_identity", card.colorIdentityString(), a.colorIdentityString())
            check(problems, card, "type_line", card.typeLine.toString(), a.str("type_line"))
            check(problems, card, "oracle_text", card.oracleText, a.str("oracle_text") ?: "")
            check(problems, card, "power", card.creatureStats?.power?.description, a.str("power"))
            check(problems, card, "toughness", card.creatureStats?.toughness?.description, a.str("toughness"))
            check(problems, card, "loyalty", card.startingLoyalty?.toString(), a.str("loyalty"))
            check(problems, card, "rarity", card.metadata.rarity.scryfall(), a.str("rarity"))
            check(problems, card, "collector_number", cn ?: "", a.str("collector_number"))
            check(problems, card, "artist", card.metadata.artist, a.str("artist"))
            check(problems, card, "flavor_text", card.metadata.flavorText, a.str("flavor_text"))
            // Compare image URIs without Scryfall's volatile `?<timestamp>` cache-buster — the
            // image identity is the path (…/<scryfall-id>.jpg); the query bumps on every re-host.
            check(problems, card, "image_uris", card.metadata.imageUri?.substringBefore("?"), a.imageNormal()?.substringBefore("?"))
        }

        if (problems.isNotEmpty()) {
            println("LTR field verification: ${problems.size} discrepancy(ies) across ${cards.size} cards")
            problems.forEach { println("  - $it") }
        } else {
            println("LTR field verification: all ${cards.size} registered cards match Scryfall on every requested field.")
        }
        problems shouldBe emptyList()
    }
})

/** A field matches when the two normalized values are equal, treating null/blank as the empty string. */
private fun check(problems: MutableList<String>, card: CardDefinition, field: String, ours: String?, auth: String?) {
    val o = ours?.trim().orEmpty()
    val a = auth?.trim().orEmpty()
    if (o != a) {
        problems += "${card.name}.$field: ours=${o.q()} auth=${a.q()}"
    }
}

private fun String.q(): String = "\"" + replace("\n", "\\n") + "\""

private fun Rarity.scryfall(): String = name.lowercase()

/** Our color identity rendered in Scryfall's canonical WUBRG order, e.g. {W}{U} -> "W,U". */
private fun CardDefinition.colorIdentityString(): String =
    colorIdentity.sortedBy { Color.entries.indexOf(it) }.joinToString(",") { it.symbol.toString() }

private fun JsonObject.colorIdentityString(): String {
    val arr = this["color_identity"] as? JsonArray ?: return ""
    return arr.map { (it as JsonPrimitive).content }
        .sortedBy { sym -> Color.entries.indexOfFirst { it.symbol == sym.firstOrNull() } }
        .joinToString(",")
}

/** Scryfall id parsed out of the normal image URI (…/front/0/e/<id>.jpg?…). */
private fun CardDefinition.scryfallId(): String? {
    val uri = metadata.imageUri ?: return null
    return Regex("""/([0-9a-f-]{36})\.""").find(uri)?.groupValues?.get(1)
}

private fun JsonObject.imageNormal(): String? =
    (this["image_uris"] as? JsonObject)?.get("normal")?.let { (it as JsonPrimitive).content }

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { !it.isString || it.content.isNotEmpty() }?.content

/** Walk up from the working directory to find the committed Scryfall dump. */
private fun ltrDumpFile(): File {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
        val f = File(dir, "backlog/sets/lord-of-the-rings/ltr_set.json")
        if (f.exists()) return f
        dir = dir.parentFile
    }
    error("Could not locate backlog/sets/lord-of-the-rings/ltr_set.json from ${System.getProperty("user.dir")}")
}
