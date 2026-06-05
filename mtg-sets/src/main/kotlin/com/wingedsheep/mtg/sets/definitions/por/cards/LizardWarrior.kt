// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Lizard Warrior
 * {3}{R}
 * Creature — Lizard Warrior
 * 4/2
 */
val LizardWarrior = card("Lizard Warrior") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard Warrior"
    power = 4
    toughness = 2
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Roger Raupp"
        flavorText = "Don't let its appearance frighten you. Let its claws and teeth do that."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/053cd970-5b79-410b-8420-82d9a490b897.jpg"
    }
}
