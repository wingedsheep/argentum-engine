package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The "put that card" band: the tutor's anaphor printed as the noun — Demonic Tutor's "put that
 * card into your hand", Entomb's "put that card into your graveyard" — and the graveyard search
 * that had no row at all.
 */
class LibrarySearchThatCardTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun print(line: String): String? = Grammar.abilityLine.printLine(fragment(line))

    "the tutor's that-card spelling reads as the search to hand" {
        fragment(
            "Search your library for a card, put that card into your hand, then shuffle."
        ).script.spellEffect shouldBe Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any,
            destination = SearchDestination.HAND,
        )
    }

    // The noun is the minority printing, so it parses and prints as the pronoun.
    "the that-card spelling normalizes to the pronoun" {
        print(
            "Search your library for a card, put that card into your hand, then shuffle."
        ) shouldBe "Search your library for a card, put it into your hand, then shuffle."
    }

    "the graveyard search reads with either anaphor" {
        val expected = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any,
            destination = SearchDestination.GRAVEYARD,
        )
        fragment(
            "Search your library for a card, put that card into your graveyard, then shuffle."
        ).script.spellEffect shouldBe expected
        fragment(
            "Search your library for a card, put it into your graveyard, then shuffle."
        ).script.spellEffect shouldBe expected
    }

    "the graveyard search round-trips under a trigger" {
        val line = "When ~ enters, search your library for a card, put it into your graveyard, then shuffle."
        print(line) shouldBe line
    }
})
