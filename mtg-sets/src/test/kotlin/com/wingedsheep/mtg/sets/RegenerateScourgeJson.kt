package com.wingedsheep.mtg.sets

import com.wingedsheep.mtg.sets.definitions.scourge.ScourgeSet
import com.wingedsheep.sdk.serialization.CardExporter
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FunSpec
import java.nio.file.Path

/**
 * Utility to regenerate Scourge JSON test resource files from Kotlin definitions.
 *
 * Run manually when card definitions or serialization format changes:
 * ```
 * ./gradlew :mtg-sets:test --tests "com.wingedsheep.mtg.sets.RegenerateScourgeJson"
 * ```
 */
@Ignored
class RegenerateScourgeJson : FunSpec({
    test("regenerate scourge json files") {
        val outputDir = Path.of("src/test/resources/cards/scourge/")
        CardExporter.exportSet(ScourgeSet.allCards, outputDir)
        println("Regenerated ${ScourgeSet.allCards.size} Scourge card JSON files to $outputDir")
    }
})
