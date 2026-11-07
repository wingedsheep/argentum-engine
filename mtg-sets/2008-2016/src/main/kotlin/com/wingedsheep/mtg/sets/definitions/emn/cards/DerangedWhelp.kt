package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Deranged Whelp (Eldritch Moon #125)
 * {1}{R}
 * Creature — Wolf
 * 2/1
 *
 * Menace (This creature can't be blocked except by two or more creatures.)
 */
val DerangedWhelp = card("Deranged Whelp") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolf"
    power = 2
    toughness = 1
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)"

    keywords(Keyword.MENACE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Zoltan Boros"
        flavorText = "\"When you confront a wolf scorned by its pack, extra caution is the order of the day.\"\n" +
            "—Raf Gyel of the Quiver of Kessig"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd06f9eb-112e-458b-83c8-3761df6d60ff.jpg"
    }
}
