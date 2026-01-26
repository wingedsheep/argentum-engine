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
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0053bd00-90fd-48c2-8f79-952d5d1e3e74.jpg"
    }
}
