package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gerrard's Irregulars
 * {4}{R}
 * Creature — Human Soldier
 * 4 / 2
 *
 * Trample, haste
 */
val GerrardsIrregulars = card("Gerrard's Irregulars") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    oracleText = "Trample, haste"
    power = 4
    toughness = 2
    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "192"
        artist = "Eric Peterson"
        flavorText = "Had Gerrard realized he'd end up fighting against them, he might not have trained them so well."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a88f507-3d78-4f7f-a91f-8489ad9250f2.jpg"
    }
}
