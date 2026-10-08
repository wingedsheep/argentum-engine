package com.wingedsheep.ai.draftsim

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

private data class BCard(
    override val name: String,
    override val manaCost: String,
    override val typeLine: String,
    override val colors: List<String> = emptyList(),
    override val colorIdentity: List<String> = emptyList(),
    override val cmc: Double = DraftsimMana.cmc(manaCost),
    override val rarity: String? = "common",
    override val priceUsd: Double? = null,
) : ScorerCard

/**
 * Stage 5: deck construction (`vX`/`Tm`/`ek`/`dW`/`SX`). Drives a realistic two-color-heavy pool
 * through the orchestrator and asserts the structural invariants: a 40-card deck (23 nonland + 17
 * lands), copy caps honored, the creature floor met, and full determinism (same pool → same build).
 */
class DraftsimDeckBuilderTest : FunSpec({

    // A ~90-card RG-heavy pool with creatures, removal, fixing lands, and off-color filler.
    fun buildPool(): Pair<List<DraftsimPoolCard>, DraftsimSetTables> {
        val ratings = HashMap<String, Double>()
        val removal = HashSet<String>()
        val cards = mutableListOf<DraftsimPoolCard>()
        var n = 0
        fun add(card: ScorerCard, rating: Double, isRemoval: Boolean = false) {
            ratings[DraftsimData.nameKey(card.name)] = rating
            if (isRemoval) removal += card.name.lowercase()
            cards += DraftsimPoolCard(card, "id-${n++}")
        }

        val costs = listOf("{R}", "{1}{R}", "{2}{R}", "{3}{R}", "{1}{G}", "{2}{G}", "{3}{G}", "{4}{G}")
        for (i in 1..16) add(BCard("RedCreature$i", costs[i % 4], "Creature — Goblin", listOf("R")), 2.6)
        for (i in 1..16) add(BCard("GreenCreature$i", costs[4 + i % 4], "Creature — Beast", listOf("G")), 2.7)
        for (i in 1..6) add(BCard("Zap$i", "{1}{R}", "Instant", listOf("R")), 3.0, isRemoval = true)
        for (i in 1..3) add(BCard("RGLand$i", "", "Land", colorIdentity = listOf("R", "G")), 2.0)
        // Off-color filler (won't be chosen for an RG build).
        for (i in 1..20) add(BCard("BlueFiller$i", "{2}{U}", "Creature — Drake", listOf("U")), 1.8)

        return cards to DraftsimSetTables(ratings, removal, emptyMap())
    }

    test("dW produces a 40-card build (23 nonland + 17 lands) honoring caps and the creature floor") {
        val (pool, tables) = buildPool()
        val builder = DraftsimDeckBuilder(tables)
        val byId = pool.associateBy { it.instanceId }

        val builds = builder.buildDecks(pool, mode = "sealed")
        builds.isNotEmpty() shouldBe true
        val best = builds.first()

        // 23 nonland + 17 lands = 40 (pool lands kept are in deckInstanceIds; basics are separate).
        (best.deckInstanceIds.size + best.basicsNeeded.values.sum()) shouldBe 40

        val chosen = best.deckInstanceIds.mapNotNull { byId[it]?.card }
        val nonland = chosen.filter { !it.typeLine.lowercase().contains("land") }
        nonland.size shouldBe 23

        // Copy cap: ≤4 of any (non-legendary) name.
        nonland.groupingBy { it.name }.eachCount().values.all { it <= 4 } shouldBe true
        // Creature floor.
        nonland.count { it.typeLine.lowercase().contains("creature") } shouldBeGreaterThanOrEqual 13
        // Score is a real 0–10 value.
        best.score shouldBeGreaterThanOrEqual 0.0
    }

    test("builds are fully deterministic") {
        val (pool, tables) = buildPool()
        val builder = DraftsimDeckBuilder(tables)
        val a = builder.buildDecks(pool, mode = "draft")
        val b = builder.buildDecks(pool, mode = "draft")
        a.map { it.deckInstanceIds } shouldBe b.map { it.deckInstanceIds }
        a.map { it.score } shouldBe b.map { it.score }
    }

    test("the chosen deck avoids the off-color filler") {
        val (pool, tables) = buildPool()
        val builder = DraftsimDeckBuilder(tables)
        val byId = pool.associateBy { it.instanceId }
        val best = builder.buildDecks(pool, mode = "draft").first()
        val chosenNames = best.deckInstanceIds.mapNotNull { byId[it]?.card?.name }
        // An RG build should not be reaching for {2}{U} filler.
        chosenNames.count { it.startsWith("BlueFiller") } shouldBe 0
    }

    test("completion keeping locked removal still builds a full 23/17 deck (no double-add)") {
        val (pool, tables) = buildPool()
        val byId = pool.associateBy { it.instanceId }
        val builder = DraftsimDeckBuilder(tables)

        // Lock three removal spells — these are seeded before the removal phase, which must not re-add them.
        val forced = pool.filter { it.card.name in setOf("Zap1", "Zap2", "Zap3") }.map { it.instanceId }.toSet()
        val build = builder.buildDecks(pool, mode = "sealed", forced = forced).single()

        val nonland = build.deckInstanceIds.mapNotNull { byId[it]?.card }
            .filter { !it.typeLine.lowercase().contains("land") }
        nonland.size shouldBe 23
        (build.deckInstanceIds.size + build.basicsNeeded.values.sum()) shouldBe 40
        // Every locked removal spell is present exactly once.
        listOf("Zap1", "Zap2", "Zap3").forEach { name -> nonland.count { it.name == name } shouldBe 1 }
    }

    test("completion does not add off-color basics for a flexible hybrid card") {
        val ratings = HashMap<String, Double>()
        val cards = mutableListOf<DraftsimPoolCard>()
        var n = 0
        fun add(card: ScorerCard, rating: Double) {
            ratings[DraftsimData.nameKey(card.name)] = rating
            cards += DraftsimPoolCard(card, "id-${n++}")
        }
        // A pure GU pool — plus a {2/B} monocolor-hybrid card that is castable with generic mana
        // (no black ever needed). Locking it must NOT pull black into the build / manabase.
        for (i in 1..14) add(BCard("GreenCreature$i", "{1}{G}", "Creature — Beast", listOf("G")), 2.6)
        for (i in 1..14) add(BCard("BlueCreature$i", "{1}{U}", "Creature — Wizard", listOf("U")), 2.6)
        add(BCard("Flexible", "{2/B}", "Creature — Horror", listOf("B")), 3.0)
        val tables = DraftsimSetTables(ratings, emptySet(), emptyMap())
        val builder = DraftsimDeckBuilder(tables)

        val forced = cards.filter { it.card.name in setOf("GreenCreature1", "BlueCreature1", "Flexible") }
            .map { it.instanceId }.toSet()
        val build = builder.buildDecks(cards, mode = "sealed", forced = forced).single()

        build.colors.toSet() shouldBe setOf("G", "U")
        (build.basicsNeeded["B"] ?: 0) shouldBe 0
    }

    test("completion fixes mana for every locked color, even a third") {
        val ratings = HashMap<String, Double>()
        val cards = mutableListOf<DraftsimPoolCard>()
        var n = 0
        fun add(card: ScorerCard, rating: Double) {
            ratings[DraftsimData.nameKey(card.name)] = rating
            cards += DraftsimPoolCard(card, "id-${n++}")
        }
        // A WU-heavy pool with a small black slice to lock as the off-axis third color.
        for (i in 1..14) add(BCard("WhiteCreature$i", "{1}{W}", "Creature — Soldier", listOf("W")), 2.6)
        for (i in 1..14) add(BCard("BlueCreature$i", "{1}{U}", "Creature — Wizard", listOf("U")), 2.6)
        for (i in 1..3) add(BCard("BlackBomb$i", "{1}{B}", "Creature — Demon", listOf("B")), 3.5)
        val tables = DraftsimSetTables(ratings, emptySet(), emptyMap())
        val builder = DraftsimDeckBuilder(tables)

        // Lock one card of each colour, including the third (black).
        val forced = cards.filter { it.card.name in setOf("WhiteCreature1", "BlueCreature1", "BlackBomb1") }
            .map { it.instanceId }.toSet()
        val build = builder.buildDecks(cards, mode = "sealed", forced = forced).single()

        // All three locked colours are build colours and the manabase carries basics for each.
        build.colors shouldContainAll listOf("W", "U", "B")
        listOf("W", "U", "B").forEach { c -> (build.basicsNeeded[c] ?: 0) shouldBeGreaterThan 0 }
    }
    // A thin WU pile in a BG-heavy pool used to be padded with the pool's best off-colour cards: the
    // creature floor swapped in creatures of any colour, and a castability-relaxed fill topped the
    // deck up to 23. A WU deck holding a {3}{B}{B}{G} rare is unplayable, so neither may happen —
    // the shortfall goes to lands instead.
    test("a pool too thin in the build's colours is filled with lands, never off-colour cards") {
        val ratings = HashMap<String, Double>()
        val cards = mutableListOf<DraftsimPoolCard>()
        var n = 0
        fun add(card: ScorerCard, rating: Double) {
            ratings[DraftsimData.nameKey(card.name)] = rating
            cards += DraftsimPoolCard(card, "id-${n++}")
        }
        for (i in 1..6) add(BCard("WhiteSpell$i", "{1}{W}", "Instant", listOf("W")), 2.5)
        for (i in 1..6) add(BCard("BlueSpell$i", "{2}{U}", "Sorcery", listOf("U")), 2.5)
        for (i in 1..4) add(BCard("WhiteBear$i", "{1}{W}", "Creature — Human", listOf("W")), 2.5)
        add(BCard("Aatchik, Emerald Radian", "{3}{B}{B}{G}", "Legendary Creature — Insect Druid", listOf("B", "G")), 4.5)
        for (i in 1..20) add(BCard("BlackBeast$i", "{2}{B}", "Creature — Beast", listOf("B")), 3.0)
        for (i in 1..20) add(BCard("GreenBeast$i", "{2}{G}", "Creature — Beast", listOf("G")), 3.0)
        val builder = DraftsimDeckBuilder(DraftsimSetTables(ratings, HashSet(), emptyMap()))
        val byId = cards.associateBy { it.instanceId }
        val forced = setOf("WhiteSpell1", "BlueSpell1").map { name -> cards.first { it.card.name == name }.instanceId }.toSet()

        val build = builder.buildDecks(cards, mode = "sealed", forced = forced).single()

        build.colors.toSet() shouldBe setOf("W", "U")
        val nonland = build.deckInstanceIds.mapNotNull { byId[it]?.card }.filter { !it.typeLine.contains("Land") }
        nonland.flatMap { it.colors }.toSet() shouldBe setOf("W", "U")
        nonland.size shouldBe 16
        (build.deckInstanceIds.size + build.basicsNeeded.values.sum()) shouldBe 40
        build.basicsNeeded.keys shouldBe setOf("W", "U")
    }
    // The archetype ranking rates a colour pair on the quality of what it could cast and never looks
    // at its shape, so a pair that is all removal and no bodies used to win. The builds are now
    // ordered by what they actually hold, so the creature-rich pair is recommended instead.
    test("prefers the colours that can meet the creature floor over a creature-starved removal pile") {
        val ratings = HashMap<String, Double>()
        val removal = HashSet<String>()
        val cards = mutableListOf<DraftsimPoolCard>()
        var n = 0
        fun add(card: ScorerCard, rating: Double, isRemoval: Boolean = false) {
            ratings[DraftsimData.nameKey(card.name)] = rating
            if (isRemoval) removal += card.name.lowercase()
            cards += DraftsimPoolCard(card, "id-${n++}")
        }
        val rbCosts = listOf("{1}{R}", "{2}{R}", "{1}{B}", "{2}{B}")
        for (i in 1..16) add(BCard("Bolt$i", rbCosts[i % 4], "Instant", listOf(if (i % 4 < 2) "R" else "B")), 3.2, isRemoval = true)
        for (i in 1..8) add(BCard("RedOgre$i", "{2}{R}", "Creature — Ogre", listOf("R")), 2.4)
        val wgCosts = listOf("{1}{W}", "{2}{W}", "{1}{G}", "{2}{G}", "{3}{G}")
        for (i in 1..26) add(BCard("Bear$i", wgCosts[i % 5], "Creature — Bear", listOf(if (i % 5 < 2) "W" else "G")), 2.6)
        val byId = cards.associateBy { it.instanceId }

        val best = DraftsimDeckBuilder(DraftsimSetTables(ratings, removal, emptyMap())).buildDecks(cards, mode = "sealed").first()

        val nonland = best.deckInstanceIds.mapNotNull { byId[it]?.card }.filter { !it.typeLine.contains("Land") }
        nonland.count { it.typeLine.contains("Creature") } shouldBeGreaterThanOrEqual 13
    }
})
