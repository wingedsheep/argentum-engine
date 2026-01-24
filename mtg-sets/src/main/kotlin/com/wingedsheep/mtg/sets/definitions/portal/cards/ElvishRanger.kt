package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Elvish Ranger
 * {2}{G}
 * Creature — Elf Ranger
 * 4/1
 */
val ElvishRanger = card("Elvish Ranger") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elf Ranger"
    power = 4
    toughness = 1

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "Randy Gallegos"
        flavorText = "Swift and sure, the ranger finds her mark."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03b67c92-7a67-4b14-b24f-e51e7b8e7fc0.jpg"
    }
}
