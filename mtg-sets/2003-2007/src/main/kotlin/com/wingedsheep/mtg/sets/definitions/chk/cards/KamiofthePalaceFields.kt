package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity

/**
 * Kami of the Palace Fields
 * {5}{W}
 * Creature — Spirit
 * 3/2
 * Flying, first strike
 * Soulshift 5 (When this creature dies, you may return target Spirit card with mana value 5 or less
 * from your graveyard to your hand.)
 */
val KamiofthePalaceFields = card("Kami of the Palace Fields") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 2
    oracleText = "Flying, first strike\n" +
        "Soulshift 5 (When this creature dies, you may return target Spirit card with mana value 5 or less " +
        "from your graveyard to your hand.)"

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)
    soulshift(5)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "24"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/690980ce-bbdc-4d52-b34e-2bad11e436a1.jpg?1783944337"
    }
}
