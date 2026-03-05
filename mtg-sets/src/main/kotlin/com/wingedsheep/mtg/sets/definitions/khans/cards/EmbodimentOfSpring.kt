package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Embodiment of Spring
 * {U}
 * Creature — Elemental
 * 0/3
 * {1}{G}, {T}, Sacrifice this creature: Search your library for a basic land card,
 * put it onto the battlefield tapped, then shuffle.
 */
val EmbodimentOfSpring = card("Embodiment of Spring") {
    manaCost = "{U}"
    typeLine = "Creature — Elemental"
    power = 0
    toughness = 3
    oracleText = "{1}{G}, {T}, Sacrifice this creature: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.SearchLibrary(
            filter = Filters.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Wayne Reynolds"
        flavorText = "\"Arel dreamed of winter's end. The next morning she followed a strange trail and found a seedling in the snow.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88e07226-f180-4972-b7f8-90c743d45fb8.jpg?1562789879"
    }
}
