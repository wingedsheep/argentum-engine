package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Weathered Wayfarer
 * {W}
 * Creature — Human Nomad Cleric
 * 1/1
 * {W}, {T}: Search your library for a land card, reveal it, put it into your hand, then shuffle.
 * Activate only if an opponent controls more lands than you.
 */
val WeatheredWayfarer = card("Weathered Wayfarer") {
    manaCost = "{W}"
    typeLine = "Creature — Human Nomad Cleric"
    power = 1
    toughness = 1
    oracleText = "{W}, {T}: Search your library for a land card, reveal it, put it into your hand, then shuffle. Activate only if an opponent controls more lands than you."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        effect = Effects.SearchLibrary(
            filter = Filters.Land,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true
        )
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.OpponentControlsMoreLands)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6601ab1-3862-4aff-82be-be15493fe4b0.jpg?1562953394"
    }
}
