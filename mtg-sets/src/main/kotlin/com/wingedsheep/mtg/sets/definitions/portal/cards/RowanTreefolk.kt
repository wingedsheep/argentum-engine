package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rowan Treefolk
 * {3}{G}
 * Creature — Treefolk
 * 3/4
 * (Vanilla creature)
 */
val RowanTreefolk = card("Rowan Treefolk") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Treefolk"
    power = 3
    toughness = 4

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Randy Gallegos"
        flavorText = "The rowan tree is said to protect against evil spirits."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fb5a13b-ef46-4b5d-890e-9eb8abe0e7a0.jpg"
    }
}
