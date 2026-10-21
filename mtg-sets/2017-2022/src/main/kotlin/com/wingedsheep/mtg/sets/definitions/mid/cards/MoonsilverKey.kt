package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Moonsilver Key
 * {2}
 * Artifact
 * {1}, {T}, Sacrifice this artifact: Search your library for an artifact card with a mana ability
 * or a basic land card, reveal it, put it into your hand, then shuffle.
 */
val MoonsilverKey = card("Moonsilver Key") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Search your library for an artifact card with a mana ability or a basic land card, reveal it, put it into your hand, then shuffle."
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.withManaAbility() or GameObjectFilter.BasicLand,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "255"
        artist = "Joseph Meehan"
        flavorText = "To restore the day-night balance, the Celestus mechanism's Sungold Lock must be reunited with its long-lost key."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87778e37-af92-402e-b037-5fbd6112b682.jpg?1783925547"
    }
}
