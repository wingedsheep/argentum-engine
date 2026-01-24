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
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18a9b0c1-2d3e-4f5a-6b7c-8d9e0f1a2b3c.jpg"
    }
}
