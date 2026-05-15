package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

val EvolvingWilds = card("Evolving Wilds") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}, Sacrifice this land: Search your library for a basic land card, put it onto " +
        "the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = EffectPatterns.searchLibrary(
            filter = Filters.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
        )
        manaAbility = false
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "228"
        artist = "Steven Belledin"
        flavorText = "Every world is an organism, able to grow new lands. Some just do it faster than others."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc7e0407-fea1-43ef-8580-82271e440bb3.jpg?1562708057"
    }
}
