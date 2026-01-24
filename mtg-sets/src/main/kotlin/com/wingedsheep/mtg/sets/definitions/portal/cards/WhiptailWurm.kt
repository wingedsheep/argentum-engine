package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Whiptail Wurm
 * {6}{G}
 * Creature — Wurm
 * 8/5
 * (Vanilla creature)
 */
val WhiptailWurm = card("Whiptail Wurm") {
    manaCost = "{6}{G}"
    typeLine = "Creature — Wurm"
    power = 8
    toughness = 5

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Roger Raupp"
        flavorText = "Its tail strikes with devastating force."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5f0b7dc-5a12-4d5b-a7e2-c6d0d9c4e3f5.jpg"
    }
}
