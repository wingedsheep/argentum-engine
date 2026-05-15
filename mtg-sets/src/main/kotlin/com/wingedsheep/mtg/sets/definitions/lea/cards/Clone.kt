package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy

/**
 * Clone
 * {3}{U}
 * Creature — Shapeshifter
 * 0/0
 * You may have Clone enter the battlefield as a copy of any creature on the battlefield.
 */
val Clone = card("Clone") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "You may have Clone enter the battlefield as a copy of any creature on the battlefield."

    replacementEffect(EntersAsCopy(optional = true))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Julie Baroh"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f00d33dd-4eb2-4446-9813-1923d8e2d2f3.jpg?1559591454"
    }
}
