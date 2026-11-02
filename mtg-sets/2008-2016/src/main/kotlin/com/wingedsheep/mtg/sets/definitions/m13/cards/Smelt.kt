package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Smelt
 * {R}
 * Instant
 * Destroy target artifact.
 */
val Smelt = card("Smelt") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Destroy target artifact."

    spell {
        val t = target(TargetFilter.Artifact)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Zoltan Boros"
        flavorText = "\"Looks like that wasn't darksteel after all.\"\n—Koth of the Hammer"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/723cb7e3-3f48-41fa-aa08-bdc59225e44f.jpg?1783940478"
    }
}
