// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Arrogant Vampire
 * {3}{B}{B}
 * Creature — Vampire
 * 4/3
 * Flying
 */
val ArrogantVampire = card("Arrogant Vampire") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 4
    toughness = 3
    keywords(Keyword.FLYING)
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Zina Saunders"
        flavorText = "Charm and grace may hide a foul heart."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7342875-d49b-4fa7-a2fb-8dfc5e3d6e4f.jpg"
    }
}
