package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Studious First-Year // Rampant Growth — Secrets of Strixhaven #162
 * {G} · Creature — Bear Wizard · 1/1
 *
 * This creature enters prepared. (While it's prepared, you may cast a copy of its spell.
 * Doing so unprepares it.)
 * //
 * Rampant Growth — {1}{G}, Sorcery: Search your library for a basic land card, put that
 * card onto the battlefield tapped, then shuffle.
 *
 * Prepare (Secrets of Strixhaven): the creature enters with the PREPARED keyword. Becoming
 * prepared creates a copy of its prepare spell ("Rampant Growth") in exile that its controller
 * may cast for {1}{G}; casting that copy unprepares the creature. Modeled via
 * [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the `prepare(name) { }` DSL.
 */
val StudiousFirstYear = card("Studious First-Year") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bear Wizard"
    power = 1
    toughness = 1
    oracleText = "This creature enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.PREPARED)

    // Rampant Growth — the prepare spell. Search for a basic land, put it onto the battlefield tapped.
    prepare("Rampant Growth") {
        manaCost = "{1}{G}"
        typeLine = "Sorcery"
        oracleText = "Search your library for a basic land card, put that card onto the battlefield tapped, then shuffle."
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Mariah Tekulve"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24f888dd-785c-4089-a89c-03f9080130ed.jpg?1778165149"
    }
}
