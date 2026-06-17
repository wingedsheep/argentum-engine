package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Maelstrom Artisan // Rocket Volley — Secrets of Strixhaven #122
 * {1}{R}{R} · Creature — Minotaur Sorcerer · 3/2
 *
 * Haste
 * This creature enters prepared. (While it's prepared, you may cast a copy of its spell.
 * Doing so unprepares it.)
 * //
 * Rocket Volley — {1}{R}, Sorcery: Destroy target nonbasic land.
 *
 * Prepare (Secrets of Strixhaven): the creature enters with the PREPARED keyword. Becoming
 * prepared creates a copy of its prepare spell ("Rocket Volley") in exile that its controller
 * may cast for {1}{R}; casting that copy unprepares the creature. Modeled via
 * [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the `prepare(name) { }` DSL.
 *
 * Substituted for Thunderdrum Soloist in this batch — that card is the canonical implementation
 * in PR #831 (Prismari), so it was removed here to avoid a duplicate definition.
 */
val MaelstromArtisan = card("Maelstrom Artisan") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Sorcerer"
    power = 3
    toughness = 2
    oracleText = "Haste\n" +
        "This creature enters prepared. (While it's prepared, you may cast a copy of its spell. " +
        "Doing so unprepares it.)"

    keywords(Keyword.HASTE, Keyword.PREPARED)

    // Rocket Volley — the prepare spell. Destroy target nonbasic land.
    prepare("Rocket Volley") {
        manaCost = "{1}{R}"
        typeLine = "Sorcery"
        oracleText = "Destroy target nonbasic land."
        spell {
            val land = target("target", TargetPermanent(filter = TargetFilter.Land.nonbasic()))
            effect = Effects.Destroy(land)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Eelis Kyttanen"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c88391d-271f-4021-a5d9-158ebc1e6357.jpg?1778165111"
    }
}
