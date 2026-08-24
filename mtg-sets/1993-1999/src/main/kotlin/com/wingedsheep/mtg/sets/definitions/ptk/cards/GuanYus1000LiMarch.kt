package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Guan Yu's 1,000-Li March
 * {4}{W}{W}
 * Sorcery
 * Destroy all tapped creatures.
 */
val GuanYus1000LiMarch = card("Guan Yu's 1,000-Li March") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy all tapped creatures."

    spell {
        effect = Effects.DestroyAll(Filters.Creature.tapped())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Yang Guangmai"
        flavorText = "\"He [Guan Yu] covered the ground on a thousand-*li* horse; / With dragon blade he took each pass by force.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fa7526a-7a4e-4b3d-b96e-91f2bbf1c7bd.jpg"
    }
}
