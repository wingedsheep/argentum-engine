package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val RampagingBaloths = card("Rampaging Baloths") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 6
    toughness = 6
    oracleText = "Trample\nLandfall — Whenever a land you control enters, create a 4/4 green Beast creature token."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast"),
            imageUri = "https://cards.scryfall.io/normal/front/d/f/df60cd9d-44f3-41d3-8ed3-b511bdee3fc6.jpg?1721427709"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "233"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e738d6f6-2da1-4c53-b32f-65e8a1f806a2.jpg?1721973413"
        ruling("2024-11-08", "A landfall ability triggers whenever a land you control enters for any reason. It triggers whenever you play a land, as well as whenever a spell or ability puts a land onto the battlefield under your control.")
        ruling("2024-11-08", "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land.")
    }
}
