// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Elite Cat Warrior
 * {2}{G}
 * Creature — Cat Warrior
 * 2/3
 * Forestwalk (This creature can't be blocked as long as defending player controls a Forest.)
 */
val EliteCatWarrior = card("Elite Cat Warrior") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat Warrior"
    power = 2
    toughness = 3
    keywords(Keyword.FORESTWALK)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "163"
        artist = "Eric Peterson"
        flavorText = "\"Hear that? No? That's a cat warrior.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/3/7396c4e9-b0d8-4b8f-8c17-6f913a967b17.jpg"
    }
}
