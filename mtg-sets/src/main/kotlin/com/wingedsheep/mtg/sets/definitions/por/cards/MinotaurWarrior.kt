// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Minotaur Warrior
 * {2}{R}
 * Creature — Minotaur Warrior
 * 2/3
 */
val MinotaurWarrior = card("Minotaur Warrior") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Warrior"
    power = 2
    toughness = 3
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Scott M. Fischer"
        flavorText = "The herd's patience, the stampede's fury."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c694f5db-a4ad-4abd-acff-d6b340d2387c.jpg"
    }
}
