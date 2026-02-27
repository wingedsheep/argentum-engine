package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aven Envoy
 * {U}
 * Creature — Bird Soldier
 * 0/2
 * Flying
 */
val AvenEnvoy = card("Aven Envoy") {
    manaCost = "{U}"
    typeLine = "Creature — Bird Soldier"
    power = 0
    toughness = 2
    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Alex Horley-Orlandelli"
        flavorText = "\"Their grieving faces tell far more than a list of casualties ever could.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40ead30e-9f96-4fca-b619-fdc8d1b5e2e0.jpg?1562907998"
    }
}
