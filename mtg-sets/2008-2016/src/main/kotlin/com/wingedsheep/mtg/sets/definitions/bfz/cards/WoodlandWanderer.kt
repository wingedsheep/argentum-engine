package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.convergeEntersWithCounters
import com.wingedsheep.sdk.model.Rarity

/**
 * Woodland Wanderer {3}{G} — Battle for Zendikar #198
 * Creature — Elemental 2/2
 *
 * "Vigilance, trample
 *  Converge — This creature enters with a +1/+1 counter on it for each color of mana spent to cast it."
 *
 * Converge is an ability word: modelled via [convergeEntersWithCounters], i.e.
 * `EntersWithDynamicCounters(DynamicAmount.DistinctColorsManaSpent)`.
 */
val WoodlandWanderer = card("Woodland Wanderer") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 2
    oracleText = "Vigilance, trample\nConverge — This creature enters with a +1/+1 counter on it for each color of mana spent to cast it."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE)

    convergeEntersWithCounters()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Vincent Proce"
        flavorText = "\"The elements understand better than anyone that actions speak louder than words.\"\n—Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc1e6db8-5093-44e4-95d7-3fdf8c55c1dd.jpg?1783938183"
        ruling("2015-08-25", "The maximum number of colors of mana you can spend to cast a spell is five. Colorless is not a color. Note that the cost of a spell with converge may limit how many colors of mana you can spend.")
        ruling("2015-08-25", "If you cast a spell with converge without spending any mana to cast it (perhaps because an effect allowed you to cast it without paying its mana cost), then the number of colors spent to cast it will be zero.")
    }
}
