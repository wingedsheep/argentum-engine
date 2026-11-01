package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Davriel's Shadowfugue — War of the Spark #84 (canonical printing)
 * {3}{B}
 * Sorcery
 * Target player discards two cards and loses 2 life.
 */
val DavrielsShadowfugue = card("Davriel's Shadowfugue") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player discards two cards and loses 2 life."

    spell {
        val t = target(Targets.Player)
        effect = Effects.Discard(2, t) then Effects.LoseLife(2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Daarken"
        flavorText = "Shadows coiled around the witness's mind. Then the moment vanished, leaving only the sting of its loss."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09c8fe63-37a7-478d-b9ba-33a0f6519cf1.jpg?1783933451"
    }
}
