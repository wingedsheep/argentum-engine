package com.wingedsheep.ai.draftsim

import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json

/**
 * Stage 1 of the Draftsim port: name-key normalization, the data loaders (ratings/removal/
 * archetypes), and the card adapter. The load-bearing risk is the **join** — our card names must
 * normalize to the same keys the vendored tables use — so this drives our real card DB through the
 * loaders and asserts a high resolve rate, then pins the split/DFC normalization hazards.
 */
class DraftsimDataTest : FunSpec({

    // ----- nameKey (gt) -----

    test("nameKey: substring before //, strips diacritics, _→space, trims, lowercases") {
        DraftsimData.nameKey("The_Torment_of_Gollum") shouldBe "the torment of gollum"
        DraftsimData.nameKey("Lim-Dûl's Vault") shouldBe "lim-dul's vault"
        DraftsimData.nameKey("  Séance  ") shouldBe "seance"
        DraftsimData.nameKey("Fire // Ice") shouldBe "fire"
        DraftsimData.nameKey("Wear // Tear") shouldBe "wear"
    }

    // ----- loaders -----

    test("LTR tables load with ratings and removal populated") {
        val ltr = DraftsimData.tablesFor(listOf("LTR"))

        ltr.ratings.size shouldBeGreaterThan 200
        ltr.removal.size shouldBeGreaterThan 10
        // Known LTR cards resolve a rating via their name key.
        ltr.ratings[DraftsimData.nameKey("The Torment of Gollum")] shouldNotBe null
        ltr.ratings[DraftsimData.nameKey("Saruman the White")] shouldNotBe null
        // Removal membership uses a plain lowercase, not nameKey.
        ltr.removal.contains("banish from edoras") shouldBe true
        // LTR ships no archetype columns ⇒ empty arch map (scorer falls back to the aX path).
        ltr.archetypes.isEmpty() shouldBe true
    }

    test("a tagged set (TMT) loads archetype records") {
        val tmt = DraftsimData.tablesFor(listOf("TMT"))
        tmt.archetypes.isEmpty() shouldBe false
        // Every record's tags carry a role.
        tmt.archetypes.values.first().archetypes.first().role.isNotBlank() shouldBe true
        // The Splashable column is read, and a row carrying nothing *but* that flag still lands —
        // both were silently dropped while the importer ignored the column.
        tmt.archetypes.values.count { it.splashable } shouldBeGreaterThan 0
        tmt.archetypes[DraftsimData.nameKey("The Last Ronin")] shouldNotBe null
    }

    test("MSH loads ratings, removal and archetypes") {
        val msh = DraftsimData.tablesFor(listOf("MSH"))

        msh.ratings.size shouldBeGreaterThan 300
        msh.ratings[DraftsimData.nameKey("Agent 13, Sharon Carter")] shouldNotBe null
        msh.removal.contains("beast within") shouldBe true
        // MSH ships archetype columns, so the scorer takes the jm path rather than color-bias.
        msh.archetypes.isEmpty() shouldBe false
        val teamwork = msh.archetypes.values.flatMap { it.archetypes }.map { it.archetype }
        teamwork.contains("Teamwork") shouldBe true
        // MSH marks Splashable with "X" where TMT/SOS write "yes" — the importer reads both.
        msh.archetypes[DraftsimData.nameKey("Thanos, the Mad Titan")]?.splashable shouldBe true
    }

    test("a set we have no file for yields empty tables (rarity-fallback path)") {
        val none = DraftsimData.tablesFor(listOf("ZZZ"))
        none.ratings.isEmpty() shouldBe true
        none.removal.isEmpty() shouldBe true
    }

    test("multi-set pool unions removal and is order-independent") {
        val a = DraftsimData.tablesFor(listOf("LTR", "BLB"))
        val b = DraftsimData.tablesFor(listOf("BLB", "LTR"))
        a.removal shouldBe b.removal
        val ltr = DraftsimData.tablesFor(listOf("LTR"))
        a.removal shouldContainAll ltr.removal
    }

    // ----- the manifest -----

    // `ratedSetCodes()` is what the whole-catalog consumers (LimitedCardRater, ConstructedRatings)
    // load, so a code listed there with no usable table silently claims coverage it doesn't have —
    // AFR sat in the manifest with an empty `{}` file for exactly that reason.
    test("every manifest code has a non-empty ratings table") {
        val empty = DraftsimData.ratedSetCodes().filter { DraftsimData.tablesFor(listOf(it)).ratings.isEmpty() }
        withClue("manifest codes with no ratings: $empty") { empty.isEmpty() shouldBe true }
    }

    // The loader re-keys through `nameKey` defensively, so this reads the resources themselves:
    // a table whose keys aren't already normalized still joins, but it hides a typo'd card name.
    test("every ratings resource is already keyed by nameKey") {
        val json = Json { ignoreUnknownKeys = true }
        val offenders = DraftsimData.ratedSetCodes().associateWith { code ->
            val text = DraftsimData::class.java.getResourceAsStream("/draftai/ratings/$code.json")
                ?.bufferedReader()?.use { it.readText() } ?: ""
            json.decodeFromString<Map<String, Double>>(text).keys
                .filter { it != DraftsimData.nameKey(it) }
        }.filterValues { it.isNotEmpty() }
        withClue("unnormalized keys: $offenders") { offenders.isEmpty() shouldBe true }
    }

    // ----- the join against our real card DB -----

    // Every table covers a whole set's booster pool, so our implemented subset should land almost
    // entirely in it. A low rate would mean the normalization is misaligned. The first three are
    // vendored Draftsim tables; the rest are the first-party tables written for the pre-Draftsim
    // sets (see `DraftsimData`'s class doc), where the join is against names we authored ourselves
    // and so should be near-total.
    listOf("LTR", "MSH", "HOB", "ATQ", "SCG", "LGN", "POR", "DOM", "MRD", "ONS", "INV").forEach { code ->
        test("most $code cards in our registry resolve a Draftsim rating (name join works)") {
            val set = MtgSetCatalog.all.first { it.code == code }
            val ratings = DraftsimData.tablesFor(listOf(code)).ratings

            val spells = set.cards.filterNot { it.typeLine.isBasicLand }
            val resolved = spells.count { ratings.containsKey(DraftsimData.nameKey(it.name)) }

            withClue("resolved $resolved / ${spells.size} $code spells") {
                (resolved.toDouble() / spells.size) shouldBeGreaterThan 0.9
            }
        }
    }

    // ----- the adapter -----

    test("CardDefinition adapter exposes the scorer fields") {
        val ltrSet = MtgSetCatalog.all.first { it.code == "LTR" }
        val creature = ltrSet.cards.first { it.typeLine.isCreature && it.manaCost.symbols.isNotEmpty() }
        val card = creature.toScorerCard()

        card.name shouldBe creature.name
        card.cmc shouldBe creature.cmc.toDouble()
        card.typeLine.lowercase().contains("creature") shouldBe true
        card.rarity shouldBe creature.metadata.rarity.name.lowercase()
        // Colors are populated for a colored creature.
        card.colors.isNotEmpty() shouldBe true
    }

    test("CardSummary adapter derives cmc from the cost; colors left for the scorer") {
        val summary = CardSummary(
            name = "Shock", manaCost = "{R}", typeLine = "Instant", rarity = "COMMON", oracleText = "Deal 2.",
        )
        val card = summary.toScorerCard()
        card.cmc shouldBe 1.0
        card.rarity shouldBe "common"
        // CardSummary carries no color list — the scorer derives it from the cost.
        card.colors.isEmpty() shouldBe true
        DraftsimMana.colorsInCost(card.manaCost) shouldBe listOf("R")
    }
})
