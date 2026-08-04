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
 * Field-level verification of every *registered* Marvel's Spider-Man (SPM) card against
 * authoritative Scryfall data.
 *
 * Mirrors [com.wingedsheep.mtg.sets.LtrCardFieldVerificationTest], and like it verifies
 * **power / toughness / loyalty** in addition to the printed metadata. The test checks the
 * **actual compiled [CardDefinition]s** the engine loads — the real source of truth for play —
 * against the committed Scryfall dump, field by field:
 *
 *   name, mana_cost, color_identity, type_line, oracle_text, power, toughness, loyalty,
 *   rarity, collector_number, artist, flavor_text, image_uris
 *
 * SPM contains double-faced cards (modal_dfc). Those are checked **face by face**: our front
 * face against Scryfall `card_faces[0]` and our [CardDefinition.backFace] against `card_faces[1]`
 * for the per-face fields (name / mana_cost / type_line / oracle_text / power / toughness /
 * loyalty / artist / flavor_text / image_uris). color_identity, rarity and collector_number are
 * whole-card and compared against the top-level object.
 *
 * Each of our cards is matched to a Scryfall printing by collector number (with a Scryfall-id
 * fallback derived from the image URI). A field is a problem when it differs from Scryfall and
 * is not legitimately empty on both sides.
 */
class SpmCardFieldVerificationTest : FunSpec({

    test("SPM: every registered card matches authoritative Scryfall on all requested fields") {
        val dump = Json.parseToJsonElement(spmDumpFile().readText()).jsonObject
        val scryfall = dump["data"]!!.jsonArray.map { it.jsonObject }
        val byCollector = scryfall.associateBy { it.str("collector_number") }
        val byId = scryfall.associateBy { it.str("id") }

        val cards = MtgSetCatalog.requireByCode("SPM").cards.sortedBy { it.name }
        val problems = mutableListOf<String>()

        for (card in cards) {
            val cn = card.metadata.collectorNumber
            val a = byCollector[cn] ?: byId[card.scryfallId()]
            if (a == null) {
                problems += "${card.name} (cn=$cn): no authoritative Scryfall match"
                continue
            }

            // Whole-card fields (never per-face on Scryfall).
            check(problems, card.name, "color_identity", card.colorIdentityString(), a.colorIdentityString())
            check(problems, card.name, "rarity", card.metadata.rarity.scryfall(), a.str("rarity"))
            check(problems, card.name, "collector_number", cn ?: "", a.str("collector_number"))

            // Per-face fields: pair our face definitions with Scryfall's card_faces (or the
            // whole object for a single-faced card).
            val ourFaces = if (card.isDoubleFaced) listOf(card, card.backFace!!) else listOf(card)
            val authFaces = a.faces()
            if (ourFaces.size != authFaces.size) {
                problems += "${card.name}: face-count mismatch ours=${ourFaces.size} auth=${authFaces.size}"
                continue
            }
            for ((idx, pair) in ourFaces.zip(authFaces).withIndex()) {
                val (ourFace, authFace) = pair
                val label = if (card.isDoubleFaced) "${card.name}[$idx:${ourFace.name}]" else card.name
                // The back face of a transforming DFC is a transformed face reached only via the
                // flip: per CR 711 it has no mana cost (its colors come from a color indicator),
                // even though Scryfall lists a printed mana_cost for it. Don't flag that.
                val transformedBack = card.isDoubleFaced && idx == 1 && ourFace.colorIndicator != null
                checkFace(problems, label, ourFace, authFace, skipManaCost = transformedBack)
            }
        }

        if (problems.isNotEmpty()) {
            println("SPM field verification: ${problems.size} discrepancy(ies) across ${cards.size} cards")
            problems.forEach { println("  - $it") }
        } else {
            println("SPM field verification: all ${cards.size} registered cards match Scryfall on every requested field.")
        }
        problems shouldBe emptyList()
    }
})

/** Compare all per-face fields of one of our faces against its authoritative Scryfall face object. */
private fun checkFace(problems: MutableList<String>, label: String, def: CardDefinition, a: JsonObject, skipManaCost: Boolean = false) {
    check(problems, label, "name", def.name, a.str("name"))
    if (!skipManaCost) check(problems, label, "mana_cost", def.manaCost.toString(), a.str("mana_cost") ?: "")
    check(problems, label, "type_line", def.typeLine.toString(), a.str("type_line"))
    check(problems, label, "oracle_text", def.oracleText, a.str("oracle_text") ?: "")
    check(problems, label, "power", def.creatureStats?.power?.description, a.str("power"))
    check(problems, label, "toughness", def.creatureStats?.toughness?.description, a.str("toughness"))
    check(problems, label, "loyalty", def.startingLoyalty?.toString(), a.str("loyalty"))
    check(problems, label, "artist", def.metadata.artist, a.str("artist"))
    check(problems, label, "flavor_text", def.metadata.flavorText, a.str("flavor_text"))
    // Compare image URIs without Scryfall's volatile `?<timestamp>` cache-buster — the image
    // identity is the path (…/<scryfall-id>.jpg); the query bumps on every re-host.
    check(problems, label, "image_uris", def.metadata.imageUri?.substringBefore("?"), a.imageNormal()?.substringBefore("?"))
}

/** A field matches when the two normalized values are equal, treating null/blank as the empty string. */
private fun check(problems: MutableList<String>, label: String, field: String, ours: String?, auth: String?) {
    val o = ours?.trim().orEmpty()
    val a = auth?.trim().orEmpty()
    if (o != a) {
        problems += "$label.$field: ours=${o.q()} auth=${a.q()}"
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

/** Scryfall's per-face objects, or the whole object when the card is single-faced. */
private fun JsonObject.faces(): List<JsonObject> {
    val arr = this["card_faces"] as? JsonArray
    if (arr != null && arr.size >= 2) return arr.map { it.jsonObject }
    return listOf(this)
}

private fun JsonObject.imageNormal(): String? =
    (this["image_uris"] as? JsonObject)?.get("normal")?.let { (it as JsonPrimitive).content }

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { !it.isString || it.content.isNotEmpty() }?.content

/** Walk up from the working directory to find the committed Scryfall dump. */
private fun spmDumpFile(): File {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
        val f = File(dir, "backlog/archived/sets/marvels-spider-man/spm_set.json")
        if (f.exists()) return f
        dir = dir.parentFile
    }
    error("Could not locate backlog/archived/sets/marvels-spider-man/spm_set.json from ${System.getProperty("user.dir")}")
}
