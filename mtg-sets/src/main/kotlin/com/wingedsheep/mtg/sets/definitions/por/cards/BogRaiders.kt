// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Bog Raiders
 * {2}{B}
 * Creature — Zombie
 * 2/2
 * Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)
 */
val BogRaiders = card("Bog Raiders") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2
    keywords(Keyword.SWAMPWALK)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Steve Luke"
        flavorText = "Those who live amid decay must expect scavengers."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb7bbb7a-b59a-4a01-b1cb-66eef881ffcd.jpg"
    }
}
