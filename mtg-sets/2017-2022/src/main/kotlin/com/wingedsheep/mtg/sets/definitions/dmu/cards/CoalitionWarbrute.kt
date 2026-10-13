package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Enlist

val CoalitionWarbrute = card("Coalition Warbrute") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Minotaur Berserker"
    power = 3
    toughness = 4
    oracleText = "Enlist (As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature's until end of turn.)\nTrample"

    staticAbility { ability = Enlist }
    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Will Gist"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/554c422f-2a1b-47c0-9358-42b85cdbb84e.jpg?1783921323"
        flavorText = "Anger is his greatest weapon, and he sharpens it daily."
    }
}
