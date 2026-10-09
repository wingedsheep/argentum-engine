package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val ShivanDevastator = card("Shivan Devastator") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon Hydra"
    oracleText = "Flying, haste\nThis creature enters with X +1/+1 counters on it."

    power = 0
    toughness = 0
    keywords(Keyword.FLYING, Keyword.HASTE)
    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "143"
        artist = "Brent Hollowell"
        flavorText = "There were many reasons why Shiv was not high on Sheoldred's list of places to conquer: big, fiery reasons."
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f5d274c-3a03-4f0d-97e8-7eef6508105d.jpg?1783921310"
    }
}
