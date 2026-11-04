package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Awakened Amalgam
 * {4}
 * Artifact Creature — Golem
 * * / *
 * Awakened Amalgam's power and toughness are each equal to the number of differently named lands
 * you control.
 *
 * Characteristic-defining ability (CR 604.3) over the distinct-names battlefield aggregation that
 * Survey Mechan and Audience with Trostani already read.
 */
val AwakenedAmalgam = card("Awakened Amalgam") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Golem"
    oracleText = "Awakened Amalgam's power and toughness are each equal to the number of differently named lands you control."

    dynamicStats(DynamicAmounts.differentlyNamedLandsYouControl())

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Zack Stella"
        flavorText = "\"The fools have done it.\"\n—Tishana"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d24b14ea-04f0-4d4b-970e-efc65d64945e.jpg?1783935268"
        ruling("2018-01-19", "The ability that defines Awakened Amalgam's power and toughness works in all zones, not just the battlefield.")
        ruling("2018-01-19", "To determine the number of differently named lands you control, count each land you control once, but only if its English name isn't exactly the same as another land you've already counted this way. For example, if you control four lands named Plains, two named Island, and one named Drowned Catacomb, Awakened Amalgam is a 3/3 creature.")
    }
}
