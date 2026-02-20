package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.costs.PayCost

/**
 * Zombie Cutthroat
 * {3}{B}{B}
 * Creature — Zombie
 * 3/4
 * Morph—Pay 5 life. (You may cast this card face down as a 2/2 creature for {3}.
 * Turn it face up any time for its morph cost.)
 */
val ZombieCutthroat = card("Zombie Cutthroat") {
    manaCost = "{3}{B}{B}"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 4
    oracleText = "Morph—Pay 5 life."

    morphCost = PayCost.PayLife(5)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Thomas M. Baxa"
        flavorText = "The single-mindedness of a zombie, the cunning of an assassin."
        imageUri = "https://cards.scryfall.io/large/front/f/e/fe491cba-6ec7-4c44-ad1e-832d936986a0.jpg?1562537392"
    }
}
