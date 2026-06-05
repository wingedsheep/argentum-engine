// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Raging Minotaur
 * {2}{R}{R}
 * Creature — Minotaur Berserker
 * 3/3
 * Haste
 */
val RagingMinotaur = card("Raging Minotaur") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Berserker"
    power = 3
    toughness = 3
    keywords(Keyword.HASTE)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Scott M. Fischer"
        flavorText = "The only thing worse than a minotaur with an axe is an angry minotaur with an axe."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ea4be95-8147-431c-8bb8-8fe7e5a2ad53.jpg"
    }
}
