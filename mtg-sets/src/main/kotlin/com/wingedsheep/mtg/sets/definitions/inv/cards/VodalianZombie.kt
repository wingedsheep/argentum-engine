package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * Vodalian Zombie
 * {U}{B}
 * Creature — Merfolk Zombie
 * 2/2
 * Protection from green
 */
val VodalianZombie = card("Vodalian Zombie") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Merfolk Zombie"
    power = 2
    toughness = 2
    oracleText = "Protection from green"

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.GREEN)))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "286"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        flavorText = "\"Every last one of you will become my servant. It's a shame you won't live to see the irony.\"\n—Tsabo Tavoc, Phyrexian general"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f30a5a06-32ce-4d71-b71f-e3e1d8d4511a.jpg?1562943854"
    }
}
