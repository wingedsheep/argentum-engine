package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bog Smugglers
 * {1}{B}{B}
 * Creature — Human Mercenary
 * 2 / 2
 */
val BogSmugglers = card("Bog Smugglers") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Mercenary"
    oracleText = "Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)"
    power = 2
    toughness = 2

    keywords(Keyword.SWAMPWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Mike Ploog"
        flavorText = "They slide over the bog like oil across glass."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2103a44-87e5-40cd-a0de-cd19456a8366.jpg"
    }
}
