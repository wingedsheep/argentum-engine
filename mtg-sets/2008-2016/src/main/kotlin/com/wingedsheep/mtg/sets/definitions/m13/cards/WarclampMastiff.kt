package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Warclamp Mastiff
 * {W}
 * Creature — Dog
 * 1/1
 * First strike
 */
val WarclampMastiff = card("Warclamp Mastiff") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog"
    oracleText = "First strike (This creature deals combat damage before creatures without first strike.)"
    power = 1
    toughness = 1

    keywords(Keyword.FIRST_STRIKE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "David Palumbo"
        flavorText = "\"One bite from that dog will send you to the healer's tent for days.\"\n—Watchmaster Finn of An Karras"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/102e48e0-8a5f-499d-ac62-005d3c075ef3.jpg"
    }
}
