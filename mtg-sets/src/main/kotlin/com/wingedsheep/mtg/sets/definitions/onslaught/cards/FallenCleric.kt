package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Fallen Cleric
 * {4}{B}
 * Creature — Zombie Cleric
 * 4/2
 * Protection from Clerics
 * Morph {4}{B}
 */
val FallenCleric = card("Fallen Cleric") {
    manaCost = "{4}{B}"
    typeLine = "Creature — Zombie Cleric"
    power = 4
    toughness = 2
    oracleText = "Protection from Clerics\nMorph {4}{B}"

    keywordAbility(KeywordAbility.ProtectionFromCreatureSubtype("Cleric"))
    morph = "{4}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Dave Dorman"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7652dc61-9170-4895-a0bf-c32a1ee0350e.jpg?1562922981"
    }
}
