package com.wingedsheep.tooling.coverage

import com.wingedsheep.tooling.coverage.bridge.Bridge
import com.wingedsheep.tooling.coverage.bridge.MappingEntry
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pins the typed Kotlin [Bridge] against the legacy hand-authored `spike/mtgish-coverage/mapping.json`
 * entry-for-entry. During the keep-both phase this guarantees the new modular bridge is a faithful,
 * complete translation of the JSON the Python tooling still reads — so the two stay in lockstep and
 * the Kotlin/Python output parity holds. (When the Python spike is finally deleted, so is this test.)
 */
class BridgeParityTest : StringSpec({
    "Kotlin Bridge matches legacy mapping.json entry-for-entry" {
        // When the Python spike is finally deleted, mapping.json goes with it and this guard is moot.
        val mismatches = mutableListOf<String>()
        if (MAPPING_JSON.exists()) {
            val legacy = J.parseToJsonElement(MAPPING_JSON.readText()).jsonObject
                .filterKeys { it != "__doc__" }
            val bridge = Bridge.all

            bridge.keys.toSet() shouldBe legacy.keys.toSet()

            for ((key, raw) in legacy) {
            val entry = raw.jsonObject
            val got = bridge[key] ?: error("missing bridge entry: $key")
            val legacyKind = entry["kind"]!!.jsonPrimitive.content
            val legacyTags = entry["tags"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet() ?: emptySet()
            val legacyNote = entry["note"]?.jsonPrimitive?.content
            val legacyTag = entry["tag"]?.jsonPrimitive?.content

            if (got.kind != legacyKind) mismatches += "$key: kind ${got.kind} != $legacyKind"
            if (got is MappingEntry.Effect || got is MappingEntry.Keyword) {
                if (got.tag != legacyTag) mismatches += "$key: tag ${got.tag} != $legacyTag"
            }
            if (got.composes.toSet() != legacyTags) mismatches += "$key: composes ${got.composes} != $legacyTags"
            if (got.note != legacyNote) mismatches += "$key: note '${got.note}' != '$legacyNote'"
            }
        }
        mismatches shouldBe emptyList()
    }
})
