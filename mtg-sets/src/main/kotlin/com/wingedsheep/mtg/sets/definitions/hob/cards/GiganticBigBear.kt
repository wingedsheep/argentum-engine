package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gigantic Big Bear
 * {5}{G}{G}
 * Creature — Bear
 * 10/7
 * This spell can't be countered.
 * Hexproof, haste
 *
 * "This spell can't be countered" is a characteristic of the card while it's on the stack, not a
 * battlefield static ability — `cantBeCountered` on the definition, not a `staticAbility`.
 */
val GiganticBigBear = card("Gigantic Big Bear") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bear"
    oracleText = "This spell can't be countered.\nHexproof, haste"
    power = 10
    toughness = 7

    cantBeCountered = true

    keywords(Keyword.HEXPROOF, Keyword.HASTE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Xabi Gaztelua"
        flavorText = "Wargs and Goblins had few predators in the foothills of the Misty Mountains, but they did have some."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d6ece3d-8e7a-41ad-974f-3c9748de4825.jpg?1785323269"
    }
}
