package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dragon Bell Monk
 * {2}{W}
 * Creature — Human Monk
 * 2/2
 *
 * Vigilance
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 *
 * Prowess is `prowess()` (keyword plus its +1/+1 trigger), not `keywords(Keyword.PROWESS)`, which
 * would only display the keyword.
 */
val DragonBellMonk = card("Dragon Bell Monk") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    oracleText = "Vigilance\n" +
        "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"
    power = 2
    toughness = 2

    keywords(Keyword.VIGILANCE)
    prowess()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Lius Lasahido"
        flavorText = "He has spent years training his eyes and ears to detect the subtle signs of a dragon's approach."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ed0c9c7-e50d-4bcf-a90e-89cb441de0af.jpg?1783938713"
    }
}
