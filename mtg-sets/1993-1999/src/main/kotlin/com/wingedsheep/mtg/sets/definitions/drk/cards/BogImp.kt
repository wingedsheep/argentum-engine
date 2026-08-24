package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Bog Imp
 * {1}{B}
 * Creature — Imp
 * 1/1
 * Flying (This creature can't be blocked except by creatures with flying or reach.)
 */
val BogImp = card("Bog Imp") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Imp"
    power = 1
    toughness = 1
    keywords(Keyword.FLYING)
    oracleText = "Flying (This creature can't be blocked except by creatures with flying or reach.)"
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Ron Spencer"
        flavorText = "On guard for larger dangers, we underestimated the power and speed of the Imp's muck-crusted claws."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3bb7271-634a-4612-9073-7a5438e8c2b8.jpg"
    }
}
