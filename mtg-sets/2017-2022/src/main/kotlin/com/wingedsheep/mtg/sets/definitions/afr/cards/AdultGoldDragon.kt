package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Adult Gold Dragon
 * {3}{R}{W}
 * Creature — Dragon
 * 4/3
 * Flying, lifelink, haste
 */
val AdultGoldDragon = card("Adult Gold Dragon") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Dragon"
    oracleText = "Flying, lifelink, haste"
    power = 4
    toughness = 3
    keywords(Keyword.FLYING, Keyword.LIFELINK, Keyword.HASTE)
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "216"
        artist = "Chris Rahn"
        flavorText = "Gold dragons are the greatest of the metallic dragons in size, power, and wisdom, and often take it upon themselves to safeguard mortal communities from evils that might threaten them."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67a76010-d932-4727-8b5e-b8e2d14e1362.jpg?1783926450"
    }
}
