package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Enlist

val BenalishFaithbonder = card("Benalish Faithbonder") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "Vigilance\nEnlist (As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature's until end of turn.)"

    keywords(Keyword.VIGILANCE)
    staticAbility { ability = Enlist }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Francisco Miyara"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/084d77df-a899-406f-9d79-e3fa3abeaebc.jpg?1783921370"
        flavorText = "\"A single light can hold back endless darkness.\""
    }
}
