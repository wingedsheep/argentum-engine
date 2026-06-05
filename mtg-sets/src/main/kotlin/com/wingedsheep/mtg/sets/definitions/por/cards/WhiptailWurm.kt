// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Whiptail Wurm
 * {6}{G}
 * Creature — Wurm
 * 8/5
 */
val WhiptailWurm = card("Whiptail Wurm") {
    manaCost = "{6}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 8
    toughness = 5
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Una Fricker"
        flavorText = "It's hard to say for certain which end is more dangerous."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1e76072-e76d-485e-b94c-c29849bc8a6f.jpg"
    }
}
