package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Shardless Outlander — Foundations Jumpstart #28
 * {7} · Artifact Creature — Construct Scout · 6/5
 *
 * Trample
 * Basic landcycling {2}
 */
val ShardlessOutlander = card("Shardless Outlander") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct Scout"
    power = 6
    toughness = 5
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, reveal it, " +
        "put it into your hand, then shuffle.)"

    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.basicLandcycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Leon Tukker"
        flavorText = "The Conflux proved that Alara's strength came from unity, not division."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fccb51a4-cb78-4437-b9ab-cc77736af561.jpg?1783908862"
    }
}
