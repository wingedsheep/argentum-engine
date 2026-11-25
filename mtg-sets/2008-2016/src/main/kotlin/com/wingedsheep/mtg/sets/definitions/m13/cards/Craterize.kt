package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Craterize
 * {3}{R}
 * Sorcery
 * Destroy target land.
 */
val Craterize = card("Craterize") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target land."

    spell {
        val t = target(TargetFilter.Land)
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Eytan Zana"
        flavorText = "\"'Utterly' is my favorite way to destroy something.\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5459409-5103-4a97-a6fb-3e3ab896eb66.jpg"
    }
}
