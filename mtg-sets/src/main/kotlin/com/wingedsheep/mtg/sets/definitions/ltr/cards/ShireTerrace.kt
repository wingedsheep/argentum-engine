package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Shire Terrace
 * Land
 *
 * {T}: Add {C}.
 * {1}, {T}, Sacrifice this land: Search your library for a basic land card, put it onto
 * the battlefield tapped, then shuffle.
 */
val ShireTerrace = card("Shire Terrace") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{1}, {T}, Sacrifice this land: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."

    // {T}: Add {C}.
    activatedAbility {
        cost = Costs.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {1}, {T}, Sacrifice this land: Search your library for a basic land card, put it
    // onto the battlefield tapped, then shuffle.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "261"
        artist = "Jeremy Paillotin"
        flavorText = "Everything looked fresh, and the new green of Spring was shimmering in the fields."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25932483-58cd-4ae5-82bf-ab455177d117.jpg?1686970417"
    }
}
