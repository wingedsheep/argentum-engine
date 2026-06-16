package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

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
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        // "Untap X target lands" — the chosen X clamps the number of land targets via
        // dynamicMaxCount (Icy Blast pattern), so no magic count.
        target = TargetPermanent(
            filter = TargetFilter.Land,
            dynamicMaxCount = DynamicAmount.XValue
        )
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
