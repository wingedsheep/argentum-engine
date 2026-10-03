package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Oashra Cultivator
 * {G}
 * Creature — Human Druid
 * 0/3
 * {2}{G}, {T}, Sacrifice this creature: Search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle.
 *
 * Embodiment of Spring's ability at a different mana cost: [Patterns.Library.searchLibrary] for a
 * basic land onto the battlefield tapped, paid for with mana, tap and self-sacrifice.
 */
val OashraCultivator = card("Oashra Cultivator") {
    manaCost = "{G}"
    typeLine = "Creature — Human Druid"
    power = 0
    toughness = 3
    oracleText = "{2}{G}, {T}, Sacrifice this creature: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = Filters.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "Sara Winters"
        flavorText = "\"Like fruits in the field, we will be harvested when the season is right.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d10be36-8e57-4b08-bc3b-e69769e0908a.jpg?1783936471"
    }
}
