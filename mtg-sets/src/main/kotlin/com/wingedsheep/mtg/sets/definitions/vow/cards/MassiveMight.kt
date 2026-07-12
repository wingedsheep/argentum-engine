package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Massive Might
 * {G}
 * Instant
 * Target creature gets +2/+2 and gains trample until end of turn.
 */
val MassiveMight = card("Massive Might") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 and gains trample until end of turn."
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Composite(
            Effects.ModifyStats(2, 2, t),
            Effects.GrantKeyword(Keyword.TRAMPLE, t)
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "208"
        artist = "Iris Compiet"
        flavorText = "\"Run! It's coming for us! Eventually!\"\n—Yaster, Havengul shopkeeper"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a5cd50b-4825-4d85-b0f9-e2a51d2a7df1.jpg?1782703046"
    }
}
