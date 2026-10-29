package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spined Megalodon
 * {5}{U}{U}
 * Creature — Shark
 * 5/7
 * Hexproof
 * Whenever this creature attacks, scry 1.
 */
val SpinedMegalodon = card("Spined Megalodon") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shark"
    power = 5
    toughness = 7
    oracleText = "Hexproof (This creature can't be the target of spells or abilities your opponents control.)\n" +
        "Whenever this creature attacks, scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    keywords(Keyword.HEXPROOF)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d42d3dd-29df-44a1-89b8-994761eda77d.jpg?1783930718"
    }
}
