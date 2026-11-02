package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Assembly Specialist
 * {1}{W}
 * Artifact Creature — Borg
 * 1/1
 *
 * When this creature enters, create a 1/1 colorless Borg artifact creature token.
 */
val AssemblySpecialist = card("Assembly Specialist") {
    manaCost = "{1}{W}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "W"
    oracleText = "When this creature enters, create a 1/1 colorless Borg artifact creature token."
    power = 1
    toughness = 1
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1, toughness = 1,
            creatureTypes = setOf("Borg"), artifactToken = true
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Sean Vo"
        flavorText = "There is no such thing as a single Borg."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4d339a5-198c-49bd-845e-2632476746e0.jpg?1793100612"
    }
}
