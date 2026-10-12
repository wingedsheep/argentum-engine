package com.wingedsheep.ai.engine

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Guards the random-set selection used by quick/AI games against picking a set whose pool is too
 * thin to open a booster.
 *
 * Regression: `randomSetCode()` used to draw from *every* available set, including partial ones
 * (incomplete or not sealed-curated) whose single-set booster generation throws
 * `No cards available for booster generation`. With ~25% of the catalog unviable, the empty-deck
 * CreateGame path failed intermittently — surfacing as a flaky `GameConnectionTest` timeout because
 * the server never replied with `GameCreated`. The fix restricts random selection to
 * [BoosterGenerator.SetConfig.fullyImplemented] sets.
 */
class SealedDeckGeneratorTest : FunSpec({

    fun card(name: String, rarity: Rarity, inBooster: Boolean = true) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = rarity, inBooster = inBooster),
    )

    // A pool deep enough to fill 8 standard boosters without exhausting any rarity. Small enough
    // (28 cards) to model a bonus sheet like "The Big Score" — below the standalone size threshold.
    fun viablePool(prefix: String): List<CardDefinition> =
        (1..15).map { card("$prefix Common $it", Rarity.COMMON) } +
            (1..8).map { card("$prefix Uncommon $it", Rarity.UNCOMMON) } +
            (1..5).map { card("$prefix Rare $it", Rarity.RARE) }

    // A full-size set (260 distinct cards) — above the standalone size threshold, so it is a valid
    // random quick/AI sealed source.
    fun largePool(prefix: String): List<CardDefinition> =
        (1..200).map { card("$prefix Common $it", Rarity.COMMON) } +
            (1..40).map { card("$prefix Uncommon $it", Rarity.UNCOMMON) } +
            (1..20).map { card("$prefix Rare $it", Rarity.RARE) }

    fun config(
        code: String,
        cards: List<CardDefinition>,
        sealedSupported: Boolean,
        incomplete: Boolean = false,
        extensionSet: Boolean = false,
    ) =
        BoosterGenerator.SetConfig(
            setCode = code,
            setName = "Set $code",
            cards = cards,
            basicLands = emptyList(),
            sealedSupported = sealedSupported,
            incomplete = incomplete,
            extensionSet = extensionSet,
        )

    test("randomSetCode only returns fully-implemented sets") {
        // One viable, curated set; several partial ones whose pools cannot open a booster
        // (the single card is flagged out of the booster pool).
        val gen = SealedDeckGenerator(
            BoosterGenerator(
                mapOf(
                    "FULL" to config("FULL", viablePool("FULL"), sealedSupported = true),
                    "PART1" to config("PART1", listOf(card("Token", Rarity.COMMON, inBooster = false)), sealedSupported = false),
                    "PART2" to config("PART2", listOf(card("Token", Rarity.COMMON, inBooster = false)), sealedSupported = false, incomplete = true),
                    "PART3" to config("PART3", viablePool("PART3"), sealedSupported = true, incomplete = true),
                )
            )
        )

        val drawn = (1..500).map { gen.randomSetCode() }.toSet()
        drawn.toList() shouldContainExactly listOf("FULL")
    }

    test("randomSetCode skips small complete sets in favour of full standalone sets") {
        // "BIG" is a complete, sealed-supported bonus sheet (28 cards) that can't fill 8 boosters on
        // its own; "FULL" is a full-size set. Only FULL is a valid standalone random source.
        val gen = SealedDeckGenerator(
            BoosterGenerator(
                mapOf(
                    "FULL" to config("FULL", largePool("FULL"), sealedSupported = true),
                    "BIG" to config("BIG", viablePool("BIG"), sealedSupported = true),
                )
            )
        )

        val drawn = (1..500).map { gen.randomSetCode() }.toSet()
        drawn.toList() shouldContainExactly listOf("FULL")
    }

    test("randomSetCode never lands on an extension set, whatever its size") {
        // An extension set (bonus sheet) is only playable alongside a regular set, so a random
        // roll — which yields a single set — must never pick one, even a hypothetical large one.
        val gen = SealedDeckGenerator(
            BoosterGenerator(
                mapOf(
                    "FULL" to config("FULL", largePool("FULL"), sealedSupported = true),
                    "EXT" to config("EXT", largePool("EXT"), sealedSupported = true, extensionSet = true),
                )
            )
        )

        val drawn = (1..500).map { gen.randomSetCode() }.toSet()
        drawn.toList() shouldContainExactly listOf("FULL")
    }

    test("falls back to an extension set only when nothing else exists at all") {
        // Pathological catalog with a single extension set: still return something rather than throw.
        val gen = SealedDeckGenerator(
            BoosterGenerator(
                mapOf("EXT" to config("EXT", viablePool("EXT"), sealedSupported = true, extensionSet = true))
            )
        )

        gen.randomSetCode() shouldBe "EXT"
    }

    test("falls back to any complete set when none meet the standalone size threshold") {
        // Only small complete sets exist — pick one rather than throw on an empty pool.
        val gen = SealedDeckGenerator(
            BoosterGenerator(
                mapOf("SMALL" to config("SMALL", viablePool("SMALL"), sealedSupported = true))
            )
        )

        gen.randomSetCode() shouldBe "SMALL"
    }

    test("generate(randomSetCode()) never throws even when partial sets are unviable") {
        val gen = SealedDeckGenerator(
            BoosterGenerator(
                mapOf(
                    "FULL" to config("FULL", viablePool("FULL"), sealedSupported = true),
                    "PART" to config("PART", listOf(card("Token", Rarity.COMMON, inBooster = false)), sealedSupported = false),
                )
            )
        )

        shouldNotThrowAny {
            repeat(200) {
                // The Draftsim autobuilder path (or its heuristic fallback) must always yield a
                // complete 40-card sealed deck. A synthetic set has no Draftsim ratings file, so this
                // also exercises the rarity-ladder fallback inside the builder.
                gen.generate(gen.randomSetCode()).values.sum() shouldBe 40
            }
        }
    }

    test("a generated deck's sideboard is the rest of its pool (CR 100.4b)") {
        val boosters = BoosterGenerator(mapOf("FULL" to config("FULL", viablePool("FULL"), sealedSupported = true)))
        val gen = SealedDeckGenerator(boosters)

        val generated = gen.generateDeck("FULL")

        // 8 boosters of a set with no basics: every opened card is in the deck or the sideboard.
        val opened = 8 * boosters.generateBooster("FULL").size
        val sideboardCards = generated.sideboard.values.sum()
        val deckNonBasics = generated.deckList.filterKeys { it.startsWith("FULL") }.values.sum()
        sideboardCards shouldBe opened - deckNonBasics
        generated.sideboard.keys.forEach { it.startsWith("FULL") shouldBe true }
    }

    test("falls back to all sets when none are fully implemented") {
        val gen = SealedDeckGenerator(
            BoosterGenerator(
                mapOf("PART" to config("PART", viablePool("PART"), sealedSupported = false))
            )
        )

        gen.randomSetCode() shouldBe "PART"
    }
})
