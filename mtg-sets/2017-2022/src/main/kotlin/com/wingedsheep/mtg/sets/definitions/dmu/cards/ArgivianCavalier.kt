package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Enlist

val ArgivianCavalier = card("Argivian Cavalier") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Orc Knight"
    power = 2
    toughness = 2
    oracleText = "Enlist (As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature's until end of turn.)\nWhen this creature enters, create a 1/1 white Soldier creature token."

    staticAbility { ability = Enlist }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57746e72-ba9f-4717-a4a2-6c6a039080c3.jpg?1783921372"
    }
}
