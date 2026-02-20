package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Dragonstalker
 * {4}{W}
 * Creature — Bird Soldier
 * 3/3
 * Flying
 * Protection from Dragons
 */
val Dragonstalker = card("Dragonstalker") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 3
    oracleText = "Flying\nProtection from Dragons"

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.ProtectionFromCreatureSubtype("Dragon"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Daren Bader"
        flavorText = "It hunts the hunters of the skies."
        imageUri = "https://cards.scryfall.io/large/front/5/7/57a4c98d-8877-4761-8aa4-b1e210cc8478.jpg?1562529549"
    }
}
