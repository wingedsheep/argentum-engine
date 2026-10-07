package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val IcebindPillar = card("Icebind Pillar") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Snow Artifact"
    oracleText = "{S}, {T}: Tap target artifact or creature. ({S} can be paid with one mana from a snow source.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{S}"), Costs.Tap)
        val permanent = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature))
        effect = Effects.Tap(permanent)
        description = "{S}, {T}: Tap target artifact or creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Wisnu Tan"
        flavorText = "\"Oh, Valki. I'm disappointed in you. How could the god of lies be so gullible?\"\n—Tibalt"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3152f1f-5d3f-4bc4-9b6f-f2983ab3f691.jpg?1783928261"
    }
}
