package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Coast Watcher
 * {1}{U}
 * Creature — Bird Soldier
 * 1/1
 * Flying, protection from green
 */
val CoastWatcher = card("Coast Watcher") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Bird Soldier"
    power = 1
    toughness = 1
    oracleText = "Flying, protection from green"

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.ProtectionFromColor(Color.GREEN))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Luca Zontini"
        flavorText = "The aven came to fear the forest's clawing branches, but the watchers quickly renewed their courage."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bbbc67d-99d0-4277-a8f2-64509e59ec00.jpg?1562530192"
    }
}
