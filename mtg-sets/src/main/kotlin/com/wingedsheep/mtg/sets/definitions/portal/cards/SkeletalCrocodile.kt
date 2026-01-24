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
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07a8b9c0-1d2e-3f4a-5b6c-7d8e9f0a1b2c.jpg"
    }
}
