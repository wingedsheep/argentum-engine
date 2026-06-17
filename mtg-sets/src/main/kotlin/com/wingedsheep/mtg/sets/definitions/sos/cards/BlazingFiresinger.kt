package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Blazing Firesinger // Seething Song — Secrets of Strixhaven #109
 * {2}{R} · Creature — Dwarf Bard · 2/3
 *
 * This creature enters prepared. (While it's prepared, you may cast a copy of its spell.
 * Doing so unprepares it.)
 * //
 * Seething Song — {2}{R}, Instant: Add {R}{R}{R}{R}{R}.
 *
 * Prepare (Secrets of Strixhaven): the creature enters with the PREPARED keyword. Becoming
 * prepared creates a copy of its prepare spell ("Seething Song") in exile that its controller
 * may cast for {2}{R}; casting that copy unprepares the creature. Modeled via
 * [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the `prepare(name) { }` DSL.
 */
val BlazingFiresinger = card("Blazing Firesinger") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Bard"
    power = 2
    toughness = 3
    oracleText = "This creature enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.PREPARED)

    // Seething Song — the prepare spell. Add five red mana.
    prepare("Seething Song") {
        manaCost = "{2}{R}"
        typeLine = "Instant"
        oracleText = "Add {R}{R}{R}{R}{R}."
        spell {
            effect = Effects.AddMana(Color.RED, 5)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Ashly Lovett"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3ba971e7-0b7a-4750-896f-7cf063e66b2a.jpg?1775937691"
    }
}
