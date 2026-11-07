package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Enlightened Maniac (Eldritch Moon #58)
 * {3}{U}
 * Creature — Human
 * 0/2
 *
 * When this creature enters, create a 3/2 colorless Eldrazi Horror creature token.
 */
val EnlightenedManiac = card("Enlightened Maniac") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human"
    power = 0
    toughness = 2
    oracleText = "When this creature enters, create a 3/2 colorless Eldrazi Horror creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Horror")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Jaime Jones"
        flavorText = "\"At last! Proof that it's not all in my head!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6e692b8-f71d-485b-b37b-71259dc6c48e.jpg"
    }
}
