package com.wingedsheep.ai.engine.deck

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CommanderEligibility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Commander build behind an AI seat that has to bring a deck and hasn't.
 *
 * Every case here is a deck-construction rule the server's `DeckValidator` will independently
 * enforce on the same deck, so a build that violates one is a seat the lobby refuses to start:
 * CR 903.3 (a legal commander), 903.5a (exact size, counting the commander), 903.5b (singleton
 * outside basics), 903.5c (colour identity bounds the deck) and 903.5d (which basics are even
 * allowed). The two remaining cases pin the deliberate limitations — no colourless commanders, and a
 * pool with nothing legal to lead it comes back null instead of throwing.
 */
class CommanderDeckGeneratorTest : FunSpec({

    fun spell(name: String, cost: String, rarity: Rarity = Rarity.COMMON): CardDefinition =
        CardDefinition.creature(
            name = name,
            manaCost = ManaCost.parse(cost),
            subtypes = emptySet(),
            power = 2,
            toughness = 2,
            metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = rarity),
        ).copy(legalFormats = setOf(DeckFormat.COMMANDER, DeckFormat.BRAWL, DeckFormat.STANDARD_BRAWL))

    fun legend(name: String, cost: String): CardDefinition =
        CardDefinition.creature(
            name = name,
            manaCost = ManaCost.parse(cost),
            subtypes = emptySet(),
            power = 4,
            toughness = 4,
            supertypes = setOf(Supertype.LEGENDARY),
            metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = Rarity.RARE),
        ).copy(legalFormats = setOf(DeckFormat.COMMANDER, DeckFormat.BRAWL, DeckFormat.STANDARD_BRAWL))

    val basics = listOf(
        CardDefinition.basicLand("Plains", Subtype.PLAINS, ScryfallMetadata(collectorNumber = "301")),
        CardDefinition.basicLand("Island", Subtype.ISLAND, ScryfallMetadata(collectorNumber = "302")),
        CardDefinition.basicLand("Swamp", Subtype.SWAMP, ScryfallMetadata(collectorNumber = "303")),
        CardDefinition.basicLand("Mountain", Subtype.MOUNTAIN, ScryfallMetadata(collectorNumber = "304")),
        CardDefinition.basicLand("Forest", Subtype.FOREST, ScryfallMetadata(collectorNumber = "305")),
    )
    val basicNames = basics.map { it.name }.toSet()

    /** A deck-list key is either a bare card name or `Name#CollectorNumber` for a pinned basic. */
    fun baseName(key: String) = key.substringBefore('#')

    /**
     * Deep enough in one colour to fill a 99, plus off-colour cards a colour-identity violation
     * would reach for and one legend of every relevant identity shape.
     */
    fun pool(): List<CardDefinition> =
        (1..120).map { spell("Green $it", "{${it % 6}}{G}") } +
            (1..60).map { spell("Blue $it", "{${it % 4}}{U}") } +
            (1..20).map { spell("Colorless $it", "{${it % 4}}") } +
            listOf(
                legend("Mono Green Legend", "{2}{G}"),
                legend("Green Blue Legend", "{1}{G}{U}"),
            )

    fun generatorOver(cards: List<CardDefinition>, random: kotlin.random.Random = kotlin.random.Random(7)):
        CommanderDeckGenerator {
        val registry = CardRegistry()
        registry.register(cards)
        registry.register(basics)
        val booster = BoosterGenerator(
            mapOf("AAA" to BoosterGenerator.SetConfig("AAA", "Set AAA", cards, basics))
        )
        return CommanderDeckGenerator(booster, registry, random)
    }

    test("builds exactly 100 cards counting the commander (CR 903.5a)") {
        val deck = generatorOver(pool()).generate(listOf("AAA"), DeckFormat.COMMANDER)!!

        deck.commander shouldNotBe null
        deck.deckList.values.sum() shouldBe 99
        deck.totalCards shouldBe 100
    }

    test("Standard Brawl builds the 60-card shape instead") {
        val deck = generatorOver(pool()).generate(listOf("AAA"), DeckFormat.STANDARD_BRAWL)!!

        deck.totalCards shouldBe 60
    }

    test("the commander is a legal commander (CR 903.3)") {
        val registry = CardRegistry().apply { register(pool()); register(basics) }
        val deck = generatorOver(pool()).generate(listOf("AAA"), DeckFormat.COMMANDER)!!

        val card = registry.getCard(deck.commander!!)!!
        CommanderEligibility.isLegalCommander(card) shouldBe true
    }

    test("a 'can be your commander' planeswalker can lead the deck (CR 903.3a)") {
        // The only legal commander in the pool is a planeswalker with the override clause, so a
        // build that only ever looked for legendary creatures would come back null here.
        val daretti = CardDefinition.planeswalker(
            name = "Daretti, Scrap Savant",
            manaCost = ManaCost.parse("{4}{R}"),
            subtypes = setOf(Subtype("Daretti")),
            startingLoyalty = 3,
            oracleText = "Daretti, Scrap Savant can be your commander.",
        ).copy(legalFormats = setOf(DeckFormat.COMMANDER))
        val red = (1..120).map { spell("Red $it", "{${it % 6}}{R}") }

        val deck = generatorOver(red + daretti).generate(listOf("AAA"), DeckFormat.COMMANDER)!!

        deck.commander shouldBe "Daretti, Scrap Savant"
    }

    test("every non-basic name appears once (CR 903.5b)") {
        val deck = generatorOver(pool()).generate(listOf("AAA"), DeckFormat.COMMANDER)!!

        deck.deckList.filterKeys { baseName(it) !in basicNames }.forEach { (name, count) ->
            withClue("$name appeared $count times in a singleton deck") { count shouldBe 1 }
        }
    }

    test("no card falls outside the commander's colour identity (CR 903.5c)") {
        val registry = CardRegistry().apply { register(pool()); register(basics) }
        val deck = generatorOver(pool()).generate(listOf("AAA"), DeckFormat.COMMANDER)!!
        val identity = registry.getCard(deck.commander!!)!!.colorIdentity

        deck.deckList.keys.forEach { key ->
            val card = registry.getCard(baseName(key))!!
            withClue("${card.name} (${card.colorIdentity}) is outside $identity") {
                card.colorIdentity.all { it in identity } shouldBe true
            }
        }
    }

    test("the manabase only uses basics the identity allows (CR 903.5d)") {
        // A mono-green commander is the only one on offer here, so an Island in the deck would be
        // both an unproducible colour and a colour-identity violation.
        val monoGreen = (1..120).map { spell("Green $it", "{${it % 6}}{G}") } +
            (1..40).map { spell("Blue $it", "{${it % 4}}{U}") } +
            legend("Mono Green Legend", "{2}{G}")

        val deck = generatorOver(monoGreen).generate(listOf("AAA"), DeckFormat.COMMANDER)!!

        deck.commander shouldBe "Mono Green Legend"
        val basicsUsed = deck.deckList.keys.map { baseName(it) }.filter { it in basicNames }.toSet()
        basicsUsed shouldBe setOf("Forest")
    }

    test("a two-colour commander's manabase covers both of its colours") {
        val greenBlueOnly = (1..80).map { spell("Green $it", "{${it % 6}}{G}") } +
            (1..80).map { spell("Blue $it", "{${it % 6}}{U}") } +
            legend("Green Blue Legend", "{1}{G}{U}")

        val deck = generatorOver(greenBlueOnly).generate(listOf("AAA"), DeckFormat.COMMANDER)!!

        val basicsUsed = deck.deckList.keys.map { baseName(it) }.filter { it in basicNames }.toSet()
        basicsUsed shouldBe setOf("Forest", "Island")
    }

    test("a colourless-identity commander is never chosen") {
        // CR 903.5d leaves a colourless deck no legal basic land, and Brawl's "any number of one
        // basic type" exception (CR 903.12e) doesn't exist in Commander. Declining the commander is
        // the honest answer; building it would produce a deck the validator rejects.
        val eldrazi = legend("Void Titan", "{8}").copy(colorIdentityOverride = emptySet())
        val colorless = (1..60).map { spell("Colorless $it", "{${it % 5}}") }

        generatorOver(colorless + eldrazi).generate(listOf("AAA"), DeckFormat.COMMANDER) shouldBe null
    }

    test("a pool with no legal commander returns null rather than throwing") {
        val deck = generatorOver((1..80).map { spell("Green $it", "{${it % 5}}{G}") })
            .generate(listOf("AAA"), DeckFormat.COMMANDER)

        deck shouldBe null
    }

    test("still reaches the exact size when the identity-legal pool is tiny") {
        // Ten castable spells and a 99-card library: the rest has to come back as basics, because a
        // deck one card short is a deck the validator rejects.
        val thin = (1..10).map { spell("Green $it", "{${it % 4}}{G}") } + legend("Mono Green Legend", "{2}{G}")

        val deck = generatorOver(thin).generate(listOf("AAA"), DeckFormat.COMMANDER)!!

        deck.totalCards shouldBe 100
        deck.deckList.keys.map { baseName(it) }.filter { it in basicNames }.toSet() shouldBe setOf("Forest")
    }

    test("refuses a non-commander format") {
        shouldThrow<IllegalArgumentException> {
            generatorOver(pool()).generate(listOf("AAA"), DeckFormat.MODERN)
        }
    }

    test("successive builds are not the identical deck") {
        val gen = generatorOver(pool(), kotlin.random.Random.Default)

        val decks = (1..6).map { gen.generate(listOf("AAA"), DeckFormat.COMMANDER) }

        decks.distinct().size shouldNotBe 1
    }

    test("keeps roughly the paper land ratio") {
        val deck = generatorOver(pool()).generate(listOf("AAA"), DeckFormat.COMMANDER)!!

        val lands = deck.deckList.entries.filter { baseName(it.key) in basicNames }.sumOf { it.value }
        withClue("$lands lands in a 99-card library") { (lands in 34..42) shouldBe true }
    }

    test("a limited pool builds to the lobby's deck size and may repeat what it owns") {
        // The drafted/sealed path: the pool is the legality universe, duplicates are allowed, and
        // the manabase stays on plain basic names so the lobby's pool check recognises it.
        val drafted = (1..12).flatMap { i -> List(3) { spell("Green $i", "{${i % 4}}{G}") } } +
            legend("Mono Green Legend", "{2}{G}")

        val deck = generatorOver(drafted).generateFromPool(drafted, deckSize = 60)!!

        deck.totalCards shouldBe 60
        deck.commander shouldBe "Mono Green Legend"
        deck.deckList.keys.filter { it in basicNames }.toSet() shouldBe setOf("Forest")
        deck.deckList.keys.none { it.contains('#') } shouldBe true
        withClue("expected the extra copies the pool holds to be used: ${deck.deckList}") {
            deck.deckList.filterKeys { it !in basicNames }.values.any { it > 1 } shouldBe true
        }
    }

    test("a limited build never plays more copies than the pool holds") {
        val drafted = (1..12).flatMap { i -> List(2) { spell("Green $i", "{${i % 4}}{G}") } } +
            legend("Mono Green Legend", "{2}{G}")

        val deck = generatorOver(drafted).generateFromPool(drafted, deckSize = 60)!!

        deck.deckList.filterKeys { it !in basicNames }.forEach { (name, count) ->
            withClue("$name: $count copies from a pool holding 2") { (count <= 2) shouldBe true }
        }
    }

    test("a limited build honours the lobby's singleton toggle") {
        val drafted = (1..40).flatMap { i -> List(3) { spell("Green $i", "{${i % 4}}{G}") } } +
            legend("Mono Green Legend", "{2}{G}")

        val deck = generatorOver(drafted).generateFromPool(drafted, deckSize = 60, allowDuplicates = false)!!

        deck.deckList.filterKeys { it !in basicNames }.values.forEach { it shouldBe 1 }
    }

    test("a limited pool with no legal commander returns null") {
        val drafted = (1..30).map { spell("Green $it", "{${it % 4}}{G}") }

        generatorOver(drafted).generateFromPool(drafted, deckSize = 60) shouldBe null
    }

    test("colour identity is read from oracle text, not just the mana cost") {
        // CR 903.4: a mana symbol anywhere in the rules text counts. A green commander's deck can't
        // contain a colourless-costed card whose activated ability needs {U}.
        val offColour = spell("Sneaky Artifact", "{2}")
            .copy(oracleText = "{U}, {T}: Draw a card.")
        val monoGreen = (1..120).map { spell("Green $it", "{${it % 6}}{G}") } +
            offColour +
            legend("Mono Green Legend", "{2}{G}")

        val deck = generatorOver(monoGreen).generate(listOf("AAA"), DeckFormat.COMMANDER)!!

        deck.deckList.keys.contains("Sneaky Artifact") shouldBe false
    }
    // ---- Role quotas and synergy (2026-10-31: generated decks were 38 basics + unrelated singletons) ----

    val legal = setOf(DeckFormat.COMMANDER, DeckFormat.BRAWL, DeckFormat.STANDARD_BRAWL)
    fun colourOf(c: Char) = mapOf('W' to Color.WHITE, 'U' to Color.BLUE, 'B' to Color.BLACK, 'R' to Color.RED, 'G' to Color.GREEN).getValue(c)

    /** A mana dork / rock: a permanent with a `{T}: Add` ability for each of [colours]. */
    fun rock(name: String, cost: String, colours: String): CardDefinition = card(name) {
        manaCost = cost
        colorIdentity = colours
        typeLine = "Artifact"
        oracleText = colours.map { "{T}: Add {$it}." }.joinToString("\n")
        colours.forEach { c ->
            activatedAbility {
                this.cost = AbilityCost.Tap
                effect = Effects.AddMana(colourOf(c))
                manaAbility = true
                timing = TimingRule.ManaAbility
            }
        }
    }.copy(legalFormats = legal)

    /** Rampant Growth: fetch a basic onto the battlefield — ramp that fixes any colour. */
    fun landFetch(name: String, cost: String): CardDefinition = card(name) {
        manaCost = cost
        typeLine = "Sorcery"
        oracleText = "Search your library for a basic land card, put that card onto the battlefield tapped, then shuffle."
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
            )
        }
    }.copy(legalFormats = legal)

    fun removal(name: String, cost: String): CardDefinition = card(name) {
        manaCost = cost
        typeLine = "Instant"
        oracleText = "Destroy target creature."
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
        }
    }.copy(legalFormats = legal)

    fun draw(name: String, cost: String): CardDefinition = card(name) {
        manaCost = cost
        typeLine = "Sorcery"
        oracleText = "Draw two cards."
        spell { effect = Effects.DrawCards(2) }
    }.copy(legalFormats = legal)

    /** An untapped dual: `{T}: Add {X}` / `{T}: Add {Y}`. */
    fun dual(name: String, colours: String): CardDefinition = card(name) {
        typeLine = "Land"
        colorIdentity = colours
        oracleText = colours.map { "{T}: Add {$it}." }.joinToString("\n")
        colours.forEach { c ->
            activatedAbility {
                cost = AbilityCost.Tap
                effect = Effects.AddMana(colourOf(c))
                manaAbility = true
                timing = TimingRule.ManaAbility
            }
        }
    }.copy(legalFormats = legal)

    /**
     * A pool deep in [colours] with more of every role than a deck needs, so a quota the build
     * misses is the builder's fault, not the pool's. Vanilla filler outnumbers the role cards
     * four to one and is rated the same, so rating and curve alone would play about a fifth of
     * each quota.
     */
    fun rolePool(colours: String, tag: String): List<CardDefinition> {
        val pips = colours.toList()
        fun pip(i: Int) = "{${pips[i % pips.size]}}"
        return (1..240).map { spell("$tag Filler $it", "{${it % 6}}${pip(it)}") } +
            (1..20).map { rock("$tag Rock $it", "{${1 + it % 3}}", "${pips[it % pips.size]}") } +
            (1..20).map { removal("$tag Removal $it", "{${1 + it % 3}}${pip(it)}") } +
            (1..20).map { draw("$tag Draw $it", "{${1 + it % 3}}${pip(it)}") }
    }

    fun roleCounts(deck: GeneratedDeck, registry: CardRegistry): Map<DeckRole, Int> {
        val cards = deck.deckList.keys.mapNotNull { registry.getCard(baseName(it)) }
        return DeckRole.entries.associateWith { role -> cards.count { role in CommanderDeckRoles.rolesOf(it) } }
    }

    fun landCount(deck: GeneratedDeck, registry: CardRegistry): Int =
        deck.deckList.entries.sumOf { (key, n) -> if (registry.getCard(baseName(key))?.isLand == true) n else 0 }

    fun registryOf(cards: List<CardDefinition>) = CardRegistry().apply { register(cards); register(basics) }

    test("a mono-colour deck fills its ramp, interaction and draw quotas and plays 36-38 lands") {
        val cards = rolePool("G", "Mono") + legend("Mono Green Legend", "{2}{G}")
        val registry = registryOf(cards)

        val deck = generatorOver(cards).generate(listOf("AAA"), DeckFormat.COMMANDER)!!
        val roles = roleCounts(deck, registry)

        withClue("roles $roles") {
            roles.getValue(DeckRole.RAMP) shouldBeGreaterThanOrEqual 10
            roles.getValue(DeckRole.REMOVAL) shouldBeGreaterThanOrEqual 8
            roles.getValue(DeckRole.DRAW) shouldBeGreaterThanOrEqual 7
        }
        landCount(deck, registry) shouldBeInRange 36..38
        deck.totalCards shouldBe 100
    }

    test("a two-colour deck meets the quotas and plays its on-colour duals") {
        val duals = (1..12).map { dual("GU Dual $it", "GU") }
        val offColour = (1..6).map { dual("GR Dual $it", "GR") }
        val cards = rolePool("GU", "Simic") + duals + offColour + legend("Green Blue Legend", "{1}{G}{U}")
        val registry = registryOf(cards)

        val deck = generatorOver(cards).generate(listOf("AAA"), DeckFormat.COMMANDER)!!
        val roles = roleCounts(deck, registry)
        val names = deck.deckList.keys.map { baseName(it) }

        withClue("roles $roles") {
            roles.getValue(DeckRole.RAMP) shouldBeGreaterThanOrEqual 10
            roles.getValue(DeckRole.REMOVAL) shouldBeGreaterThanOrEqual 8
            roles.getValue(DeckRole.DRAW) shouldBeGreaterThanOrEqual 7
        }
        landCount(deck, registry) shouldBeInRange 36..38
        withClue("on-colour duals in $names") { names.count { it.startsWith("GU Dual") } shouldBeGreaterThanOrEqual 6 }
        withClue("off-colour duals break CR 903.5c") { names.none { it.startsWith("GR Dual") } shouldBe true }
        names.filter { it in basicNames }.toSet() shouldBe setOf("Forest", "Island")
    }

    test("a three-colour deck fixes: half its ramp makes two colours or fetches basics, and it plays the pool's duals") {
        val fixingRocks = (1..8).map { rock("Talisman $it", "{2}", listOf("GU", "UB", "BG")[it % 3]) }
        val fetches = (1..6).map { landFetch("Rampant Fetch $it", "{1}{G}") }
        val duals = listOf("GU", "UB", "BG").flatMap { pair -> (1..5).map { dual("$pair Dual $it", pair) } }
        val cards = rolePool("GUB", "Sultai") + fixingRocks + fetches + duals + legend("Sultai Legend", "{G}{U}{B}")
        val registry = registryOf(cards)

        val deck = generatorOver(cards).generate(listOf("AAA"), DeckFormat.COMMANDER)!!
        val deckCards = deck.deckList.keys.mapNotNull { registry.getCard(baseName(it)) }
        val identity = setOf(Color.GREEN, Color.BLUE, Color.BLACK)
        val fixingRamp = deckCards.count { DeckRole.RAMP in CommanderDeckRoles.rolesOf(it) && CommanderDeckRoles.fixes(it, identity) }
        val basicCounts = deck.deckList.filterKeys { baseName(it) in basicNames }.mapKeys { baseName(it.key) }

        withClue("roles ${roleCounts(deck, registry)}") {
            roleCounts(deck, registry).getValue(DeckRole.RAMP) shouldBeGreaterThanOrEqual 10
        }
        fixingRamp shouldBeGreaterThanOrEqual 5
        landCount(deck, registry) shouldBeInRange 36..38
        withClue("duals in ${deck.deckList.keys}") { deckCards.count { it.isLand && !it.typeLine.isBasicLand } shouldBeGreaterThanOrEqual 10 }
        withClue("basics $basicCounts") {
            basicCounts.keys shouldBe setOf("Forest", "Island", "Swamp")
            basicCounts.values.forEach { it shouldBeGreaterThanOrEqual 4 }
        }
    }

    test("a tribal commander's deck is mostly its tribe when the pool allows") {
        val elfLord = CardDefinition.creature(
            name = "Elf Lord Legend",
            manaCost = ManaCost.parse("{2}{G}"),
            subtypes = setOf(Subtype("Elf")),
            power = 2,
            toughness = 2,
            oracleText = "Other Elves you control get +1/+1.",
            supertypes = setOf(Supertype.LEGENDARY),
            metadata = ScryfallMetadata(collectorNumber = "999", rarity = Rarity.RARE),
        ).copy(legalFormats = legal)
        fun tribe(type: String, i: Int) = CardDefinition.creature(
            name = "$type $i",
            manaCost = ManaCost.parse("{${i % 6}}{G}"),
            subtypes = setOf(Subtype(type)),
            power = 2,
            toughness = 2,
            metadata = ScryfallMetadata(collectorNumber = "$type$i"),
        ).copy(legalFormats = legal)
        val cards = listOf("Elf", "Goblin", "Merfolk").flatMap { type -> (1..45).map { tribe(type, it) } } + elfLord

        val deck = generatorOver(cards).generate(listOf("AAA"), DeckFormat.COMMANDER)!!
        val typed = deck.deckList.keys.filter { baseName(it) !in basicNames }
        val elves = typed.count { it.startsWith("Elf ") }

        // A third of the pool is Elves; a deck that ignored the commander would be a third Elves.
        withClue("$elves Elves of ${typed.size} typed creatures") { (elves * 2 > typed.size) shouldBe true }
    }

    test("a commander that rewards a keyword draws mostly cards with it") {
        val skyLord = CardDefinition.creature(
            name = "Sky Marshal Legend",
            manaCost = ManaCost.parse("{2}{W}"),
            subtypes = emptySet(),
            power = 2,
            toughness = 2,
            oracleText = "Creatures you control with flying get +1/+1.",
            supertypes = setOf(Supertype.LEGENDARY),
            metadata = ScryfallMetadata(collectorNumber = "998", rarity = Rarity.RARE),
        ).copy(legalFormats = legal)
        fun keyworded(kw: Keyword, i: Int) = CardDefinition.creature(
            name = "${kw.displayName} $i",
            manaCost = ManaCost.parse("{${i % 6}}{W}"),
            subtypes = emptySet(),
            power = 2,
            toughness = 2,
            keywords = setOf(kw),
            metadata = ScryfallMetadata(collectorNumber = "${kw.name}$i"),
        ).copy(legalFormats = legal)
        val cards = listOf(Keyword.FLYING, Keyword.VIGILANCE, Keyword.FIRST_STRIKE)
            .flatMap { kw -> (1..45).map { keyworded(kw, it) } } + skyLord

        val deck = generatorOver(cards).generate(listOf("AAA"), DeckFormat.COMMANDER)!!
        val picks = deck.deckList.keys.filter { baseName(it) !in basicNames }
        val flyers = picks.count { it.startsWith("Flying ") }

        withClue("$flyers flyers of ${picks.size}") { (flyers * 2 > picks.size) shouldBe true }
    }

    test("a commander whose identity can't fill the library isn't picked over one that can") {
        // 2026-11-01: a Bloomburrow-only Commander deck drew Eluge (mono-blue) from a set with too
        // few blue singletons to fill a 99, and seated 46 lands. The narrow identity bias still
        // applies — but only among commanders the pool can actually build.
        val thin = (1..20).map { spell("Blue $it", "{${it % 4}}{U}") } +
            (1..10).map { spell("Colorless $it", "{${it % 4}}") } +
            (1..80).map { spell("Green $it", "{${it % 6}}{G}") } +
            listOf(legend("Mono Blue Legend", "{2}{U}"), legend("Green Blue Legend", "{1}{G}{U}"))
        repeat(20) { seed ->
            val deck = generatorOver(thin, kotlin.random.Random(seed)).generate(listOf("AAA"), DeckFormat.COMMANDER)!!
            withClue("seed $seed") { deck.commander shouldBe "Green Blue Legend" }
        }
    }
})
