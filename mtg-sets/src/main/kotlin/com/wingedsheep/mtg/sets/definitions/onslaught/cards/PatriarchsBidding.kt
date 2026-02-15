package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PatriarchsBiddingEffect

/**
 * Patriarch's Bidding
 * {3}{B}{B}
 * Sorcery
 * Each player chooses a creature type. Each player returns all creature cards of a type
 * chosen this way from their graveyard to the battlefield.
 */
val PatriarchsBidding = card("Patriarch's Bidding") {
    manaCost = "{3}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Each player chooses a creature type. Each player returns all creature cards of a type chosen this way from their graveyard to the battlefield."

    spell {
        effect = PatriarchsBiddingEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Carl Critchlow"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/2/d/2deba175-8c02-492d-b404-5d842910c095.jpg?1562905776"
    }
}
