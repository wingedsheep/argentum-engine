package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Stormchaser Mage
 * {U}{R}
 * Creature — Human Wizard
 * 1/3
 *
 * Flying, haste
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 */
val StormchaserMage = card("Stormchaser Mage") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Wizard"
    oracleText = "Flying, haste\n" +
        "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"
    power = 1
    toughness = 3

    keywords(Keyword.FLYING, Keyword.HASTE)
    prowess()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Clint Cearley"
        flavorText = "Roilmages' tactics are as chaotic as the energies they channel."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bba1b45-d8ae-42fd-b30f-c904640ab173.jpg?1783937896"
    }
}
