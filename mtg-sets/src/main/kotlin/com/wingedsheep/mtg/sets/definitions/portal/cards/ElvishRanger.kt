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
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26caff65-3a96-46f2-8f0b-e5091b632a2e.jpg"
    }
}
