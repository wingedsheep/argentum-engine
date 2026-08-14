package com.wingedsheep.gameserver.replay

import com.wingedsheep.gameserver.persistence.persistenceJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Encodes replay payloads for durable storage as compactly as possible: gzip, then base64.
 *
 * Both stored payloads are JSON and both are highly repetitive — the input stream repeats `"type"`
 * discriminators and entity-id strings, the archived presentation stream repeats whole card objects
 * frame after frame — so gzip typically shaves 80–95%. base64 keeps them portable TEXT columns
 * across databases (no `bytea` round-tripping in Spring Data JDBC).
 *
 * Decoding is deliberately tolerant: [persistenceJson] ignores unknown keys and every field added to
 * [CompactReplay] since v1 has a default, so a record written by a newer build stays readable by an
 * older one — the situation you are in for the minutes a rolling deploy takes.
 */
object ReplayCodec {

    fun encode(replay: CompactReplay): String =
        encodeText(persistenceJson.encodeToString(CompactReplay.serializer(), replay))

    fun decode(encoded: String): CompactReplay =
        persistenceJson.decodeFromString(CompactReplay.serializer(), migrateKickerFlag(decodeText(encoded)))

    /**
     * Rewrite the pre-Bargain `CastSpell.wasKicked: Boolean` into the `declaredCostSlot: ChoiceSlot?`
     * that replaced it. Bargain (CR 702.166) rides the same optional-additional-cost rail as kicker,
     * so the flag had to become *which* mechanic declared — and a rename is not something
     * [persistenceJson] can bridge on its own.
     *
     * This has to happen or old records decode wrong rather than fail: `ignoreUnknownKeys` silently
     * drops the legacy key and `declaredCostSlot` falls back to its `null` default, so every recorded
     * kicked cast re-simulates *unkicked* — a different game from the one that was played, which is
     * precisely the silent drift the checkpoint fingerprints exist to catch.
     *
     * Gated on a substring test so the untouched majority of replays skip the extra parse entirely.
     */
    private fun migrateKickerFlag(json: String): String {
        if (!json.contains("\"wasKicked\"")) return json
        val root = persistenceJson.parseToJsonElement(json).jsonObject
        val actions = root["actions"] as? JsonArray ?: return json
        val migrated = JsonArray(
            actions.map { action ->
                val obj = action as? JsonObject ?: return@map action
                val kicked = (obj["wasKicked"] as? JsonPrimitive)?.booleanOrNull ?: return@map action
                val rest = obj.filterKeys { it != "wasKicked" }
                JsonObject(
                    if (kicked) rest + ("declaredCostSlot" to JsonPrimitive("KICKED")) else rest
                )
            }
        )
        return persistenceJson.encodeToString(
            JsonElement.serializer(),
            JsonObject(root + ("actions" to migrated)),
        )
    }

    /** gzip + base64 an arbitrary JSON payload (used for the archived presentation stream). */
    fun encodeText(text: String): String {
        val gzipped = ByteArrayOutputStream().also { out ->
            GZIPOutputStream(out).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }.toByteArray()
        return Base64.getEncoder().encodeToString(gzipped)
    }

    fun decodeText(encoded: String): String =
        GZIPInputStream(ByteArrayInputStream(Base64.getDecoder().decode(encoded))).use {
            it.readBytes().toString(Charsets.UTF_8)
        }

    /**
     * The pinned card definitions, encoded for their own write-once column rather than folded into
     * [encode]'s blob — they are the largest part of a record and the only part that never changes,
     * so keeping them out of the per-flush payload is what stops a long game rewriting them a few
     * hundred times. Null for a record with no pins, so the column stays honestly empty.
     */
    fun encodePins(pins: List<String>): String? =
        if (pins.isEmpty()) null
        else encodeText(persistenceJson.encodeToString(ListSerializer(String.serializer()), pins))

    fun decodePins(encoded: String?): List<String> =
        encoded?.let { persistenceJson.decodeFromString(ListSerializer(String.serializer()), decodeText(it)) }
            ?: emptyList()
}
