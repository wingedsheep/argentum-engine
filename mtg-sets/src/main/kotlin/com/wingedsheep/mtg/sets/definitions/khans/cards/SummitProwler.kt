package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Summit Prowler
 * {2}{R}{R}
 * Creature — Yeti
 * 4/3
 */
val SummitProwler = card("Summit Prowler") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Yeti"
    power = 4
    toughness = 3

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Filip Burburan"
        flavorText = "\"Do you hunt the yetis of the high peaks, stripling? They are as fierce as the bear that fears no foe and as sly as the mink that creeps unseen. You will be much prey as they.\"\n—Nitula, the Hunt Caller"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7f998fb-4518-4a0c-8b1e-55393b6ff9c4.jpg?1562796210"
    }
}
