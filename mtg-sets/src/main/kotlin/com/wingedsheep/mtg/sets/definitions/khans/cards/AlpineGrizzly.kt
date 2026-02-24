package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Alpine Grizzly
 * {2}{G}
 * Creature — Bear
 * 4/2
 */
val AlpineGrizzly = card("Alpine Grizzly") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Bear"
    power = 4
    toughness = 2

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "John Severin Brassell"
        flavorText = "The Temur welcome bears into the clan, fighting alongside them in battle. The relationship dates back to when they labored side by side under Sultai rule."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38bbf983-df71-4403-86f3-2e86aa8765b8.jpg?1562784926"
    }
}
