package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Stone Retrieval Unit
 * {4}
 * Artifact Creature — Construct
 * 2/3
 * When this creature enters, create a tapped Powerstone token.
 */
val StoneRetrievalUnit = card("Stone Retrieval Unit") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreatePowerstone(tapped = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "248"
        artist = "Andreas Zafiratos"
        flavorText = "Automatons could be rebuilt. Powerstones could not."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8a36d1f-f6cc-4962-9ad6-589f7b3e5f95.jpg?1783920013"
    }
}
