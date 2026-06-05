// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Grizzly Bears
 * {1}{G}
 * Creature — Bear
 * 2/2
 */
val GrizzlyBears = card("Grizzly Bears") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bear"
    power = 2
    toughness = 2
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "169"
        artist = "Zina Saunders"
        flavorText = "Don't worry about provoking grizzly bears; they come that way."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48e1b99c-97d0-48f2-bfdf-faa65bc0b608.jpg"
    }
}
