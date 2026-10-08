package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Candelabra of Tawnos
 * {1}
 * Artifact
 * {X}, {T}: Untap X target lands.
 */
val CandelabraOfTawnos = card("Candelabra of Tawnos") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{X}, {T}: Untap X target lands."

    activatedAbility {
        targets(TargetFilter.Land, exactly = DynamicAmounts.xValue())
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        // "Untap X target lands" — exactly X land targets (Icy Blast pattern); X = 0 targets none.
        effect = Effects.UntapEachTarget()
        description = "{X}, {T}: Untap X target lands."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Douglas Shuler"
        flavorText = "Tawnos learned quickly from Urza that utter simplicity often led to wondrous, yet subtle utility."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35a335bf-7358-460f-b7c9-1e8bc4300f64.jpg?1562906316"
    }
}
