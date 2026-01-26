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
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1e76072-e76d-485e-b94c-c29849bc8a6f.jpg"
    }
}
