package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Enormous Baloth
 * {6}{G}
 * Creature — Beast
 * 7/7
 */
val EnormousBaloth = card("Enormous Baloth") {
    manaCost = "{6}{G}"
    typeLine = "Creature — Beast"
    power = 7
    toughness = 7

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Mark Tedin"
        flavorText = "Its diet consists of fruits, plants, small woodland animals, large woodland animals, woodlands, fruit groves, fruit farmers, and small cities."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cebfb5a6-9052-47be-b931-834b5064df31.jpg?1562936577"
    }
}
