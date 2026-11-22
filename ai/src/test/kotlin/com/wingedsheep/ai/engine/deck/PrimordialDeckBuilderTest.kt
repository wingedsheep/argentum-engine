package com.wingedsheep.ai.engine.deck

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.random.Random

/**
 * The AI's Primordial deck: 40 cards from one block, exactly 2 rares (one of each) and 6 uncommons
 * (two of each at most), commons as four-ofs at most.
 */
class PrimordialDeckBuilderTest : FunSpec({

    fun card(name: String, cost: String, rarity: Rarity) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(cost),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = rarity),
    )

    fun colourCost(i: Int) = "{${i % 4}}{${if (i % 2 == 0) "G" else "R"}}"

    // Zendikar block spread over two sets; a reprint row in WWK is a common there.
    val zen = (1..30).map { card("Zen Common $it", colourCost(it), Rarity.COMMON) } +
        (1..8).map { card("Zen Uncommon $it", colourCost(it), Rarity.UNCOMMON) } +
        (1..6).map { card("Zen Rare $it", colourCost(it), Rarity.RARE) }
    val wwk = (1..4).map { card("Wwk Uncommon $it", colourCost(it), Rarity.UNCOMMON) }
    val reprint = card("Old Reprint", "{1}{G}", Rarity.RARE)
    val basics = listOf(
        CardDefinition.basicLand("Forest", Subtype.FOREST, ScryfallMetadata(collectorNumber = "300")),
        CardDefinition.basicLand("Mountain", Subtype.MOUNTAIN, ScryfallMetadata(collectorNumber = "301")),
    )
    val registry = CardRegistry().apply { (zen + wwk + reprint + basics).forEach { register(it) } }
    val configs = mapOf(
        "ZEN" to BoosterGenerator.SetConfig("ZEN", "Zendikar", zen, basics),
        "WWK" to BoosterGenerator.SetConfig(
            "WWK", "Worldwake", wwk, basics,
            printings = listOf(Printing("old", "Old Reprint", "WWK", "99", rarity = Rarity.COMMON)),
        ),
        // Not a Primordial set: nothing may come from here.
        "CMM" to BoosterGenerator.SetConfig("CMM", "Commander Masters", (1..40).map { card("Cmm $it", "{G}", Rarity.COMMON) }, basics),
    )

    test("builds a legal 40-card deck from one block") {
        repeat(5) { seed ->
            val deck = ConstructedDeckGenerator(BoosterGenerator(configs), registry, Random(seed))
                .generateDeck(emptyList(), DeckFormat.PRIMORDIAL).deckList
            val byName = (zen + wwk).associateBy { it.name } + (reprint.name to reprint.copy(metadata = reprint.metadata.copy(rarity = Rarity.COMMON)))
            withClue(deck) {
                deck.values.sum() shouldBe 40
                val spells = deck.filterKeys { it.substringBefore('#') !in setOf("Forest", "Mountain") }
                spells.keys.all { it in byName } shouldBe true
                val rarity = spells.mapValues { (name, _) -> byName.getValue(name).metadata.rarity }
                spells.filterKeys { rarity[it] == Rarity.RARE }.values.sum() shouldBe 2
                spells.filterKeys { rarity[it] == Rarity.UNCOMMON }.values.sum() shouldBe 6
                spells.forEach { (name, n) ->
                    val cap = when (rarity[name]) { Rarity.RARE -> 1; Rarity.UNCOMMON -> 2; else -> 4 }
                    n shouldBeLessThanOrEqual cap
                }
            }
        }
    }

    test("refuses when no Primordial pool has enough cards") {
        shouldThrow<IllegalArgumentException> {
            ConstructedDeckGenerator(BoosterGenerator(configs), registry, Random(1))
                .generateDeck(listOf("CMM"), DeckFormat.PRIMORDIAL)
        }
    }
})
