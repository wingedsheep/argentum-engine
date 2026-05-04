package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Keyword

/**
 * Hullcarver
 * {B}
 * Artifact Creature — Robot Assassin
 * Deathtouch
 */
val Hullcarver = card("Hullcarver") {
    manaCost = "{B}"
    typeLine = "Artifact Creature — Robot Assassin"
    power = 1
    toughness = 1
    oracleText = "Deathtouch"

    // Deathtouch keyword
    keywords(Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Michal Ivan"
        flavorText = "\"Sunstar thinking is too linear. While they fight head-on, we hit them everywhere else.\"\n—Alpharael, Stonechosen"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/817b5b18-beb5-48c8-aa45-0515ff9ca5da.jpg?1752946978"
    }
}
