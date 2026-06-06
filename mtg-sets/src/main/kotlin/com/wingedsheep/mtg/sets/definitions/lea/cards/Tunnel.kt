package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Tunnel
 * {R}
 * Instant
 * Destroy target Wall. It can't be regenerated.
 *
 * Oracle errata: the original "Bury target Wall" is now "Destroy … It can't be regenerated."
 */
val Tunnel = card("Tunnel") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Destroy target Wall. It can't be regenerated."
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Wall"))))
        effect = Effects.Destroy(t, noRegenerate = true)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b21ebc9f-a93e-4d18-b3e8-8459e3abbf31.jpg"
    }
}
