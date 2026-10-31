package com.wingedsheep.gameserver.ai

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.controller.DecksController
import com.wingedsheep.gameserver.deck.DeckValidator
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.legality.LegalityData
import com.wingedsheep.sdk.core.DeckFormat
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random

/**
 * An Auto AI facing a built-in starter plays a *different* starter from the same set, over the real
 * Bloomburrow tribal lists — the family the feature exists for. Duskmourn rides along as a second
 * set so "which set is this deck from" has a wrong answer available.
 */
class PremadeOpponentPickerTest : FunSpec({

    val registry = CardRegistry().apply {
        for (set in MtgSetCatalog.all) {
            // Stamped as the server's registry is, so format legality reads real Scryfall data.
            register(set.cards.map(LegalityData::stamp))
            register(set.basicLands.map(LegalityData::stamp))
        }
    }
    val booster = BoosterGenerator(
        listOf("BLB", "DSK").associateWith { code ->
            val set = MtgSetCatalog.requireByCode(code)
            BoosterGenerator.SetConfig(
                setCode = code,
                setName = set.displayName,
                cards = set.cards,
                basicLands = set.basicLands,
                printings = set.printings,
            )
        },
    )
    val inference = DeckSetInference(booster, registry)
    val picker = PremadeOpponentPicker(inference, DeckValidator(registry))

    val starters = DecksController.EXAMPLE_DECKS.associateBy { it.id }
    val bloomburrowTribes = listOf(
        "boros_mice", "selesnya_rabbits", "rakdos_lizards", "golgari_squirrels", "simic_frogs", "orzhov_bats",
    )
    val borosMice = starters.getValue("boros_mice").cards

    test("a Bloomburrow starter reads as Bloomburrow") {
        inference.setsOf(borosMice) shouldBe listOf("BLB")
    }

    test("the AI facing a Bloomburrow starter gets another Bloomburrow starter, never the same one") {
        val tribeByCards = bloomburrowTribes.associateBy { starters.getValue(it).cards }
        val picks = (1..40).map { seed ->
            val deck = picker.pick(borosMice, listOf("BLB"), format = null, commanderRules = false, Random(seed))
            deck shouldNotBe null
            deck!!.commander shouldBe null
            deck.deckList.values.sum() shouldBe 60
            tribeByCards[deck.deckList]
        }

        picks.forEach { it shouldBeIn bloomburrowTribes - "boros_mice" }
        // Not a fixed pairing: replaying the same starter meets different tribes.
        picks.distinct().size shouldNotBe 1
    }

    test("a Commander game never seats a 60-card starter") {
        picker.pick(borosMice, listOf("BLB"), format = null, commanderRules = true) shouldBe null
    }

    test("sets the human didn't play offer no sibling") {
        picker.pick(borosMice, listOf("DSK"), format = null, commanderRules = false) shouldBe null
    }

    // --- A constructed list with no set identity is matched by format. Orzhov Lifegain stands in
    // for the human's list — a Standard deck spread over a dozen sets.

    val orzhovLifegain = starters.getValue("standard_orzhov_lifegain").cards

    test("a Standard list spread over many sets has no set identity to match") {
        inference.setsOf(orzhovLifegain) shouldBe emptyList()
    }

    test("the format the deck was built for wins") {
        picker.matchFormat(orzhovLifegain, declared = DeckFormat.PIONEER, lobbyFormat = null) shouldBe DeckFormat.PIONEER
        picker.matchFormat(orzhovLifegain, declared = DeckFormat.HIGHLANDER, lobbyFormat = null) shouldBe DeckFormat.HIGHLANDER
    }

    test("an undeclared list is matched to the narrowest nested format it is legal in") {
        picker.matchFormat(orzhovLifegain, declared = null, lobbyFormat = null) shouldBe DeckFormat.STANDARD
    }

    test("a singleton list is matched like any other") {
        // No playsets at all — strength isn't read off duplicates.
        val singletons = orzhovLifegain.keys.filter { it != "Plains" && it != "Swamp" }
        val list = singletons.associateWith { 1 } + ("Plains" to 60 - singletons.size)
        picker.matchFormat(list, declared = null, lobbyFormat = null) shouldBe DeckFormat.STANDARD
    }

    test("the lobby's restriction bounds the match") {
        // A Standard deck in a Modern lobby meets a Standard deck, not a Modern one.
        picker.matchFormat(orzhovLifegain, declared = null, lobbyFormat = DeckFormat.MODERN) shouldBe DeckFormat.STANDARD
        // A declaration the lobby doesn't admit falls back to the lobby.
        picker.matchFormat(orzhovLifegain, declared = DeckFormat.MODERN, lobbyFormat = DeckFormat.STANDARD) shouldBe DeckFormat.STANDARD
        // Off the nested ladder the lobby's format stands as it is.
        picker.matchFormat(orzhovLifegain, declared = null, lobbyFormat = DeckFormat.PREMODERN) shouldBe DeckFormat.PREMODERN
    }

    test("a Standard list matched by format meets a Standard-legal starter, never its own") {
        val validator = DeckValidator(registry)
        val picks = (1..30).map { seed ->
            val deck = picker.pickForFormat(orzhovLifegain, DeckFormat.STANDARD, Random(seed))
            deck shouldNotBe null
            deck!!.commander shouldBe null
            deck.deckList.values.sum() shouldBe 60
            validator.validate(deck.deckList, DeckFormat.STANDARD).valid shouldBe true
            deck.deckList
        }

        picks.none { it == orzhovLifegain } shouldBe true
        // Random among the qualifying starters, so the AI's list still turns over.
        picks.distinct().size shouldNotBe 1
    }

    test("a format no starter is legal in leaves the build to the generator") {
        picker.pickForFormat(orzhovLifegain, DeckFormat.PAUPER) shouldBe null
        picker.pickForFormat(orzhovLifegain, DeckFormat.COMMANDER) shouldBe null
    }

    test("commander shapes never apply") {
        picker.matchFormat(orzhovLifegain, declared = DeckFormat.COMMANDER, lobbyFormat = null) shouldBe DeckFormat.STANDARD
        picker.matchFormat(orzhovLifegain, declared = null, lobbyFormat = DeckFormat.COMMANDER) shouldBe null
    }
})
