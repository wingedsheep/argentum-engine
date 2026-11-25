package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ajani's Sunstriker
 * {W}{W}
 * Creature — Cat Cleric
 * 2/2
 * Lifelink
 */
val AjanisSunstriker = card("Ajani's Sunstriker") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Cleric"
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)"
    power = 2
    toughness = 2

    keywords(Keyword.LIFELINK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Matt Stewart"
        flavorText = "\"Ajani goes where he is needed most and in his name we keep his lands safe.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3570c4d9-cd42-4aca-9421-ac44e057a785.jpg"
    }
}
