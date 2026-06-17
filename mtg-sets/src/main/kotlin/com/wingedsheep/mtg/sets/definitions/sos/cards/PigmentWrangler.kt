package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pigment Wrangler // Striking Palette — Secrets of Strixhaven #126
 * {4}{R} · Creature — Orc Sorcerer · 4/4
 *
 * Flying
 * This creature enters prepared. (While it's prepared, you may cast a copy of its spell.
 * Doing so unprepares it.)
 * //
 * Striking Palette — {R}, Sorcery: When you next cast an instant or sorcery spell this turn,
 * copy that spell. You may choose new targets for the copy.
 *
 * Prepare (Secrets of Strixhaven): the creature enters with the PREPARED keyword. Becoming
 * prepared creates a copy of its prepare spell ("Striking Palette") in exile that its controller
 * may cast for {R}; casting that copy unprepares the creature. Modeled via
 * [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the `prepare(name) { }` DSL.
 */
val PigmentWrangler = card("Pigment Wrangler") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Sorcerer"
    power = 4
    toughness = 4
    oracleText = "Flying\nThis creature enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.FLYING, Keyword.PREPARED)

    // Striking Palette — the prepare spell. Copy the next instant or sorcery you cast this turn.
    prepare("Striking Palette") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        oracleText = "When you next cast an instant or sorcery spell this turn, copy that spell. You may choose new targets for the copy."
        spell {
            effect = Effects.CopyNextSpellCast(1)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Gonzalo Kenny"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2faf4cf-c4b6-4721-ac06-0e045dd9704a.jpg?1775937841"
    }
}
