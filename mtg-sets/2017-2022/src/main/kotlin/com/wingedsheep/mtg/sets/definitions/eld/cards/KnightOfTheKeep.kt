package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Knight of the Keep
 * {2}{W}
 * Creature — Human Knight
 * 3/2
 * (Vanilla)
 *
 * Canonical earliest real printing is Throne of Eldraine (ELD, 2019); reprinted in
 * The Lord of the Rings: Tales of Middle-earth (LTR) — see KnightOfTheKeepReprint in the LTR package.
 */
val KnightOfTheKeep = card("Knight of the Keep") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 2
    oracleText = ""

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Bram Sels"
        flavorText = "\"What good is loyalty to the crown if we are not also in service to those less fortunate? I fight for all of us.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d49653d-cd4e-40a7-99de-fc531b5d8594.jpg?1783932670"
    }
}
