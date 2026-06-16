package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Reconstruction
 * {U}
 * Sorcery
 * Return target artifact card from your graveyard to your hand.
 */
val Reconstruction = card("Reconstruction") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return target artifact card from your graveyard to your hand."

    spell {
        val artifact = target(
            "target artifact card from your graveyard",
            TargetObject(filter = TargetFilter(GameObjectFilter.Artifact.ownedByYou(), zone = Zone.GRAVEYARD))
        )
        effect = Effects.ReturnToHand(artifact)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Anson Maddocks"
        flavorText = "Tedious research made the Sages of the College of Lat-Nam adept in repairing broken artifacts."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1aa2d27b-cc25-4baa-86f4-4db45b30e2a4.jpg?1592254833"
    }
}
