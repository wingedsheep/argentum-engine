package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Conscripted Infantry
 * {2}{R}
 * Creature — Human Soldier
 * 3/1
 * When this creature dies, create a 1/1 colorless Soldier artifact creature token.
 */
val ConscriptedInfantry = card("Conscripted Infantry") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 1
    oracleText = "When this creature dies, create a 1/1 colorless Soldier artifact creature token."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Soldier"),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Noah Thatcher"
        flavorText = "\"Funny,\" remarked Laga to his squad. \"Mishra's lackeys wear luxurious silks in their gilded palaces, while we toil in the trenches eating stale bread.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f50b0449-8ff7-4549-893b-aeca93720c64.jpg"
    }
}
