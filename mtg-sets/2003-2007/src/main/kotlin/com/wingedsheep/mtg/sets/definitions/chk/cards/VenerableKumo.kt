package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity

/**
 * Venerable Kumo
 * {4}{G}
 * Creature — Spirit
 * 2/3
 * Reach
 * Soulshift 4 (When this creature dies, you may return target Spirit card with mana value 4 or less
 * from your graveyard to your hand.)
 */
val VenerableKumo = card("Venerable Kumo") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 3
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "Soulshift 4 (When this creature dies, you may return target Spirit card with mana value 4 or less " +
        "from your graveyard to your hand.)"

    keywords(Keyword.REACH)
    soulshift(4)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "248"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/308566ed-18cc-4e3b-b5ab-d5b17795f2f1.jpg?1783944280"
    }
}
