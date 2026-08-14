package com.wingedsheep.engine.limited

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * Pins that a meld result never reaches a player's card pool.
 *
 * A meld result (Chittering Host, Brisela, …) is the *back halves* of two meld cards combined, not
 * a card of its own: it can only be created by melding the pair on the battlefield. Scryfall still
 * reports it `booster: true` — the physical card is in the pack, as the meld parts — so the
 * product-level `metadata.inBooster` flag can't exclude it and [CardDefinition.meldResult] must.
 *
 * The bug this locks down: a drafted Chittering Host went into a deck and was drawn as a permanent
 * its controller could never cast.
 */
class BoosterGeneratorMeldResultTest : DescribeSpec({

    fun common(name: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString()),
    )

    // 13 ordinary commons plus the meld result. A pack draws 12 distinct commons out of 14, so an
    // unfiltered pool would surface the meld result in nearly every pack — a sharp control.
    val meldResultName = "Chittering Host"
    fun poolOf(setCode: String) =
        (1..13).map { common("$setCode Common $it") } + common(meldResultName).copy(meldResult = true)

    fun generator(vararg setCodes: String) = BoosterGenerator(
        setCodes.associateWith { code ->
            BoosterGenerator.SetConfig(
                setCode = code,
                setName = "Set $code",
                cards = poolOf(code),
                basicLands = emptyList(),
            )
        }
    )

    describe("single-set booster") {
        it("never opens a meld result") {
            val gen = generator("AAA")
            repeat(60) {
                gen.generateBooster("AAA").map { it.name } shouldNotContain meldResultName
            }
        }

        it("opens the same card when it isn't a meld result (control)") {
            val gen = BoosterGenerator(
                mapOf(
                    "AAA" to BoosterGenerator.SetConfig(
                        setCode = "AAA",
                        setName = "Set AAA",
                        cards = (1..13).map { common("AAA Common $it") } + common(meldResultName),
                        basicLands = emptyList(),
                    )
                )
            )
            val names = (1..60).flatMap { gen.generateBooster("AAA").map { c -> c.name } }
            names shouldContain meldResultName
        }
    }

    describe("sealed pool") {
        it("excludes a meld result from a single-set pool") {
            generator("AAA").generateSealedPool("AAA", boosterCount = 6)
                .map { it.name } shouldNotContain meldResultName
        }

        it("excludes a meld result from a seeded multi-set pool") {
            generator("AAA", "BBB")
                .generateSealedPool(listOf("AAA", "BBB"), boosterCount = 6, distributionSeed = 42L)
                .map { it.name } shouldNotContain meldResultName
        }

        it("excludes a meld result from an explicit-distribution pool") {
            generator("AAA", "BBB").generateSealedPool(mapOf("AAA" to 3, "BBB" to 3))
                .map { it.name } shouldNotContain meldResultName
        }

        it("excludes a meld result from a chaos pool") {
            generator("AAA", "BBB")
                .generateSealedPool(listOf("AAA", "BBB"), boosterCount = 6, chaos = true)
                .map { it.name } shouldNotContain meldResultName
        }
    }
})
