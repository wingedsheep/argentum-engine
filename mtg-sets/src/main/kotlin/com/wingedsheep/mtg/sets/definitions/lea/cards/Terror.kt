package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Terror
 * {1}{B}
 * Instant
 * Destroy target nonartifact, nonblack creature. It can't be regenerated.
 */
val Terror = card("Terror") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target nonartifact, nonblack creature. It can't be regenerated."
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.nonartifact().notColor(Color.BLACK)))
        effect = Effects.Destroy(t, noRegenerate = true)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21004958-2c7e-4a55-bc80-411c4d780106.jpg"
    }
}
