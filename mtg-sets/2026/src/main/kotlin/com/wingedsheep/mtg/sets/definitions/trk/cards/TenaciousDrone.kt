package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tenacious Drone
 * {1}{B}
 * Artifact Creature — Borg
 * 2/1
 *
 * When this creature dies, create a 1/1 colorless Borg artifact creature token.
 */
val TenaciousDrone = card("Tenacious Drone") {
    manaCost = "{1}{B}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "B"
    oracleText = "When this creature dies, create a 1/1 colorless Borg artifact creature token."
    power = 2
    toughness = 1
    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1, toughness = 1,
            creatureTypes = setOf("Borg"), artifactToken = true
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Anton Solovianchyk"
        flavorText = "\"They regenerate and keep coming. Eventually you will weaken. Your reserves will be gone. They are relentless.\"\n—Q"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/206ae042-09af-4fc1-8441-69cecdd087de.jpg?1793359485"
    }
}
