package com.wingedsheep.gameserver.ai

import com.wingedsheep.ai.engine.SealedDeckGenerator
import com.wingedsheep.ai.engine.deck.CommanderDeckGenerator
import com.wingedsheep.ai.engine.deck.ConstructedDeckGenerator
import com.wingedsheep.ai.engine.deck.RandomDeckGenerator
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.lobby.AiDeckSpec
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The policy matrix behind "what does a seat with no submitted deck play" — what the seat asked for
 * crossed with what the lobby's format allows.
 *
 * The behaviour under test is the one the feature exists for: a lobby's deck-format restriction used
 * to apply only to a *submitted* deck, so a Pauper or Standard lobby seated a 40-card sealed pool
 * opposite a validated constructed deck. The AI seat was fixed first; a human on "Random" kept the
 * old behaviour until it was routed through this same resolver, which is what the `randomDeck` cases
 * at the bottom pin. Each case covers one cell of the matrix, including the deliberate fallbacks: a
 * legal pool too thin to build from, and a commander-shape lobby whose pool holds no legal commander.
 */
class RandomDeckResolverTest : FunSpec({

    /** Every format the synthetic pool claims legality in, so one `card()` covers every case. */
    val allFormats = setOf(DeckFormat.PAUPER, DeckFormat.MODERN, DeckFormat.COMMANDER, DeckFormat.STANDARD)

    fun card(name: String, cost: String, rarity: Rarity, formats: Set<DeckFormat>) =
        CardDefinition.creature(
            name = name,
            manaCost = ManaCost.parse(cost),
            subtypes = emptySet(),
            power = 2,
            toughness = 2,
            metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = rarity),
        ).copy(legalFormats = formats)

    fun legend(name: String, formats: Set<DeckFormat>) =
        CardDefinition.creature(
            name = name,
            manaCost = ManaCost.parse("{3}{G}"),
            subtypes = emptySet(),
            power = 4,
            toughness = 4,
            supertypes = setOf(Supertype.LEGENDARY),
            metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = Rarity.RARE),
        ).copy(legalFormats = formats)

    /**
     * Deep enough to open 8 boosters without exhausting a rarity, and past the 200-card standalone
     * threshold `randomSetCode()` enforces. Commons are Pauper-legal; rares are Modern-only, so a
     * Pauper build has to leave them behind. Everything green so a mono-green commander can lead a
     * full 99.
     *
     * Only set AAA prints legendary creatures, which is what makes BBB a pool with no legal
     * commander in it — the case the resolver has to fall back from.
     */
    fun pool(prefix: String): List<CardDefinition> =
        (1..200).map {
            card("$prefix Common $it", "{${it % 5}}{G}", Rarity.COMMON, allFormats - DeckFormat.STANDARD)
        } + (1..40).map {
            card("$prefix Uncommon $it", "{${it % 5}}{G}", Rarity.UNCOMMON, allFormats - DeckFormat.STANDARD)
        } + (1..20).map {
            card("$prefix Rare $it", "{${it % 5}}{G}", Rarity.RARE, setOf(DeckFormat.MODERN, DeckFormat.COMMANDER))
        } + if (prefix == "AAA") {
            (1..5).map { legend("$prefix Legend $it", setOf(DeckFormat.MODERN, DeckFormat.COMMANDER)) }
        } else {
            emptyList()
        }

    val basics = listOf(
        CardDefinition.basicLand("Forest", Subtype.FOREST, ScryfallMetadata(collectorNumber = "300")),
    )

    fun resolver(): RandomDeckResolver {
        val configs = listOf("AAA", "BBB").associateWith { code ->
            BoosterGenerator.SetConfig(
                setCode = code,
                setName = "Set $code",
                cards = pool(code),
                basicLands = basics,
            )
        }
        val booster = BoosterGenerator(configs)
        val registry = CardRegistry()
        configs.values.forEach { registry.register(it.cards) }
        registry.register(basics)
        return RandomDeckResolver(
            SealedDeckGenerator(booster),
            ConstructedDeckGenerator(booster, registry),
            CommanderDeckGenerator(booster, registry),
        )
    }

    val sealedSize = 40
    val constructedSize = RandomDeckGenerator.DECK_SIZE
    /** CR 903.5a — 100 cards *including* the commander, which lives outside `deckList`. */
    val commanderLibrarySize = 99

    test("Auto with no format opens a sealed pool from the lobby's set") {
        val deck = resolver().resolve(AiDeckSpec.Auto, format = null, fallbackSetCode = "AAA", commanderRules = false)

        deck.deckList.values.sum() shouldBe sealedSize
    }

    test("Auto under a constructed format builds to that format instead") {
        val deck = resolver().resolve(AiDeckSpec.Auto, format = DeckFormat.MODERN, fallbackSetCode = "AAA", commanderRules = false)

        deck.deckList.values.sum() shouldBe constructedSize
    }

    test("Auto under Pauper uses only Pauper-legal cards") {
        // The regression this feature closes: the AI's deck must respect the same restriction the
        // human's deck was validated against.
        val deck = resolver().resolve(AiDeckSpec.Auto, format = DeckFormat.PAUPER, fallbackSetCode = "AAA", commanderRules = false)

        deck.deckList.keys.filterNot { it.startsWith("Forest") }.forEach { it.contains("Rare") shouldBe false }
    }

    test("Sets pins the sealed pool to the chosen sets") {
        val deck = resolver().resolve(AiDeckSpec.Sets(listOf("BBB")), format = null, fallbackSetCode = "AAA", commanderRules = false)

        deck.deckList.values.sum() shouldBe sealedSize
        deck.deckList.keys.filterNot { it.startsWith("Forest") }.forEach { it.startsWith("BBB") shouldBe true }
    }

    test("Sets under a constructed format builds to the format from those sets") {
        val deck = resolver()
            .resolve(AiDeckSpec.Sets(listOf("BBB")), format = DeckFormat.MODERN, fallbackSetCode = "AAA", commanderRules = false)

        deck.deckList.values.sum() shouldBe constructedSize
        deck.deckList.keys.filterNot { it.startsWith("Forest") }.forEach { it.startsWith("BBB") shouldBe true }
    }

    test("an empty set selection falls back to Auto rather than failing the game start") {
        val deck = resolver().resolve(AiDeckSpec.Sets(emptyList()), format = null, fallbackSetCode = "AAA", commanderRules = false)

        deck.deckList.values.sum() shouldBe sealedSize
    }

    test("Fixed plays the submitted list verbatim, whatever the format") {
        val list = mapOf("Anything" to 4, "Forest" to 56)

        resolver().resolve(AiDeckSpec.Fixed(list), format = null, fallbackSetCode = "AAA", commanderRules = false).deckList shouldBe list
        resolver().resolve(AiDeckSpec.Fixed(list), DeckFormat.PAUPER, "AAA", commanderRules = false).deckList shouldBe list
        resolver().resolve(AiDeckSpec.Fixed(list), DeckFormat.COMMANDER, "AAA", commanderRules = true).deckList shouldBe list
    }

    test("Fixed carries the host's designated commander through") {
        val spec = AiDeckSpec.Fixed(mapOf("Anything" to 99), commander = "AAA Legend 1")

        resolver().resolve(spec, DeckFormat.COMMANDER, "AAA", commanderRules = true).commander shouldBe "AAA Legend 1"
    }

    test("Auto under Commander builds a 100-card singleton deck led by a legal commander") {
        val deck = resolver().resolve(AiDeckSpec.Auto, format = DeckFormat.COMMANDER, fallbackSetCode = "AAA", commanderRules = true)

        deck.commander shouldNotBe null
        deck.commander!!.contains("Legend") shouldBe true
        deck.totalCards shouldBe 100
        deck.deckList.values.sum() shouldBe commanderLibrarySize
        // CR 903.5b: singleton other than basic lands.
        deck.deckList.filterKeys { !it.startsWith("Forest") }.values.forEach { it shouldBe 1 }
    }

    test("Commander rules with no deck-format restriction still build a commander deck") {
        // The premade Commander pod: its Rules axis says commanders, its legality axis says
        // nothing. Reading commander-ness off the format alone would seat a 40-card sealed pool
        // with no commander, which the engine refuses to start.
        val deck = resolver().resolve(AiDeckSpec.Auto, format = null, fallbackSetCode = "AAA", commanderRules = true)

        deck.commander shouldNotBe null
        deck.totalCards shouldBe 100
    }

    test("a commander-shape lobby whose pool holds no legal commander falls back") {
        // Set BBB prints no legendary creatures, so there is nothing to lead a deck. The caller has
        // to notice the missing commander and refuse the start; the resolver's job is not to throw.
        val deck = resolver()
            .resolve(AiDeckSpec.Sets(listOf("BBB")), format = DeckFormat.COMMANDER, fallbackSetCode = "BBB", commanderRules = true)

        deck.commander shouldBe null
        deck.deckList.values.sum() shouldBe sealedSize
    }

    test("a format with no legal cards falls back to a limited deck") {
        // Nothing in the synthetic pool is Standard-legal, so the constructed build throws and the
        // resolver has to recover rather than propagate.
        val deck = resolver().resolve(AiDeckSpec.Auto, format = DeckFormat.STANDARD, fallbackSetCode = "AAA", commanderRules = false)

        deck.deckList.values.sum() shouldBe sealedSize
    }

    test("successive Auto resolutions are not identical decks") {
        // Each game start re-rolls; a cached deck would make every AI game the same match.
        val resolver = resolver()
        val decks = (1..8).map { resolver.resolve(AiDeckSpec.Auto, null, "AAA", commanderRules = false) }

        decks.distinct().size shouldNotBe 1
    }

    test("a human Random seat with no format opens a sealed pool from their set") {
        val deck = resolver().randomDeck(format = null, setCodes = emptyList(), fallbackSetCode = "BBB", commanderRules = false)

        deck.deckList.values.sum() shouldBe sealedSize
        deck.deckList.keys.filterNot { it.startsWith("Forest") }.forEach { it.startsWith("BBB") shouldBe true }
    }

    test("a human Random seat under a constructed format builds to that format") {
        // The asymmetry this closes: the AI seat honoured the lobby format while a human on Random
        // always got a 40-card sealed pool, so a Pauper lobby could seat a rare-filled sealed deck
        // opposite a validated 60-card Pauper deck.
        val deck = resolver().randomDeck(DeckFormat.MODERN, setCodes = emptyList(), fallbackSetCode = "AAA", commanderRules = false)

        deck.deckList.values.sum() shouldBe constructedSize
    }

    test("a human Random seat under Pauper uses only Pauper-legal cards") {
        val deck = resolver().randomDeck(DeckFormat.PAUPER, setCodes = emptyList(), fallbackSetCode = "AAA", commanderRules = false)

        deck.deckList.values.sum() shouldBe constructedSize
        deck.deckList.keys.filterNot { it.startsWith("Forest") }.forEach { it.contains("Rare") shouldBe false }
    }

    test("a human Random seat in a commander lobby gets a commander deck too") {
        // "Random" used to hand a Commander seat a 40-card sealed pool with no commander, which the
        // engine then refused to start on.
        val deck = resolver().randomDeck(DeckFormat.COMMANDER, setCodes = emptyList(), fallbackSetCode = "AAA", commanderRules = true)

        deck.commander shouldNotBe null
        deck.totalCards shouldBe 100
    }
})
