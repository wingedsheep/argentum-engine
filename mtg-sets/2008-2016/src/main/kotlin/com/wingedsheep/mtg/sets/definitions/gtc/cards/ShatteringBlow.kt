package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shattering Blow
 * {1}{R/W}
 * Instant
 * Exile target artifact.
 */
val ShatteringBlow = card("Shattering Blow") {
    manaCost = "{1}{R/W}"
    colorIdentity = "WR"
    typeLine = "Instant"
    oracleText = "Exile target artifact."

    spell {
        val artifact = target(TargetFilter.Artifact)
        effect = Effects.Exile(artifact)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "225"
        artist = "Steve Prescott"
        flavorText = "\"Aurelia seems too eager to solve her problems by delivering them to oblivion.\"\n—Prime Speaker Zegana"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a77058d9-d2b5-424a-bfe2-070b754051cb.jpg"
    }
}
