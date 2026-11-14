package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Orchard Spirit
 * {2}{G}
 * Creature — Spirit
 * 2/2
 * This creature can't be blocked except by creatures with flying or reach.
 */
val OrchardSpirit = card("Orchard Spirit") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "This creature can't be blocked except by creatures with flying or reach."

    staticAbility {
        ability = CantBeBlockedExceptBy(
            GameObjectFilter.Creature.withKeyword(Keyword.FLYING) or
                GameObjectFilter.Creature.withKeyword(Keyword.REACH)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        artist = "Howard Lyon"
        flavorText = "\"Pick not the rotten fruit, but neither touch the best. Leave those as an offering for our unseen guests.\"\n—Radwick, farmer of Gatstaf"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aac43ced-35b0-4e70-a049-1a65db9b2b1e.jpg?1783940912"
    }
}
