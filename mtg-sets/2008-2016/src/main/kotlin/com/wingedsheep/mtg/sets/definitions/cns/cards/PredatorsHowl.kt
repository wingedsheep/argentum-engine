package com.wingedsheep.mtg.sets.definitions.cns.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/** Morbid checks whether any creature died this turn when the spell resolves. */
val PredatorsHowl = card("Predator's Howl") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Create a 2/2 green Wolf creature token.\nMorbid — Create three 2/2 green Wolf creature tokens instead if a creature died this turn."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.conditional(Conditions.CreatureDiedThisTurn, 3, 1),
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            imageUri = "https://cards.scryfall.io/normal/front/a/4/a4cd989d-e274-4058-876f-3c20f28def0d.jpg?1783939332",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Ralph Horsley"
        flavorText = "\"And Muzzio says my arguments have no teeth.\"\n—Selvala, ranger of the Lowlands"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cae61a9d-39fc-4c25-adec-d578d57c2903.jpg?1783939373"
    }
}
