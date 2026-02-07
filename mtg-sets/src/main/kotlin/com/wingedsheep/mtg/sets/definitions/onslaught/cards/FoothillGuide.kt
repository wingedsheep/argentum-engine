package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Foothill Guide
 * {W}
 * Creature — Human Cleric Soldier
 * 1/1
 * Protection from Goblins
 * Morph {W}
 */
val FoothillGuide = card("Foothill Guide") {
    manaCost = "{W}"
    typeLine = "Creature — Human Cleric Soldier"
    power = 1
    toughness = 1

    keywordAbility(KeywordAbility.ProtectionFromCreatureSubtype("Goblin"))
    morph = "{W}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/large/front/c/d/cd6cc30a-9ed4-4f36-95cb-6f0a2b8dce02.jpg?1562943472"
    }
}
