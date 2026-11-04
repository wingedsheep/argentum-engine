package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val EmergentGrowth = card("Emergent Growth") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +5/+5 until end of turn and must be blocked this turn if able."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(5, 5, creature) then
            Effects.MustBeBlocked(creature, allCreatures = false)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Daniel Ljunggren"
        flavorText = "\"By accepting my smallness I am vast.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e396762-fe76-4b2d-b571-373ab2c240ad.jpg?1783935725"
    }
}
