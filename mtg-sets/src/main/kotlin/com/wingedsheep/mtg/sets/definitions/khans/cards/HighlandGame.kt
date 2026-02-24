package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Highland Game
 * {1}{G}
 * Creature — Elk
 * 2/1
 * When Highland Game dies, you gain 2 life.
 */
val HighlandGame = card("Highland Game") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elk"
    power = 2
    toughness = 1
    oracleText = "When this creature dies, you gain 2 life."

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "John Severin Brassell"
        flavorText = "\"Bring down a stag and fix its horns upon her head. This one hears the whispers.\" —Chianul, at the weaving of Arel"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fbb10a9-486a-4b9a-b3f5-c17f661af2b2.jpg?1562789268"
    }
}
