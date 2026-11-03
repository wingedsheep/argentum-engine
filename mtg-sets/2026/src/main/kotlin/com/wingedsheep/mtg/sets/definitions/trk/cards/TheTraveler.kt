package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * The Traveler
 * {G}{U}
 * Legendary Creature — Scout
 * Star/3
 *
 * Reach, vigilance
 * Strange new worlds — The Traveler's power is equal to the number of differently named lands
 * you control.
 *
 * "Strange new worlds" is an ability word with no rules meaning. Power is a characteristic-defining
 * [dynamicPower] over the TRK count (works in all zones); toughness is a fixed 3.
 */
val TheTraveler = card("The Traveler") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Scout"
    oracleText = "Reach, vigilance\n" +
        "Strange new worlds — The Traveler's power is equal to the number of differently named lands you control."
    toughness = 3

    dynamicPower(DynamicAmounts.differentlyNamedLandsYouControl())

    keywords(Keyword.REACH, Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "254"
        artist = "Ioannis Fiore"
        flavorText = "\"You do understand, don't you, that thought is the basis of all reality. The energy of thought, to put it in your terms, is very powerful.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/7/171932ba-26da-48e9-b8a2-c71aafe5c6a6.jpg?1792508396"
    }
}
