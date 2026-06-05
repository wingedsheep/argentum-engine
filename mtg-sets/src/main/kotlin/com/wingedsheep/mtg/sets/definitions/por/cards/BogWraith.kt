// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Bog Wraith
 * {3}{B}
 * Creature — Wraith
 * 3/3
 * Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)
 */
val BogWraith = card("Bog Wraith") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wraith"
    power = 3
    toughness = 3
    keywords(Keyword.SWAMPWALK)
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Ted Naifeh"
        flavorText = "It moved through the troops leaving no footprints, save on their souls."
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4487d7d0-d5a5-4b0c-bf30-e0ec511e9aa4.jpg"
    }
}
