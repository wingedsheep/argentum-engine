package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Enlist

val CoalitionSkyknight = card("Coalition Skyknight") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Flying\nEnlist (As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature's until end of turn.)"

    keywords(Keyword.FLYING)
    staticAbility { ability = Enlist }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b12da45-aff4-406d-83a3-39ecaff6ff56.jpg?1783921368"
    }
}
