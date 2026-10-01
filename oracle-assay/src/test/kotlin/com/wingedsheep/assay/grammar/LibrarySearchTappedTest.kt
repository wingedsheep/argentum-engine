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
 * The tapped-fetch band: "search your library for a basic land card, put it onto the battlefield
 * tapped, then shuffle" — `Patterns.Library.searchLibrary` with `entersTapped`.
 */
class LibrarySearchTappedTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun print(line: String): String? = Grammar.abilityLine.printLine(fragment(line))

    "the tapped fetch reads as a search that puts the land onto the battlefield tapped" {
        val script = fragment(
            "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."
        ).script

        script.spellEffect shouldBe Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
        )
    }

    "the fetch-land activation round-trips" {
        val line = "{T}, Sacrifice ~: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."
        print(line) shouldBe line
    }

    // "that card" is the minority printing of the same recipe, so it parses and prints as the pronoun.
    "the that-card spelling normalizes to the pronoun" {
        print(
            "Search your library for a basic land card, put that card onto the battlefield tapped, then shuffle."
        ) shouldBe "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."
    }

    // Fail-closed: the untapped search keeps its own sentence rather than borrowing the tapped one.
    "the untapped search does not print tapped" {
        print(
            "Search your library for a Forest card, put that card onto the battlefield, then shuffle."
        ) shouldBe "Search your library for a Forest card, put that card onto the battlefield, then shuffle."
    }
})
