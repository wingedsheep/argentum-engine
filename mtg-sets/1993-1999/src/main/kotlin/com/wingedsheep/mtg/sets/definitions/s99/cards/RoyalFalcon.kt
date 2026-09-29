package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val RoyalFalcon = card("Royal Falcon") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Bird"
    oracleText = "Flying"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Carl Critchlow"
        flavorText = "Hunter by instinct, weapon by training."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11270682-d751-49e7-9c84-e47bde2f2647.jpg?1783946048"
    }
}
