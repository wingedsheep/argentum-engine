package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

val EvolvingWilds = card("Evolving Wilds") {
    typeLine = "Land"
    oracleText = "{T}, Sacrifice this land: Search your library for a basic land card, put it onto " +
        "the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = EffectPatterns.searchLibrary(
            filter = Filters.Land,
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            reveal = true,
            shuffleAfter = true
        )
        manaAbility = false
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "264"
        artist = "Alayna Danner"
        flavorText = "\"I don't think we're going to make it back in time for Introduction to Prophecy.\"\n—Tam, Strixhaven first-year"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c632984-5176-4c37-91df-6577cc294b85.jpg?1767863461"
    }
}
