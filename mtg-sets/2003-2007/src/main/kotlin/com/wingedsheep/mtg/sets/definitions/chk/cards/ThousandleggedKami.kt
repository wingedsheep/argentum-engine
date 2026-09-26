package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity

/**
 * Thousand-legged Kami
 * {6}{G}{G}
 * Creature — Spirit
 * 6/6
 * Soulshift 7 (When this creature dies, you may return target Spirit card with mana value 7 or less
 * from your graveyard to your hand.)
 */
val ThousandleggedKami = card("Thousand-legged Kami") {
    manaCost = "{6}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    power = 6
    toughness = 6
    oracleText = "Soulshift 7 (When this creature dies, you may return target Spirit card with mana value 7 or less " +
        "from your graveyard to your hand.)"

    soulshift(7)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "246"
        artist = "Nottsuo"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c88355e-dff0-4d51-a33c-08e14d6217d4.jpg?1783944281"
    }
}
