package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Winged Coatl
 * {1}{G}{U}
 * Creature — Snake
 * 1 / 1
 *
 * Flash (You may cast this spell any time you could cast an instant.)
 * Flying
 * Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)
 *
 * Three evergreen keywords and no other rules text: [Keyword.FLASH], [Keyword.FLYING] and
 * [Keyword.DEATHTOUCH] go on the card's keyword set, which the builder turns into one `Simple`
 * keyword ability apiece. Flash is read off that set by the casting-timing check, so no
 * alternative-timing plumbing is needed here.
 */
val WingedCoatl = card("Winged Coatl") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Creature — Snake"
    power = 1
    toughness = 1
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Flying\n" +
        "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)"

    keywords(Keyword.FLASH, Keyword.FLYING, Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Izzy"
        flavorText = "The nacatl called this new species \"vetli,\" their word for poison arrows."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd23ae2b-9f69-4d8d-87f9-ebcbccd67342.jpg"
    }
}
