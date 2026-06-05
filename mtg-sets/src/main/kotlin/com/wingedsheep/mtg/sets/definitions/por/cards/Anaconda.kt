// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Anaconda
 * {3}{G}
 * Creature — Snake
 * 3/3
 * Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)
 */
val Anaconda = card("Anaconda") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake"
    power = 3
    toughness = 3
    keywords(Keyword.SWAMPWALK)
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Andrew Robinson"
        flavorText = "Something soft bumped against the rowboat, then was gone."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a2012ad-6425-4935-83af-fc7309ec2ece.jpg"
    }
}
