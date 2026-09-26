package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity

/**
 * Vine Kami
 * {6}{G}
 * Creature — Spirit
 * 4/4
 * Menace
 * Soulshift 6 (When this creature dies, you may return target Spirit card with mana value 6 or less
 * from your graveyard to your hand.)
 *
 * Oracle errata: printed as "can't be blocked except by two or more creatures", now menace.
 */
val VineKami = card("Vine Kami") {
    manaCost = "{6}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    power = 4
    toughness = 4
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Soulshift 6 (When this creature dies, you may return target Spirit card with mana value 6 or less " +
        "from your graveyard to your hand.)"

    keywords(Keyword.MENACE)
    soulshift(6)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "249"
        artist = "Tsutomu Kawade"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa5241c8-7f50-413e-9ac0-9ab0ad5c884c.jpg?1783944280"
    }
}
