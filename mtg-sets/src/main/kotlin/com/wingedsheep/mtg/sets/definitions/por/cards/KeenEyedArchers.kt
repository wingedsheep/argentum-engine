// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Keen-Eyed Archers
 * {2}{W}
 * Creature — Elf Archer
 * 2/2
 * Reach (This creature can block creatures with flying.)
 */
val KeenEyedArchers = card("Keen-Eyed Archers") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elf Archer"
    power = 2
    toughness = 2
    keywords(Keyword.REACH)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Alan Rabinowitz"
        flavorText = "\"If it has wings, shoot it. If it doesn't, shoot it anyway.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/594de429-58ed-4a0c-9631-464cde7a48c3.jpg"
    }
}
