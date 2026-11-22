package com.wingedsheep.gameserver.deck

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardEntry
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Primordial deck construction: one block or standalone set, exactly 2 rares/mythics and 6
 * uncommons, rarity copy caps across deck and sideboard, a 10-card sideboard.
 */
class DeckValidatorPrimordialTest : FunSpec({

    fun creature(name: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 2,
        toughness = 2,
    )

    val registry = CardRegistry()
    val printings = PrintingRegistry()
    var collector = 0
    fun print(name: String, setCode: String, rarity: Rarity) {
        if (registry.getCard(name) == null) registry.register(creature(name))
        printings.register(Printing(oracleId = name, name = name, setCode = setCode, collectorNumber = "${++collector}", rarity = rarity))
    }

    // Zendikar block: commons in ZEN, uncommons in WWK, rares in ROE.
    (1..10).forEach { print("Zen Common $it", "ZEN", Rarity.COMMON) }
    (1..6).forEach { print("Zen Uncommon $it", "WWK", Rarity.UNCOMMON) }
    (1..3).forEach { print("Zen Rare $it", "ROE", Rarity.RARE) }
    print("Zen Mythic", "ROE", Rarity.MYTHIC)
    // Printed uncommon in ZEN and common in ROE: a common in the block.
    print("Zen Shifter", "ZEN", Rarity.UNCOMMON)
    print("Zen Shifter", "ROE", Rarity.COMMON)
    print("Bloom Common", "BLB", Rarity.COMMON)
    print("Commander Only", "CMM", Rarity.COMMON)
    print("Skullclamp", "DST", Rarity.UNCOMMON)
    // A core-set reprint: common in M10, uncommon in M11.
    print("Core Reprint", "M10", Rarity.COMMON)
    print("Core Reprint", "M11", Rarity.UNCOMMON)
    registry.register(
        CardDefinition.basicLand("Forest", Subtype("Forest")),
    )

    val validator = DeckValidator(registry, printings)

    /** A legal 40-card Zendikar deck: 2 rares, 6 uncommons, 4×4 commons, 16 Forests. */
    fun legalDeck(): MutableMap<String, Int> = linkedMapOf<String, Int>().apply {
        put("Zen Rare 1", 1)
        put("Zen Mythic", 1)
        (1..3).forEach { put("Zen Uncommon $it", 2) }
        (1..4).forEach { put("Zen Common $it", 4) }
        put("Forest", 16)
    }

    fun codes(deck: Map<String, Int>, sideboard: List<String> = emptyList()) =
        validator.validate(
            Deck(cards = deck.flatMap { (n, c) -> List(c) { n } }, sideboard = sideboard.map { CardEntry(it) }),
            DeckFormat.PRIMORDIAL,
        )

    test("a one-block deck with 2 rares and 6 uncommons is legal and reports its pool") {
        val result = codes(legalDeck())
        result.errors.shouldBeEmpty()
        val summary = result.primordial.shouldNotBeNull()
        summary.pool shouldBe "Zendikar block"
        summary.setCodes shouldBe listOf("ZEN", "WWK", "ROE")
        summary.rares shouldBe 2
        summary.uncommons shouldBe 6
        summary.slotByCard["Zen Mythic"] shouldBe "RARE"
    }

    test("the rare and uncommon totals are exact") {
        val deck = legalDeck().apply { remove("Zen Mythic"); put("Zen Uncommon 4", 2); put("Forest", 15) }
        val errors = codes(deck).errors.map { it.code }
        errors shouldContain "PRIMORDIAL_RARE_COUNT"
        errors shouldContain "PRIMORDIAL_UNCOMMON_COUNT"
    }

    test("copy caps follow rarity: 1 rare, 2 uncommon, 4 common") {
        val deck = legalDeck().apply { put("Zen Common 1", 5); put("Forest", 15) }
        val result = codes(deck)
        result.errors.single().code shouldBe "TOO_MANY_COPIES"
        result.errors.single().message shouldContain "allows 4 of a common"
    }

    test("copy caps count the sideboard; the 2/6 totals don't") {
        val result = codes(legalDeck(), sideboard = listOf("Zen Rare 1", "Zen Rare 2", "Zen Uncommon 4"))
        result.errors.map { it.cardName to it.code } shouldBe listOf("Zen Rare 1" to "TOO_MANY_COPIES")
        result.primordial!!.rares shouldBe 2
    }

    test("a card's rarity is its lowest printing in the block") {
        val deck = legalDeck().apply { put("Zen Shifter", 4); put("Forest", 12) }
        val result = codes(deck)
        result.errors.shouldBeEmpty()
        result.primordial!!.slotByCard["Zen Shifter"] shouldBe "COMMON"
    }

    test("a card from another set names the deck's pool and where it is from") {
        val deck = legalDeck().apply { put("Bloom Common", 1); put("Forest", 15) }
        val issue = codes(deck).errors.single()
        issue.code shouldBe "PRIMORDIAL_OUTSIDE_POOL"
        issue.cardName shouldBe "Bloom Common"
        issue.message shouldContain "Zendikar block"
        issue.message shouldContain "Bloomburrow"
    }

    test("cards from no legal set, and banned cards, are not legal") {
        val deck = legalDeck().apply { put("Commander Only", 1); put("Skullclamp", 1); put("Forest", 14) }
        val issues = codes(deck).errors.filter { it.code == "NOT_LEGAL_IN_FORMAT" }
        issues.map { it.cardName }.shouldContainExactlyInAnyOrder("Commander Only", "Skullclamp")
        issues.first { it.cardName == "Skullclamp" }.message shouldContain "banned"
    }

    test("the sideboard holds at most 10 cards") {
        val sideboard = (5..10).flatMap { listOf("Zen Common $it", "Zen Common $it") }
        codes(legalDeck(), sideboard).errors.map { it.code } shouldContain "SIDEBOARD_TOO_LARGE"
    }

    test("40 is the minimum deck size") {
        val deck = legalDeck().apply { put("Forest", 15) }
        codes(deck).errors.map { it.code } shouldContain "TOO_FEW_CARDS"
    }

    test("an all-reprint deck picks the pool its rarities fit and lists the others") {
        // Four copies: legal as an M10 common, over the 2-copy cap as an M11 uncommon.
        val deck = mapOf("Core Reprint" to 4, "Forest" to 36)
        val summary = codes(deck).primordial.shouldNotBeNull()
        summary.pool shouldBe "Magic 2010"
        summary.alternatives shouldBe listOf("Magic 2011")
        summary.candidateSetCodes shouldContainExactlyInAnyOrder listOf("M10", "M11")
    }

    test("a deck of only basics has no pool yet") {
        val result = codes(mapOf("Forest" to 40))
        result.primordial!!.pool.shouldBeNull()
        result.errors.map { it.code } shouldNotContain "PRIMORDIAL_OUTSIDE_POOL"
    }
})
