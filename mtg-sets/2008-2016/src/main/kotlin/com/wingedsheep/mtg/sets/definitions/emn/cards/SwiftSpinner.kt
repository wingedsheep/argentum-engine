package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Swift Spinner (Eldritch Moon #173)
 * {3}{G}
 * Creature — Spider
 * 2/3
 *
 * Flash
 * Reach
 */
val SwiftSpinner = card("Swift Spinner") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 2
    toughness = 3
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Reach (This creature can block creatures with flying.)"

    keywords(Keyword.FLASH, Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Nils Hamm"
        flavorText = "Its web is woven so tightly that not even geists can avoid capture."
        imageUri = "https://cards.scryfall.io/normal/front/2/6/264f78d8-d554-45f3-867e-19dba38e4644.jpg"
    }
}
