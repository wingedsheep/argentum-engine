package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Shatterstorm
 * {2}{R}{R}
 * Sorcery
 * Destroy all artifacts. They can't be regenerated.
 */
val Shatterstorm = card("Shatterstorm") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy all artifacts. They can't be regenerated."

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Artifact, noRegenerate = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Mark Poole"
        flavorText = "Chains of leaping fire and sizzling lightning laid waste the artificers' handiwork, sparing not a single device."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0987461a-45c0-4956-8627-cd27a7e038d0.jpg?1562897000"
    }
}
