package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EachPlayerMayRevealCreaturesEffect

/**
 * Kamahl's Summons
 * {3}{G}
 * Sorcery
 * Each player may reveal any number of creature cards from their hand. Then each
 * player creates a 2/2 green Bear creature token for each card they revealed this way.
 */
val KamahlsSummons = card("Kamahl's Summons") {
    manaCost = "{3}{G}"
    typeLine = "Sorcery"
    oracleText = "Each player may reveal any number of creature cards from their hand. Then each player creates a 2/2 green Bear creature token for each card they revealed this way."

    spell {
        effect = EachPlayerMayRevealCreaturesEffect(
            tokenPower = 2,
            tokenToughness = 2,
            tokenColors = setOf(Color.GREEN),
            tokenCreatureTypes = setOf("Bear")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "269"
        artist = "Anthony S. Waters"
        flavorText = "As Krosa unleashed the peace in Kamahl, he unleashed the fury in Krosa."
        imageUri = "https://cards.scryfall.io/large/front/0/e/0edc37c6-b6a8-424f-95dd-928d03c28542.jpg?1562897867"
    }
}
