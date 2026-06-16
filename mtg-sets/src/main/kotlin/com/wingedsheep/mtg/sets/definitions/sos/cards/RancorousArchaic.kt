package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.convergeEntersWithCounters
import com.wingedsheep.sdk.model.Rarity

/**
 * Rancorous Archaic
 * {5}
 * Creature — Avatar
 * 2/2
 *
 * Trample, reach
 * Converge — This creature enters with a +1/+1 counter on it for each color of mana spent to cast it.
 *
 * The mana cost is all generic, so the colour count is entirely up to how you pay it: cast for five
 * colourless and it enters with no counters; pay the {5} with mana of five different colours and it
 * enters 7/7. Modelled via [convergeEntersWithCounters], i.e.
 * `EntersWithDynamicCounters(DynamicAmount.DistinctColorsManaSpent)`.
 */
val RancorousArchaic = card("Rancorous Archaic") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Creature — Avatar"
    power = 2
    toughness = 2
    oracleText = "Trample, reach\n" +
        "Converge — This creature enters with a +1/+1 counter on it for each color of mana spent to cast it."

    keywords(Keyword.TRAMPLE, Keyword.REACH)

    convergeEntersWithCounters()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Loïc Canavaggia"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2565e16a-ed31-4867-adb8-f1633d580397.jpg?1775936927"
    }
}
