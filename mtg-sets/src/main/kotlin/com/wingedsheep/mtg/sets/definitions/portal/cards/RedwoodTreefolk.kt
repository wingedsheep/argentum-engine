package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Redwood Treefolk
 * {4}{G}
 * Creature — Treefolk
 * 3/6
 * (Vanilla creature)
 */
val RedwoodTreefolk = card("Redwood Treefolk") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Treefolk"
    power = 3
    toughness = 6

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Randy Gallegos"
        flavorText = "The mightiest redwoods outlive whole civilizations."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9399667-ae2a-4b64-84dd-8f97f3e5fe79.jpg"
    }
}
