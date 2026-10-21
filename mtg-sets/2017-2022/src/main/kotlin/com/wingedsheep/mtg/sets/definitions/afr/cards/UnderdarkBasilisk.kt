package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Underdark Basilisk
 * {1}{G}
 * Creature — Basilisk
 * 1/2
 * Deathtouch
 */
val UnderdarkBasilisk = card("Underdark Basilisk") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Basilisk"
    oracleText = "Deathtouch"
    power = 1
    toughness = 2
    keywords(Keyword.DEATHTOUCH)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "208"
        artist = "Brent Hollowell"
        flavorText = "\"See how the statues all have chunks bitten out of them? We're dealing with a basilisk, or I'm a froghemoth.\"\n—Larissa Kyriln, learned warlock"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed220158-e4e3-4d46-8098-7b940a923ce9.jpg?1783926453"
    }
}
