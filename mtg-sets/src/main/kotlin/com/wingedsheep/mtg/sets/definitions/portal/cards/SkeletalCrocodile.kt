package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Skeletal Crocodile
 * {3}{B}
 * Creature — Crocodile Skeleton
 * 5/1
 */
val SkeletalCrocodile = card("Skeletal Crocodile") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Crocodile Skeleton"
    power = 5
    toughness = 1

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Heather Hudson"
        flavorText = "Animated bones given a hunger for living flesh."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebcbbd6f-2915-4b4c-85d3-1d9b55d36c11.jpg"
    }
}
