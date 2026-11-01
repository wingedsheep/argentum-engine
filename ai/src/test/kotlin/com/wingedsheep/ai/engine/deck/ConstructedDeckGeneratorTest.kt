package com.wingedsheep.ai.engine.deck

import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.DraftsimDeckShape
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card as cardDef
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.assertions.withClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.core.spec.style.FunSpec
import kotlin.random.Random

/**
 * The constructed build behind a quick lobby's Random / Auto seats.
 *
 * The bug this closes: a quick lobby's deck-format restriction only ever applied to a *submitted*
 * deck. A Pauper lobby validated your list down to commons and then sat you across from a sealed
 * pool full of rares. These tests pin the two properties that fix it — the deck is built to the
 * constructed shape, and every card in it is legal in the requested format — plus the explicit
 * refusal to fake a Commander deck, and that the build actually runs through the Draftsim
 * autobuilder rather than the unrated random filler it fell back to before.
 */
class ConstructedDeckGeneratorTest : FunSpec({

    val shape = DraftsimDeckShape.CONSTRUCTED
    val deckSize = shape.nonlandCount + shape.landCount

    /** Draftsim's copy cap for a non-legendary card, which is constructed's four-of rule. */
    val maxCopies = 4

    /** Any set Draftsim ships ratings for; the rating-preference test borrows its card names. */
    val ratedSet = "BLB"

    fun card(
        name: String,
        cost: String,
        rarity: Rarity,
        formats: Set<DeckFormat>,
    ): CardDefinition = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(cost),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = rarity),
    ).copy(legalFormats = formats)

    /**
     * A mono-green pool spread across the curve, with a rarity split that matters: commons are
     * Pauper-legal, rares are not. Everything is Modern-legal, so "Modern" means "the whole pool".
     */
    fun pool(prefix: String): List<CardDefinition> =
        (1..12).flatMap { i ->
            val cmc = (i % 5) + 1
            listOf(
                card("$prefix Common $i", "{${cmc - 1}}{G}", Rarity.COMMON, setOf(DeckFormat.PAUPER, DeckFormat.MODERN)),
                card("$prefix Rare $i", "{${cmc - 1}}{G}", Rarity.RARE, setOf(DeckFormat.MODERN)),
            )
        }

    fun basics(): List<CardDefinition> = listOf(
        CardDefinition.basicLand("Forest", Subtype.FOREST, ScryfallMetadata(collectorNumber = "300")),
    )

    /**
     * Seeded so a run is reproducible: the generator's default [Random.Default] made the shortlist
     * draw — and so the build — differ every run. Over the flat-rated 24-card [pool] about one draw
     * in twenty stacks no card to four copies, which failed the four-ofs test at random. Successive
     * builds from one generator still differ, as the seeded stream advances.
     */
    fun generatorOver(vararg sets: Pair<String, List<CardDefinition>>): ConstructedDeckGenerator {
        val configs = sets.associate { (code, cards) ->
            code to BoosterGenerator.SetConfig(
                setCode = code,
                setName = "Set $code",
                cards = cards,
                basicLands = basics(),
            )
        }
        val registry = CardRegistry()
        sets.forEach { (_, cards) -> registry.register(cards) }
        registry.register(basics())
        return ConstructedDeckGenerator(BoosterGenerator(configs), registry, Random(SEED))
    }

    test("builds a 60-card deck from the chosen sets") {
        val gen = generatorOver("AAA" to pool("AAA"))

        val deck = gen.generate(listOf("AAA"), DeckFormat.MODERN)

        deck.values.sum() shouldBe deckSize
    }

    test("only uses cards legal in the requested format") {
        // The whole point: a Pauper build must not reach for the rares sharing the set.
        val gen = generatorOver("AAA" to pool("AAA"))

        val deck = gen.generate(listOf("AAA"), DeckFormat.PAUPER)

        val nonBasics = deck.keys.filterNot { it.startsWith("Forest") }
        nonBasics.shouldNotBeEmpty()
        nonBasics.forEach { name ->
            withClue("$name is not Pauper-legal but was put in a Pauper deck") {
                name.contains("Common") shouldBe true
            }
        }
    }

    test("scopes the pool to the chosen sets") {
        val gen = generatorOver("AAA" to pool("AAA"), "BBB" to pool("BBB"))

        val deck = gen.generate(listOf("BBB"), DeckFormat.MODERN)

        deck.keys.filterNot { it.startsWith("Forest") }.forEach { it.startsWith("BBB") shouldBe true }
    }

    test("an empty set list draws on the whole registered card base") {
        val gen = generatorOver("AAA" to pool("AAA"), "BBB" to pool("BBB"))

        val deck = gen.generate(emptyList(), DeckFormat.MODERN)

        deck.values.sum() shouldBe deckSize
    }

    test("resolves a set's reprint rows through the registry") {
        // A set whose own `cards` are empty but which reprints AAA's pool: without the printings
        // lookup its legal pool would look empty and the build would fail.
        val aaa = pool("AAA")
        val registry = CardRegistry()
        registry.register(aaa)
        registry.register(basics())
        val gen = ConstructedDeckGenerator(
            BoosterGenerator(
                mapOf(
                    "AAA" to BoosterGenerator.SetConfig("AAA", "Set AAA", aaa, basics()),
                    "RPT" to BoosterGenerator.SetConfig(
                        setCode = "RPT",
                        setName = "Reprint Set",
                        cards = emptyList(),
                        basicLands = basics(),
                        printings = aaa.mapIndexed { i, c ->
                            Printing(
                                oracleId = "oracle-$i",
                                name = c.name,
                                setCode = "RPT",
                                collectorNumber = i.toString(),
                            )
                        },
                    ),
                )
            ),
            registry,
        )

        gen.generate(listOf("RPT"), DeckFormat.MODERN).values.sum() shouldBe deckSize
    }

    test("reads a set's own cards' legality from the registry, not the raw set config") {
        // The live server builds a set's booster `cards` from the raw set definitions, which carry
        // no legalFormats; only the registry's copies are stamped with Scryfall legality. Read
        // as-is, every card the set prints itself fell out of the legal pool and the build ran on
        // the set's reprint rows alone.
        val stamped = pool("AAA")
        val raw = stamped.map { it.copy(legalFormats = emptySet()) }
        val registry = CardRegistry()
        registry.register(stamped)
        registry.register(basics())
        val gen = ConstructedDeckGenerator(
            BoosterGenerator(mapOf("AAA" to BoosterGenerator.SetConfig("AAA", "Set AAA", raw, basics()))),
            registry,
            Random(SEED),
        )

        gen.generate(listOf("AAA"), DeckFormat.MODERN).values.sum() shouldBe deckSize
    }

    test("builds a 100-card singleton deck for the Highlander formats") {
        // A wide mono-green pool: a singleton deck needs one distinct card per nonland slot.
        val wide = (1..90).map { i ->
            card("HL $i", "{${i % 5}}{G}", Rarity.COMMON, setOf(DeckFormat.HIGHLANDER, DeckFormat.CANADIAN_HIGHLANDER))
        }
        val gen = generatorOver("AAA" to wide)

        listOf(DeckFormat.HIGHLANDER, DeckFormat.CANADIAN_HIGHLANDER).forEach { format ->
            val deck = gen.generateDeck(listOf("AAA"), format)
            withClue(format.displayName) {
                deck.deckList.values.sum() shouldBe 100
                deck.deckList.filterKeys { !it.startsWith("Forest") }.values.forEach { it shouldBe 1 }
            }
        }
    }

    test("a Canadian Highlander deck spends no points and carries no sideboard") {
        val legal = setOf(DeckFormat.HIGHLANDER, DeckFormat.CANADIAN_HIGHLANDER)
        val wide = (1..90).map { i -> card("HL $i", "{${i % 5}}{G}", Rarity.COMMON, legal) } +
            // On the points list; the generator leaves it out rather than budget for it.
            card("Sol Ring", "{1}", Rarity.UNCOMMON, legal)
        val gen = generatorOver("AAA" to wide)

        val deck = gen.generateDeck(listOf("AAA"), DeckFormat.CANADIAN_HIGHLANDER)

        deck.deckList.keys.contains("Sol Ring") shouldBe false
        deck.sideboard shouldBe emptyMap()
    }

    test("refuses commander-shape formats rather than faking one") {
        // Commander needs a designated commander, singleton construction and a colour-identity
        // constraint — none of which this builder models. RandomDeckResolver relies on the throw to
        // fall back to a limited deck instead of seating an illegal 60-card pile.
        val gen = generatorOver("AAA" to pool("AAA"))

        listOf(DeckFormat.COMMANDER, DeckFormat.BRAWL, DeckFormat.STANDARD_BRAWL).forEach { format ->
            shouldThrow<IllegalArgumentException> { gen.generate(listOf("AAA"), format) }
        }
    }

    test("refuses a format with no legal cards rather than building an illegal deck") {
        // Nothing in the pool is Standard-legal. Failing here is what lets the resolver fall back.
        val gen = generatorOver("AAA" to pool("AAA"))

        shouldThrow<IllegalArgumentException> { gen.generate(listOf("AAA"), DeckFormat.STANDARD) }
    }

    test("refuses a pool too thin to fill the spell slots rather than padding it with lands") {
        // A set whose only legal cards are four R/W reprint rows — the shape of a single-set
        // "matched" build from a set the corpus barely covers. Draftsim padded the missing spell
        // slots with basics and the short-build check counted those lands, so the AI was seated
        // with 44 lands, 16 instants and no creatures. Throwing is what lets the resolver fall back.
        val thin = listOf(
            CardDefinition.instant("Thin Strike", ManaCost.parse("{1}{R}"), ""),
            CardDefinition.instant("Thin Crescendo", ManaCost.parse("{1}{R}"), ""),
            CardDefinition.instant("Thin Shield", ManaCost.parse("{1}{W}"), ""),
            CardDefinition.instant("Thin Web", ManaCost.parse("{W}"), ""),
        ).map { it.copy(legalFormats = setOf(DeckFormat.LEGACY)) }
        val gen = generatorOver("THN" to thin)

        shouldThrow<IllegalArgumentException> { gen.generateDeck(listOf("THN"), DeckFormat.LEGACY) }
    }

    test("refuses a build without enough creatures to play") {
        // Plenty of distinct spells, but a single creature: a deck that can't put a body on the
        // board isn't a deck the AI can win with, whatever its size.
        val spells = (1..30).map { i ->
            CardDefinition.instant("Spell $i", ManaCost.parse("{${i % 4}}{G}"), "")
                .copy(legalFormats = setOf(DeckFormat.MODERN))
        }
        val creatures = (1..1).map { i -> card("Body $i", "{1}{G}", Rarity.COMMON, setOf(DeckFormat.MODERN)) }
        val gen = generatorOver("AAA" to (spells + creatures))

        shouldThrow<IllegalArgumentException> { gen.generateDeck(listOf("AAA"), DeckFormat.MODERN) }
    }

    test("rejects an unknown set code") {
        val gen = generatorOver("AAA" to pool("AAA"))

        shouldThrow<IllegalArgumentException> { gen.generate(listOf("NOPE"), DeckFormat.MODERN) }
    }

    test("keeps a playable land count") {
        val gen = generatorOver("AAA" to pool("AAA"))

        val deck = gen.generate(listOf("AAA"), DeckFormat.MODERN)

        // A constructed land count, set by the curve: never limited's 17, never a flood.
        val lands = deck.entries.filter { it.key.startsWith("Forest") }.sumOf { it.value }
        lands shouldBeInRange 21..25
    }

    test("never exceeds the four-copy limit") {
        val gen = generatorOver("AAA" to pool("AAA"))

        val deck = gen.generate(listOf("AAA"), DeckFormat.MODERN)

        deck.filterKeys { !it.startsWith("Forest") }.values.forEach {
            (it <= maxCopies) shouldBe true
        }
    }

    test("builds four-ofs rather than a flat pile of singletons") {
        // Draftsim caps a name at four physical copies, which is exactly constructed's four-of rule.
        // A build that never stacked a card would mean the pool is being handed over one copy at a
        // time — the shape of deck the unrated random filler used to produce.
        val gen = generatorOver("AAA" to pool("AAA"))

        val deck = gen.generate(listOf("AAA"), DeckFormat.MODERN)

        val spells = deck.filterKeys { !it.startsWith("Forest") }
        withClue("spell copy counts were $spells") {
            spells.values.any { it == maxCopies } shouldBe true
        }
    }

    test("prefers the cards Draftsim rates highest") {
        // The property that separates this from the unrated random filler: given a pool where one
        // slice is rated far above the rest, the build has to come out of that slice. Ratings are
        // keyed by card name, so the pool borrows real names off a set Draftsim actually rated.
        val ratings = DraftsimData.tablesFor(listOf(ratedSet)).ratings.entries.sortedByDescending { it.value }
        val strong = ratings.take(24).map { it.key }
        val weak = ratings.takeLast(120).map { it.key }
        fun named(names: List<String>) =
            names.mapIndexed { i, key -> card(key, "{${i % 4}}{G}", Rarity.COMMON, setOf(DeckFormat.MODERN)) }
        val gen = generatorOver("AAA" to (named(strong) + named(weak)))

        val deck = gen.generate(listOf("AAA"), DeckFormat.MODERN)

        val spells = deck.keys.filterNot { it.startsWith("Forest") }
        val fromStrong = spells.count { it in strong }
        withClue("only $fromStrong of ${spells.size} distinct spells came from the top-rated slice") {
            (fromStrong * 2 > spells.size) shouldBe true
        }
    }

    test("successive builds are not the identical deck") {
        // The shortlist draw is weighted, not deterministic: a constructed lobby that seated the
        // same AI deck every game would be a worse experience than the build this replaces.
        val gen = generatorOver("AAA" to (1..400).map {
            card("Filler $it", "{${it % 4}}{G}", Rarity.COMMON, setOf(DeckFormat.MODERN))
        })

        val decks = (1..6).map { gen.generate(listOf("AAA"), DeckFormat.MODERN) }

        decks.distinct().size shouldNotBe 1
    }

    // --- Constructed deck shape (D5): playsets of the best cards on a low curve, with a plan ---

    val modern = setOf(DeckFormat.MODERN)
    val pipOf = mapOf(Color.WHITE to "W", Color.BLUE to "U", Color.BLACK to "B", Color.RED to "R", Color.GREEN to "G")

    fun removal(name: String, cost: String, rarity: Rarity = Rarity.UNCOMMON): CardDefinition = cardDef(name) {
        manaCost = cost
        typeLine = "Instant"
        oracleText = "Destroy target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = com.wingedsheep.sdk.dsl.Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
        }
        metadata { this.rarity = rarity }
    }.copy(legalFormats = modern)

    /**
     * Five colours deep, every mana value from one to seven, creatures and removal in each — the
     * shape of a whole-format pool, where the old build drew a random shortlist and stacked
     * whichever six-drops it turned up.
     */
    fun widePool(): List<CardDefinition> = Color.entries.flatMap { colour ->
        val pip = pipOf.getValue(colour)
        (1..30).map { i ->
            val mv = (i % 7) + 1
            card("$pip Creature $i", "{${mv - 1}}{$pip}", if (i % 5 == 0) Rarity.RARE else Rarity.COMMON, modern)
        } + (1..8).map { i -> removal("$pip Removal $i", "{${i % 3}}{$pip}") }
    }

    val basicNames = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
    fun isBasic(key: String) = key.substringBefore('#') in basicNames

    fun wideGenerator(seed: Long, cards: List<CardDefinition> = widePool()): ConstructedDeckGenerator {
        val allBasics = listOf(
            CardDefinition.basicLand("Plains", Subtype.PLAINS, ScryfallMetadata(collectorNumber = "301")),
            CardDefinition.basicLand("Island", Subtype.ISLAND, ScryfallMetadata(collectorNumber = "302")),
            CardDefinition.basicLand("Swamp", Subtype.SWAMP, ScryfallMetadata(collectorNumber = "303")),
            CardDefinition.basicLand("Mountain", Subtype.MOUNTAIN, ScryfallMetadata(collectorNumber = "304")),
            CardDefinition.basicLand("Forest", Subtype.FOREST, ScryfallMetadata(collectorNumber = "305")),
        )
        val registry = CardRegistry()
        registry.register(cards)
        registry.register(allBasics)
        val booster = BoosterGenerator(mapOf("WID" to BoosterGenerator.SetConfig("WID", "Wide", cards, allBasics)))
        return ConstructedDeckGenerator(booster, registry, Random(seed))
    }

    test("a whole-format build is a constructed deck: playsets, a low curve, creatures and interaction") {
        val byName = widePool().associateBy { it.name }
        (1L..4L).forEach { seed ->
            val deck = wideGenerator(seed).generate(DeckFormat.MODERN)
            val spells = deck.filterKeys { !isBasic(it) }.mapKeys { (k, _) -> byName.getValue(k) }
            val copies = spells.values.sum()
            fun count(pred: (CardDefinition) -> Boolean) = spells.filterKeys(pred).values.sum()

            withClue("seed $seed: ${deck.entries.joinToString { "${it.value}x ${it.key}" }}") {
                deck.values.sum() shouldBe deckSize
                // The land count follows the curve: this low-curve pool runs 21–24, never limited's 17.
                deck.filterKeys(::isBasic).values.sum() shouldBeInRange 21..24
                // Density: most of the spell slots are in three- and four-ofs, not 1-of filler.
                count { spells.getValue(it) >= 3 } shouldBeGreaterThanOrEqual copies * 3 / 4
                spells.size shouldBeLessThanOrEqual 14
                // A constructed curve: the bulk at one to three, almost nothing at five and up.
                count { it.cmc <= 3 } shouldBeGreaterThanOrEqual copies * 3 / 5
                count { it.cmc >= 5 } shouldBeLessThanOrEqual 5
                count { it.cmc >= 6 } shouldBeLessThanOrEqual 2
                // The creature floor holds and the deck can answer a threat.
                count { it.typeLine.isCreature } shouldBeGreaterThanOrEqual 16
                count { DeckRole.REMOVAL in CommanderDeckRoles.rolesOf(it) } shouldBeGreaterThanOrEqual 8
                // Two colours at most, on a basics manabase that follows them.
                val colours = spells.keys.flatMap { it.colors }.toSet()
                colours.size shouldBeLessThanOrEqual 2
                deck.filterKeys(::isBasic).keys.map { it.substringBefore('#') }.toSet() shouldBe
                    colours.map { BasicLandSplit.BASIC_FOR.getValue(it) }.toSet()
            }
        }
    }

    test("a build-around's tribe fills the deck") {
        // A rare Elf lord whose text names its tribe, twenty Elves and sixty equally rated strangers.
        // The lord reads as the deck's plan, so it goes in and the Elves beat the strangers to the slots.
        val lord = CardDefinition.creature(
            name = "Elvish Captain",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Elf")),
            power = 2,
            toughness = 2,
            oracleText = "Other Elves you control get +1/+1.",
            metadata = ScryfallMetadata(collectorNumber = "1", rarity = Rarity.MYTHIC),
        ).copy(legalFormats = modern)
        val elves = (1..20).map { i ->
            CardDefinition.creature(
                name = "Elf $i",
                manaCost = ManaCost.parse("{${i % 4}}{G}"),
                subtypes = setOf(Subtype("Elf")),
                power = 1,
                toughness = 1,
                metadata = ScryfallMetadata(collectorNumber = "e$i", rarity = Rarity.COMMON),
            ).copy(legalFormats = modern)
        }
        val strangers = (1..60).map { i -> card("Stranger $i", "{${i % 4}}{G}", Rarity.COMMON, modern) }
        val gen = generatorOver("ELF" to (listOf(lord) + elves + strangers))

        val deck = gen.generate(listOf("ELF"), DeckFormat.MODERN)

        withClue("deck was $deck") {
            deck.keys.contains(lord.name) shouldBe true
            deck.filterKeys { it.startsWith("Elf ") }.values.sum() shouldBeGreaterThanOrEqual 24
        }
    }

    // 2026-11-01 deckbuilding review: whole-card-base constructed decks all ran 24 basics on one
    // template curve, never played a dual land, and seated dead cards — four Auratogs with no
    // enchantment, four Wraths of God in a twenty-creature deck.

    val catalog by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun real(name: String, rarity: Rarity = Rarity.MYTHIC): CardDefinition {
        val card = catalog[name] ?: error("$name is not in the card catalog")
        return card.copy(legalFormats = modern, metadata = card.metadata.copy(rarity = rarity))
    }
    fun landsIn(deck: Map<String, Int>, cards: List<CardDefinition>): Int {
        val lands = cards.filter { it.isLand }.map { it.name }.toSet()
        return deck.filterKeys { isBasic(it) || it.substringBefore('#') in lands }.values.sum()
    }

    test("the land count follows the curve: a low curve runs fewer lands than a high one") {
        val low = (1..30).map { card("W Low $it", if (it % 2 == 0) "{W}" else "{1}{W}", Rarity.COMMON, modern) }
        val high = (1..30).map { card("W High $it", "{${2 + it % 3}}{W}", Rarity.COMMON, modern) }

        val lowLands = landsIn(wideGenerator(1, low).generate(DeckFormat.MODERN), low)
        val highLands = landsIn(wideGenerator(1, high).generate(DeckFormat.MODERN), high)

        withClue("low curve ran $lowLands lands, high curve $highLands") {
            (lowLands < highLands) shouldBe true
            lowLands shouldBeInRange 21..23
            highLands shouldBeInRange 24..25
        }
    }

    test("plays the pool's dual lands for its colours") {
        val cards = (1..20).map { card("G Body $it", "{${it % 3}}{G}", Rarity.COMMON, modern) } +
            (1..20).map { card("W Body $it", "{${it % 3}}{W}", Rarity.COMMON, modern) } +
            real("Savannah", Rarity.RARE)

        val deck = wideGenerator(1, cards).generate(DeckFormat.MODERN)

        withClue("deck was $deck") { deck["Savannah"] shouldBe 4 }
    }

    test("a sweeper stays out of a deck full of creatures") {
        val cards = (1..30).map { card("W Body $it", "{${it % 3}}{W}", Rarity.COMMON, modern) } + real("Wrath of God")

        (1L..3L).forEach { seed ->
            val deck = wideGenerator(seed, cards).generate(DeckFormat.MODERN)
            withClue("seed $seed: $deck") { deck.containsKey("Wrath of God") shouldBe false }
        }
    }

    test("a payoff with nothing to feed it stays out, and comes in once the deck feeds it") {
        // Auratog: "Sacrifice an enchantment: put two +1/+1 counters on Auratog."
        val bodies = (1..30).map { card("W Body $it", "{${it % 3}}{W}", Rarity.COMMON, modern) }
        val enchantments = (1..12).map { i ->
            cardDef("W Aura $i") {
                manaCost = "{${i % 2}}{W}"
                typeLine = "Enchantment"
                metadata { rarity = Rarity.RARE }
            }.copy(legalFormats = modern)
        }

        val starved = wideGenerator(1, bodies + real("Auratog")).generate(DeckFormat.MODERN)
        withClue("starved deck was $starved") { starved.containsKey("Auratog") shouldBe false }

        val fed = wideGenerator(1, bodies + enchantments + real("Auratog")).generate(DeckFormat.MODERN)
        withClue("fed deck was $fed") {
            if (fed.keys.count { it.startsWith("W Aura") } >= 2) fed.containsKey("Auratog") shouldBe true
        }
    }
})

/** Fixed seed for [ConstructedDeckGeneratorTest]'s generators. */
private const val SEED = 2927L
