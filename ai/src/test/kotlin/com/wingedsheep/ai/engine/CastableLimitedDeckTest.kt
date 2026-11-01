package com.wingedsheep.ai.engine

import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.definitions.fra.cards.Countersculpt
import com.wingedsheep.mtg.sets.definitions.fra.cards.EntrustTheSpark
import com.wingedsheep.mtg.sets.definitions.fra.cards.KarnGildedGuardian
import com.wingedsheep.mtg.sets.definitions.fra.cards.LoyalTutor
import com.wingedsheep.mtg.sets.definitions.msh.cards.TheUnbeatableSquirrelGirl
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe

/**
 * The heuristic limited deck builder (tournament AI decks) builds a deck it can cast. Regressions
 * from the 2026-10-15 Reality Fracture and Marvel games: basics split by raw pip count left
 * Squirrel Girl ({1}{G}{G}{G}) on 3 Forests (c71b39d3); Karn, Gilded Guardian (five `{2/C}` pips)
 * made a W/B deck (ca15647d); Loyal Tutor and Entrust the Spark sat in hand in decks with no or one
 * planeswalker (577f03e0, c13e39d1); a deck ran about 8 creatures beside 3x Unsummon (32b8e508).
 */
class CastableLimitedDeckTest : FunSpec({

    fun creature(name: String, manaCost: String, power: Int = 2, toughness: Int = 2) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(manaCost),
        subtypes = emptySet(),
        power = power,
        toughness = toughness,
        metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = Rarity.COMMON),
    )

    fun walker(name: String, manaCost: String) = CardDefinition.planeswalker(
        name = name,
        manaCost = ManaCost.parse(manaCost),
        subtypes = emptySet(),
        startingLoyalty = 4,
        metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = Rarity.RARE),
    )

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun real(name: String) = byName[name] ?: error("$name is not in the card catalog")

    test("the basic split follows pip intensity: a {1}{G}{G}{G} card gets enough Forests") {
        // White has more cards and pips by raw count; green's demand is one triple-pip four-drop.
        val pool = (1..14).map { creature("White $it", "{1}{W}") } +
            (1..8).map { creature("Green $it", "{3}{G}") } +
            TheUnbeatableSquirrelGirl +
            (1..3).map { creature("Red $it", "{4}{R}") }

        val deck = buildHeuristicSealedDeck(pool)

        deck.values.sum() shouldBe 40
        deck shouldContainKey "The Unbeatable Squirrel Girl"
        (deck["Forest"] ?: 0) shouldBeGreaterThanOrEqual 10
        (deck["Plains"] ?: 0) shouldBeGreaterThanOrEqual 6
    }

    test("a double-pip card the mana base can't support makes way for one it can") {
        // Blue is the minor color; once green's triple pip claims 10 sources, {U}{U} can't get 8.
        val pool = (1..14).map { creature("Green $it", "{2}{G}{G}{G}") } +
            (1..8).map { creature("Blue $it", "{3}{U}") } +
            Countersculpt +
            (1..3).map { creature("Blue Extra $it", "{2}{U}", power = 1, toughness = 1) }

        val deck = buildHeuristicSealedDeck(pool)

        deck.values.sum() shouldBe 40
        deck shouldNotContainKey "Countersculpt"
        (deck["Forest"] ?: 0) shouldBeGreaterThanOrEqual 10
    }

    test("a five-color {2/C} card stays out of a two-color deck") {
        castableWithColors(KarnGildedGuardian, setOf(Color.WHITE, Color.BLACK)) shouldBe false
        castableWithColors(creature("One Twobrid", "{1}{W}{2/B}"), setOf(Color.WHITE, Color.GREEN)) shouldBe true

        val pool = (1..12).map { creature("White $it", "{1}{W}") } +
            (1..12).map { creature("Black $it", "{1}{B}") } +
            KarnGildedGuardian

        val deck = buildHeuristicSealedDeck(pool)

        deck.values.sum() shouldBe 40
        deck shouldNotContainKey "Karn, Gilded Guardian"
    }

    test("a planeswalker tutor stays out of a deck with no planeswalkers") {
        DeckDependencies.inert(LoyalTutor, emptyList()) shouldBe true
        DeckDependencies.inert(LoyalTutor, (1..3).map { walker("Walker $it", "{2}{W}{W}") }) shouldBe false

        // 22 on-color cards + Loyal Tutor: exactly 23 candidates, so only the dependency check cuts it.
        val pool = (1..11).map { creature("White $it", "{1}{W}") } +
            (1..11).map { creature("Black $it", "{1}{B}") } +
            LoyalTutor

        val deck = buildHeuristicSealedDeck(pool)

        deck.values.sum() shouldBe 40
        deck shouldNotContainKey "Loyal Tutor"
    }

    test("sacrifice-a-planeswalker stays out of a deck with a single planeswalker") {
        DeckDependencies.inert(EntrustTheSpark, listOf(walker("Lone Walker", "{2}{U}{U}"))) shouldBe true
        DeckDependencies.inert(EntrustTheSpark, (1..3).map { walker("Walker $it", "{2}{U}{U}") }) shouldBe false

        val pool = (1..11).map { creature("Green $it", "{1}{G}") } +
            (1..10).map { creature("Blue $it", "{1}{U}") } +
            walker("Lone Walker", "{2}{U}{U}") +
            EntrustTheSpark

        val deck = buildHeuristicSealedDeck(pool)

        deck.values.sum() shouldBe 40
        deck shouldContainKey "Lone Walker"
        deck shouldNotContainKey "Entrust the Spark"
    }

    test("the deck runs at least 14 creatures when the pool has them, ahead of tricks") {
        val spells = List(4) { real("Unsummon") } + List(4) { real("Giant Growth") } +
            List(4) { real("Divination") } + List(4) { real("Countersculpt") }
        val creatures = (1..8).map { creature("Green Wall $it", "{4}{G}", power = 0, toughness = 1) } +
            (1..8).map { creature("Blue Wall $it", "{4}{U}", power = 0, toughness = 1) }

        val deck = buildHeuristicSealedDeck(spells + creatures)

        deck.values.sum() shouldBe 40
        val creatureCount = deck.entries.sumOf { (name, n) -> if (name.contains("Wall")) n else 0 }
        creatureCount shouldBeGreaterThanOrEqual CREATURES_PER_40
    }

    test("a devoid card is castable only where its cost is, whatever its colour") {
        // Writhing Chrysalis is colourless (devoid) at {2}{R}{G}: a constructed U/R deck ran four.
        castableWithColors(real("Writhing Chrysalis"), setOf(Color.BLUE, Color.RED)) shouldBe false
        castableWithColors(real("Writhing Chrysalis"), setOf(Color.RED, Color.GREEN)) shouldBe true
    }

    test("a Room is castable only where both doors are") {
        // Funeral Room // Awakening Hall has no cost of its own; its doors cost {2}{B} and {6}{B}{B}.
        val room = real("Funeral Room // Awakening Hall")
        castableWithColors(room, setOf(Color.GREEN, Color.WHITE)) shouldBe false
        castableWithColors(room, setOf(Color.BLACK, Color.WHITE)) shouldBe true
        spellColors(room) shouldBe setOf(Color.BLACK)
    }

    test("a mana rock that makes none of the deck's colours doesn't fit it") {
        // Mardu Banner taps for R, W or B: filler in a U/G deck, fixing in an R/W one.
        fitsDeckColors(real("Mardu Banner"), setOf(Color.BLUE, Color.GREEN)) shouldBe false
        fitsDeckColors(real("Mardu Banner"), setOf(Color.RED, Color.WHITE)) shouldBe true
        // A plain colourless artifact still fits anywhere.
        fitsDeckColors(creature("Golem", "{3}"), setOf(Color.BLUE, Color.GREEN)) shouldBe true
    }
})
