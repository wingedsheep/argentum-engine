package com.wingedsheep.engine.limited

import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Pins limited basic-land art: a sealed/draft deck gets exactly one printing of each basic — the
 * set's standard art — and it is the same printing the deck builder previewed.
 *
 * Previously the deck builder showed one variant while [BoosterGenerator] round-robined the deck's
 * copies across *all* of the set's variants, so a player who built with regular art ended up
 * holding full-art / borderless lands in game with no way to choose.
 */
class BoosterGeneratorBasicLandArtTest : DescribeSpec({

    fun land(type: String, cn: String, set: String = "BLB", draftable: Boolean = true): CardDefinition =
        basicLand(type) {
            collectorNumber = cn
            imageUri = "$set-$cn.jpg"
            inBooster = draftable
        }.copy(setCode = set)

    /** Regular booster arts 262-263, full-art treatments 369-370, mirroring Bloomburrow. */
    val regular262 = land("Plains", "262")
    val regular263 = land("Plains", "263")
    val fullArt369 = land("Plains", "369")
    val fullArt370 = land("Plains", "370")

    fun generatorFor(basics: List<CardDefinition>, setCode: String = "BLB") = BoosterGenerator(
        mapOf(
            setCode to BoosterGenerator.SetConfig(
                setCode = setCode,
                setName = "Test Set",
                cards = emptyList(),
                basicLands = basics,
            ),
        ),
    )

    describe("getBasicLands") {

        it("offers the standard art — the lowest-numbered variant — not a special treatment") {
            val basics = generatorFor(listOf(regular262, regular263, fullArt369, fullArt370)).getBasicLands("BLB")
            basics.keys shouldContainExactlyInAnyOrder setOf("Plains")
            basics["Plains"]!!.metadata.collectorNumber shouldBe "262"
        }

        it("picks the standard art regardless of the order the set lists its variants in") {
            // The set's basicLands come from reflective discovery, so the incoming order is not a
            // contract — the choice must come from the collector number itself.
            val shuffled = listOf(fullArt370, regular263, fullArt369, regular262)
            generatorFor(shuffled).getBasicLands("BLB")["Plains"]!!
                .metadata.collectorNumber shouldBe "262"
        }

        it("ignores variants excluded from the draft/sealed product even when they number lowest") {
            val basics = generatorFor(
                listOf(land("Plains", "100", draftable = false), regular262),
            ).getBasicLands("BLB")
            basics["Plains"]!!.metadata.collectorNumber shouldBe "262"
        }

        it("offers one printing per land type") {
            val basics = generatorFor(
                listOf(regular262, fullArt369, land("Forest", "280"), land("Forest", "375")),
            ).getBasicLands("BLB")
            basics.keys shouldContainExactlyInAnyOrder setOf("Plains", "Forest")
            basics["Forest"]!!.metadata.collectorNumber shouldBe "280"
        }

        it("falls through a multi-set pool to the first set that prints basics") {
            // A pool led by a set with no basics of its own must still hand out lands.
            val generator = BoosterGenerator(
                mapOf(
                    "DMU" to BoosterGenerator.SetConfig("DMU", "Dominaria United", emptyList(), emptyList()),
                    "POR" to BoosterGenerator.SetConfig("POR", "Portal", emptyList(), listOf(land("Plains", "196", "POR"))),
                ),
            )
            generator.getBasicLands(listOf("DMU", "POR"))["Plains"]!!
                .metadata.collectorNumber shouldBe "196"
        }

        it("returns no basics when no set in the pool prints any") {
            val generator = BoosterGenerator(
                mapOf("DMU" to BoosterGenerator.SetConfig("DMU", "Dominaria United", emptyList(), emptyList())),
            )
            generator.getBasicLands(listOf("DMU")) shouldBe emptyMap()
        }
    }

    describe("withBasicLandArt") {

        val basics = mapOf("Plains" to regular262, "Forest" to land("Forest", "280"))

        it("stamps every copy of a basic with the one printing deck building offered") {
            BoosterGenerator.withBasicLandArt(mapOf("Plains" to 8, "Forest" to 9), basics)
                .shouldContainExactly(mapOf("Plains#BLB-262" to 8, "Forest#BLB-280" to 9))
        }

        it("leaves spells untouched") {
            BoosterGenerator.withBasicLandArt(mapOf("Llanowar Elves" to 2, "Plains" to 1), basics)
                .shouldContainExactly(mapOf("Llanowar Elves" to 2, "Plains#BLB-262" to 1))
        }

        it("passes a basic through unchanged when the set prints none of that type") {
            // A pool whose set has no basics at all can't stamp art onto the deck's lands.
            BoosterGenerator.withBasicLandArt(mapOf("Plains" to 8), emptyMap())
                .shouldContainExactly(mapOf("Plains" to 8))
            BoosterGenerator.withBasicLandArt(mapOf("Island" to 8), basics)
                .shouldContainExactly(mapOf("Island" to 8))
        }

        it("stamps a colorless basic too — the land names come from the set, not a fixed list") {
            // Final Fantasy prints Wastes; the old hardcoded five-name gate silently skipped it.
            val wastes = mapOf("Wastes" to land("Wastes", "309", "FIN"))
            BoosterGenerator.withBasicLandArt(mapOf("Wastes" to 4), wastes)
                .shouldContainExactly(mapOf("Wastes#FIN-309" to 4))
        }

        it("degrades to the bare name when the printing has no collector number to address") {
            val unnumbered = mapOf("Plains" to basicLand("Plains") { imageUri = "x.jpg" }.copy(setCode = "BLB"))
            BoosterGenerator.withBasicLandArt(mapOf("Plains" to 8), unnumbered)
                .shouldContainExactly(mapOf("Plains" to 8))
        }

        it("degrades to the bare name when the printing has no set code to address") {
            val setless = mapOf("Plains" to regular262.copy(setCode = null))
            BoosterGenerator.withBasicLandArt(mapOf("Plains" to 8), setless)
                .shouldContainExactly(mapOf("Plains" to 8))
        }

        it("keeps every copy when the deck already names the printing it gets stamped with") {
            // Premade tournament lists may carry Name#SET-CN entries; merging (not replacing) keeps
            // the 4 plain Plains from being swallowed by the 4 already-stamped ones.
            BoosterGenerator.withBasicLandArt(mapOf("Plains" to 4, "Plains#BLB-262" to 4), basics)
                .shouldContainExactly(mapOf("Plains#BLB-262" to 8))
        }

        it("is a no-op on an empty deck list") {
            BoosterGenerator.withBasicLandArt(emptyMap(), basics) shouldBe emptyMap()
        }
    }

    describe("withCardArt") {
        it("pins a reprint to the printing opened in the Limited pool") {
            val fdnPacifism = regular262.copy(
                name = "Pacifism",
                setCode = "FDN",
                metadata = regular262.metadata.copy(collectorNumber = "501", imageUri = "fdn-501.jpg"),
            )

            BoosterGenerator.withCardArt(mapOf("Pacifism" to 2), listOf(fdnPacifism))
                .shouldContainExactly(mapOf("Pacifism#FDN-501" to 2))
        }
    }
})
