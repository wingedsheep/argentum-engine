package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spined Wurm
 * {4}{G}
 * Creature — Wurm
 * 5/4
 * (Vanilla creature)
 */
val SpinedWurm = card("Spined Wurm") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Wurm"
    power = 5
    toughness = 4

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Richard Kane Ferguson"
        flavorText = "Towering over the treetops, the wurm cares not for the smaller creatures beneath."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa68af36-4fe1-4ca4-821a-4b9f4e2b4a87.jpg"
    }
}
