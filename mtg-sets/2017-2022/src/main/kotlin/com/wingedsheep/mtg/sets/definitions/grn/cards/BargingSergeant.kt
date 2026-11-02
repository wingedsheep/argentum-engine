package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity

/**
 * Barging Sergeant
 * {4}{R}
 * Creature — Minotaur Soldier
 * 4/2
 *
 * Haste
 * Mentor
 */
val BargingSergeant = card("Barging Sergeant") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Minotaur Soldier"
    power = 4
    toughness = 2
    oracleText = "Haste\n" +
        "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)"

    keywords(Keyword.HASTE)
    mentor()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Filip Burburan"
        flavorText = "\"Don't stop till your horns come out their back.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52a54ebc-008a-4180-a023-2e1d5318780c.jpg?1783934166"
    }
}
