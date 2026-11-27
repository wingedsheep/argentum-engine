package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Strangleroot Geist
 * {G}{G}
 * Creature — Spirit
 * 2/1
 * Haste
 * Undying
 */
val StranglerootGeist = card("Strangleroot Geist") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    oracleText = "Haste\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)"
    power = 2
    toughness = 1
    keywords(Keyword.HASTE, Keyword.UNDYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Jason Chan"
        flavorText = "Geists of the hanged often haunt the tree on which they died."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf1fb137-205c-480f-b6dc-dfa137793ae3.jpg"
    }
}
