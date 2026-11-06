package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cogworker's Puzzleknot
 * {2}
 * Artifact
 * When this artifact enters, create a 1/1 colorless Servo artifact creature token.
 * {1}{W}, Sacrifice this artifact: Create a 1/1 colorless Servo artifact creature token.
 */
val CogworkersPuzzleknot = card("Cogworker's Puzzleknot") {
    manaCost = "{2}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, create a 1/1 colorless Servo artifact creature token.\n" +
        "{1}{W}, Sacrifice this artifact: Create a 1/1 colorless Servo artifact creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true,
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.SacrificeSelf)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00d594df-c51b-4936-9af1-536dab1792ae.jpg?1783937162"
    }
}
