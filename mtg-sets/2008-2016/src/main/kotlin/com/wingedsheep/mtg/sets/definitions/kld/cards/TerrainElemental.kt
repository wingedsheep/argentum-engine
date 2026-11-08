package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Terrain Elemental
 * {1}{G}
 * Creature — Elemental
 * 3/2
 *
 * Vanilla — no rules text.
 */
val TerrainElemental = card("Terrain Elemental") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 2

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "272"
        artist = "Magali Villeneuve"
        flavorText = "\"You tread upon the land all the time, yet you seem dismayed when it moves to step on you.\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32b89e5c-ffb4-406f-99d1-ec2797aca061.jpg?1783937136"
        inBooster = false
    }
}
