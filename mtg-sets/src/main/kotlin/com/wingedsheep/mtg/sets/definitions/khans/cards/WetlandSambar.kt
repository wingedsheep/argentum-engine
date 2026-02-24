package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wetland Sambar
 * {1}{U}
 * Creature — Elk
 * 2/1
 */
val WetlandSambar = card("Wetland Sambar") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Elk"
    power = 2
    toughness = 1

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "James Zapata"
        flavorText = "As a test of calm and compassion, a Jeskai monk softly approaches a grazing sambar and offers it a lotus from his or her hand. If the creature eats, the student ascends to the next level of training."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f71a86e0-d15a-4fba-94f6-bfbaade8d837.jpg?1562796153"
    }
}
