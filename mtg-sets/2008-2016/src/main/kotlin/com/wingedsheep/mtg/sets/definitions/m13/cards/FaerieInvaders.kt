package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Faerie Invaders
 * {4}{U}
 * Creature — Faerie Rogue
 * 3/3
 * Flash
 * Flying
 */
val FaerieInvaders = card("Faerie Invaders") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Rogue"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Flying"
    power = 3
    toughness = 3

    keywords(Keyword.FLASH, Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Ryan Pancoast"
        flavorText = "Small enough to penetrate the narrowest crack in a castle wall and numerous enough to hack apart a griffin."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcbc71b3-544b-4b81-8922-52744892989b.jpg"
    }
}
