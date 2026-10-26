package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tezzeret's Gambit
 * {3}{U/P}
 * Sorcery
 * ({U/P} can be paid with either {U} or 2 life.)
 * Draw two cards, then proliferate.
 */
val TezzeretsGambit = card("Tezzeret's Gambit") {
    manaCost = "{3}{U/P}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "({U/P} can be paid with either {U} or 2 life.)\n" +
        "Draw two cards, then proliferate. (Choose any number of permanents and/or players, then give each " +
        "another counter of each kind already there.)"

    spell {
        effect = Effects.DrawCards(2) then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/fff5a09e-9276-44b8-b374-4b84aebd47cc.jpg?1783941318"
        ruling(
            "2011-06-01",
            "To calculate the mana value of a card with Phyrexian mana symbols in its cost, count each " +
                "Phyrexian mana symbol as 1."
        )
        ruling("2011-06-01", "If you're at 1 life or less, you can't pay 2 life.")
    }
}
