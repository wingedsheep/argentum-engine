package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Muck Rats
 * {B}
 * Creature — Rat
 * 1/1
 */
val MuckRats = card("Muck Rats") {
    manaCost = "{B}"
    typeLine = "Creature — Rat"
    power = 1
    toughness = 1

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Heather Hudson"
        flavorText = "They scurry through the darkness, leaving only disease in their wake."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4041226-7ce2-46d1-8844-20fa50b6568a.jpg"
    }
}
