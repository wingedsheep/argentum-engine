package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nearheath Stalker
 * {4}{R}
 * Creature — Vampire Rogue
 * 4/1
 * Undying
 */
val NearheathStalker = card("Nearheath Stalker") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Rogue"
    oracleText = "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)"
    power = 4
    toughness = 1
    keywords(Keyword.UNDYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Michael C. Hayes"
        flavorText = "\"Do not speak of this to the elders. They look unfavorably upon my indulgences.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d4cdf4a-2d55-4769-8c51-bc86c13000ef.jpg"
    }
}
