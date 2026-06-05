// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Elvish Ranger
 * {2}{G}
 * Creature — Elf Ranger
 * 4/1
 */
val ElvishRanger = card("Elvish Ranger") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Ranger"
    power = 4
    toughness = 1
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "DiTerlizzi"
        flavorText = "It's up to you if you enter their woods—and up to them if you leave."
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26caff65-3a96-46f2-8f0b-e5091b632a2e.jpg"
    }
}
