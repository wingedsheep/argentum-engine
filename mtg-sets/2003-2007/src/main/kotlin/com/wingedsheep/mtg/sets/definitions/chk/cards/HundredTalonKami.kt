package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity

/**
 * Hundred-Talon Kami
 * {4}{W}
 * Creature — Spirit
 * 2/3
 * Flying
 * Soulshift 4 (When this creature dies, you may return target Spirit card with mana value 4 or less
 * from your graveyard to your hand.)
 */
val HundredTalonKami = card("Hundred-Talon Kami") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Soulshift 4 (When this creature dies, you may return target Spirit card with mana value 4 or less " +
        "from your graveyard to your hand.)"

    keywords(Keyword.FLYING)
    soulshift(4)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7b2892a-5c16-4624-9a47-6a47f10e2466.jpg?1783944339"
    }
}
