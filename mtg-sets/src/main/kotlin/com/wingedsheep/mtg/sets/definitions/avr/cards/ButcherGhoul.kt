package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/** Butcher Ghoul — Avacyn Restored #89. */
val ButcherGhoul = card("Butcher Ghoul") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "Undying (When this creature dies, if it had no +1/+1 counters on it, return it " +
        "to the battlefield under its owner's control with a +1/+1 counter on it.)"
    power = 1
    toughness = 1
    keywords(Keyword.UNDYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Christopher Moeller"
        flavorText = "Without a mind, it doesn't fear death. Without a soul, it doesn't mind killing."
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44a91e62-e946-4101-8cef-d1c147caebf2.jpg?1783940705"
    }
}
