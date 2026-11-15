package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pilgrim of the Fires
 * {7}
 * Artifact Creature — Golem
 * 6/4
 * First strike, trample
 */
val PilgrimOfTheFires = card("Pilgrim of the Fires") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    oracleText = "First strike, trample"
    power = 6
    toughness = 4

    keywords(Keyword.FIRST_STRIKE, Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Izzy"
        flavorText = "The most skilled Jeskai artisans can craft imposing golems with such precision that they will last an eternity—the time believed necessary to master the Fires of the Jeskai Way."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45a60623-d523-4b43-89b0-8e89568546c7.jpg"
    }
}
