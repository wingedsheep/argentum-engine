package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gates Ablaze — RNA #102
 * {2}{R} Sorcery, Uncommon
 *
 * Gates Ablaze deals X damage to each creature, where X is the number of Gates you control.
 */
val GatesAblaze = card("Gates Ablaze") {
    manaCost = "{2}{R}"
    typeLine = "Sorcery"
    oracleText = "Gates Ablaze deals X damage to each creature, where X is the number of Gates you control."

    spell {
        effect = Patterns.Group.dealDamageToAll(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype(Subtype.GATE)).count(),
            GroupFilter.AllCreatures,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Eytan Zana"
        flavorText = "When the Izzet's spirit of invention extends to the city's infrastructure, " +
            "the results are sometimes explosive."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b574b44-01e1-4197-99bd-57e54aebc5ff.jpg?1783933682"
    }
}
