package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pharika's Chosen
 * {B}
 * Creature — Snake
 * 1/1
 * Deathtouch
 */
val PharikasChosen = card("Pharika's Chosen") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Snake"
    power = 1
    toughness = 1
    oracleText = "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)"

    keywords(Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Jonas De Ro"
        flavorText = "The serpents' dens built into statues of Pharika serve not only to consecrate her temples, " +
            "but as a hidden line of defense."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/8119ce0c-d709-463c-9abc-f7025673ea72.jpg?1783939434"
    }
}
