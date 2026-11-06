package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Servo Schematic
 * {2}
 * Artifact
 * When this artifact enters or is put into a graveyard from the battlefield, create a 1/1
 * colorless Servo artifact creature token.
 */
val ServoSchematic = card("Servo Schematic") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters or is put into a graveyard from the battlefield, create a 1/1 " +
        "colorless Servo artifact creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Titus Lunter"
        flavorText = "The Consulate prizes innovation when it is dependable and replicable."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af2299e0-d31f-4ec4-9497-16e494ee21e6.jpg"
        ruling("2017-02-09", "Servo Schematic's ability doesn't allow you to sacrifice it whenever you'd like. You must find another way to get Servo Schematic into the graveyard.")
    }
}
