package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/** Young Wolf — Dark Ascension #134. */
val YoungWolf = card("Young Wolf") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    oracleText = "Undying (When this creature dies, if it had no +1/+1 counters on it, return it " +
        "to the battlefield under its owner's control with a +1/+1 counter on it.)"
    power = 1
    toughness = 1
    keywords(Keyword.UNDYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Ryan Pancoast"
        flavorText = "The Ulvenwald makes no allowances for youth. Today's newborn is either " +
            "tomorrow's hunter or tomorrow's lunch."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c39aa40-ef5f-40f1-a6dd-fbce91172c50.jpg?1783940798"
    }
}
