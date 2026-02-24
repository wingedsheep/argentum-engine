package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Shoreline Ranger
 * {5}{U}
 * Creature — Bird Soldier
 * 3/4
 * Flying
 * Islandcycling {2} ({2}, Discard this card: Search your library for an Island card,
 * reveal it, put it into your hand, then shuffle.)
 */
val ShorelineRanger = card("Shoreline Ranger") {
    manaCost = "{5}{U}"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 4
    oracleText = "Flying\nIslandcycling {2}"

    keywords(Keyword.FLYING)

    keywordAbility(KeywordAbility.Typecycling("Island", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Glen Angus"
        flavorText = "It waits for the weather to change before changing its tactics."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eed813c4-fff0-43f1-bc62-cbc3a126d600.jpg?1562536698"
    }
}
