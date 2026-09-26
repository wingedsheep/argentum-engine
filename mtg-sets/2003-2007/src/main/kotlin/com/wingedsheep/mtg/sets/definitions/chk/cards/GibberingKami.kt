package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity

/**
 * Gibbering Kami
 * {3}{B}
 * Creature — Spirit
 * 2/2
 * Flying
 * Soulshift 3 (When this creature dies, you may return target Spirit card with mana value 3 or less
 * from your graveyard to your hand.)
 */
val GibberingKami = card("Gibbering Kami") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Soulshift 3 (When this creature dies, you may return target Spirit card with mana value 3 or less " +
        "from your graveyard to your hand.)"

    keywords(Keyword.FLYING)
    soulshift(3)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Jim Pavelec"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a3e988f-314c-4e83-b279-c2a736933e64.jpg?1783944315"
    }
}
