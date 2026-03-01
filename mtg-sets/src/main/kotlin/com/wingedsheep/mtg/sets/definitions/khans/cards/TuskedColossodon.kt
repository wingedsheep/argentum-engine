package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tusked Colossodon
 * {4}{G}{G}
 * Creature — Beast
 * 6/5
 */
val TuskedColossodon = card("Tusked Colossodon") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Beast"
    power = 6
    toughness = 5

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Yeong-Hao Han"
        flavorText = "\"A band of Temur hunters, fleeing the Mardu, dug a hideout beneath such a creature as it slept. The horde found them and attacked. For three days the Temur held them at bay, and all the while the great beast slumbered.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d511407-0c1e-4342-a578-ca557c6886fd.jpg?1562784330"
    }
}
