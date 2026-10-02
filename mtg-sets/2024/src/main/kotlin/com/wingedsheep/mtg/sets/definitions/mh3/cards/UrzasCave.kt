package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Urza's Cave — Modern Horizons 3 #234.
 * Land — Urza's Cave
 * {T}: Add {C}.
 * {3}, {T}, Sacrifice this land: Search your library for a land card, put it onto the battlefield
 * tapped, then shuffle.
 */
val UrzasCave = card("Urza's Cave") {
    typeLine = "Land — Urza's Cave"
    oracleText = "{T}: Add {C}.\n" +
        "{3}, {T}, Sacrifice this land: Search your library for a land card, put it onto the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land,
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Mark Poole"
        flavorText = "No matter how deep Urza delved, he couldn't escape his regrets."
        imageUri = "https://cards.scryfall.io/normal/front/9/2/926916ed-2f22-4ba9-9427-194886ad6c1e.jpg?1783911233"
    }
}
