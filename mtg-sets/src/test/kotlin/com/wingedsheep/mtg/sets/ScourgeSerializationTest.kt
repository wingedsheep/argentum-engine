package com.wingedsheep.mtg.sets

import com.wingedsheep.mtg.sets.definitions.scourge.ScourgeSet
import com.wingedsheep.sdk.serialization.CardExporter
import com.wingedsheep.sdk.serialization.CardLoader
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies that Scourge cards survive a Kotlin → JSON → Kotlin round-trip.
 *
 * For each card:
 * 1. Export the Kotlin-defined card to JSON
 * 2. Load from JSON
 * 3. Re-export and verify the JSON is identical (ignoring ability IDs)
 *
 * Also verifies that the pre-exported JSON files in test resources
 * match the current Kotlin definitions.
 */
class ScourgeSerializationTest : FunSpec({

    val jsonDir = "/cards/scourge/"
    val jsonIndex = listOf(
        "ark-of-blight.json",
        "bladewing-the-risen.json",
        "break-asunder.json",
        "carrion-feeder.json",
        "fierce-empath.json",
        "carbonize.json",
        "daru-warchief.json",
        "goblin-warchief.json",
        "rush-of-knowledge.json",
        "siege-gang-commander.json",
        "sulfuric-vortex.json",
    )

    test("Kotlin cards round-trip through JSON export and import") {
        for (card in ScourgeSet.allCards) {
            val exported = CardExporter.exportToJson(card)
            val reimported = CardLoader.fromJson(exported)
            val reExported = CardExporter.exportToJson(reimported)
            stripAbilityIds(reExported) shouldBe stripAbilityIds(exported)
        }
    }

    test("pre-exported JSON files match current Kotlin definitions") {
        for (fileName in jsonIndex) {
            val resourceJson = ScourgeSerializationTest::class.java
                .getResource("$jsonDir$fileName")!!.readText()

            val reimported = CardLoader.fromJson(resourceJson)
            val reExported = CardExporter.exportToJson(reimported)
            stripAbilityIds(reExported) shouldBe stripAbilityIds(resourceJson)
        }
    }

    test("JSON-loaded cards produce same export as Kotlin-defined cards") {
        val jsonCards = CardLoader.loadSetFromClasspath(
            clazz = ScourgeSerializationTest::class.java,
            resourcePath = jsonDir,
            index = jsonIndex,
        ).associateBy { it.name }

        for (kotlinCard in ScourgeSet.allCards) {
            val jsonCard = jsonCards.getValue(kotlinCard.name)
            stripAbilityIds(CardExporter.exportToJson(jsonCard)) shouldBe
                stripAbilityIds(CardExporter.exportToJson(kotlinCard))
        }
    }
})

/** Remove ability IDs from JSON since they're generated fresh on each load. */
private fun stripAbilityIds(json: String): String =
    json.replace(Regex("""^\s*"id":\s*"ability_\d+",?\n""", RegexOption.MULTILINE), "")
