package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Skeletal Snake
 * {1}{B}
 * Creature — Snake Skeleton
 * 2/1
 */
val SkeletalSnake = card("Skeletal Snake") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Snake Skeleton"
    power = 2
    toughness = 1

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Heather Hudson"
        flavorText = "Bones slither through the darkness."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42bd4896-4191-4479-be57-070753f8725c.jpg"
    }
}
