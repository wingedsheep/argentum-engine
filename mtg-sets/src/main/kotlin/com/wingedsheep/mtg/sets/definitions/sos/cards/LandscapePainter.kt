package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Landscape Painter // Vibrant Idea — Secrets of Strixhaven #56
 * {1}{U} · Creature — Merfolk Wizard · 2/1
 *
 * This creature enters prepared. (While it's prepared, you may cast a copy of its spell.
 * Doing so unprepares it.)
 * //
 * Vibrant Idea — {4}{U}, Sorcery: Draw two cards.
 *
 * Prepare (Secrets of Strixhaven): the creature enters with the PREPARED keyword. Becoming
 * prepared creates a copy of its prepare spell ("Vibrant Idea") in exile that its controller may
 * cast for {4}{U}; casting that copy unprepares the creature. Modeled via [CardLayout.PREPARE] +
 * the `prepare(name) { }` DSL.
 */
val LandscapePainter = card("Landscape Painter") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 1
    oracleText = "This creature enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.PREPARED)

    // Vibrant Idea — the prepare spell. Draw two cards.
    prepare("Vibrant Idea") {
        manaCost = "{4}{U}"
        typeLine = "Sorcery"
        oracleText = "Draw two cards."
        spell {
            effect = Effects.DrawCards(2)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Vincent Christiaens"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0bd30c4-3cdf-4eda-8be5-0fb5e5ddddbf.jpg?1778165077"
    }
}
