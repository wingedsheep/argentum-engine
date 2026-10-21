package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Clarion Cathars (Innistrad: Midnight Hunt #14)
 * {3}{W}
 * Creature — Human Knight
 * 3/3
 * When this creature enters, create a 1/1 white Human creature token.
 */
val ClarionCathars = card("Clarion Cathars") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    oracleText = "When this creature enters, create a 1/1 white Human creature token."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human"),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Leanna Crossan"
        flavorText = "The sound of the trumpet is both warning and comfort: it tells of approaching danger but also reminds the village folk that someone is there to stand against it."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89f8ea28-7de1-4908-8e2d-b1aeff651015.jpg"
    }
}
