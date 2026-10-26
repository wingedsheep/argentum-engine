package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Harvest Season
 * {2}{G}
 * Sorcery
 * Search your library for up to X basic land cards, where X is the number of tapped creatures you
 * control, put those cards onto the battlefield tapped, then shuffle.
 */
val HarvestSeason = card("Harvest Season") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to X basic land cards, where X is the number of tapped " +
        "creatures you control, put those cards onto the battlefield tapped, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.tapped()).count(),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Shreya Shetty"
        flavorText = "A true reflection of nature: death fostering life."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb18e745-a925-477f-a50f-3ec7fba22d88.jpg?1783936475"
    }
}
