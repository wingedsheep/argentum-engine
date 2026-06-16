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

    // A pool deep enough to fill 8 standard boosters without exhausting any rarity.
    fun viablePool(prefix: String): List<CardDefinition> =
        (1..15).map { card("$prefix Common $it", Rarity.COMMON) } +
            (1..8).map { card("$prefix Uncommon $it", Rarity.UNCOMMON) } +
            (1..5).map { card("$prefix Rare $it", Rarity.RARE) }

    fun config(code: String, cards: List<CardDefinition>, sealedSupported: Boolean, incomplete: Boolean = false) =
        BoosterGenerator.SetConfig(
            setCode = code,
            setName = "Set $code",
            cards = cards,
            basicLands = emptyList(),
            sealedSupported = sealedSupported,
            incomplete = incomplete,
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

    test("falls back to all sets when none are fully implemented") {
        val gen = SealedDeckGenerator(
            BoosterGenerator(
                mapOf("PART" to config("PART", viablePool("PART"), sealedSupported = false))
            )
        )

        gen.randomSetCode() shouldBe "PART"
    }
})
