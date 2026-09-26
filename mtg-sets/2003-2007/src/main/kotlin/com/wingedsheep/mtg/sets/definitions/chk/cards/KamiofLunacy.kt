package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity

/**
 * Kami of Lunacy
 * {4}{B}{B}
 * Creature — Spirit
 * 4/1
 * Flying
 * Soulshift 5 (When this creature dies, you may return target Spirit card with mana value 5 or less
 * from your graveyard to your hand.)
 */
val KamiofLunacy = card("Kami of Lunacy") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 4
    toughness = 1
    oracleText = "Flying\n" +
        "Soulshift 5 (When this creature dies, you may return target Spirit card with mana value 5 or less " +
        "from your graveyard to your hand.)"

    keywords(Keyword.FLYING)
    soulshift(5)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4baef070-d265-4c6d-9b4b-3cafbd3b34c3.jpg?1783944313"
    }
}
