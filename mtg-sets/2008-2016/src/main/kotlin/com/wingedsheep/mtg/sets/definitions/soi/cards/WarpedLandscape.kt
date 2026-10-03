package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Warped Landscape
 * Land
 * {T}: Add {C}.
 * {2}, {T}, Sacrifice this land: Search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle.
 *
 * The Terminal Moraine shape: a colorless mana ability plus a sacrifice-self activated ability
 * over [Patterns.Library.searchLibrary] for a basic land to the battlefield tapped.
 */
val WarpedLandscape = card("Warped Landscape") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{2}, {T}, Sacrifice this land: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "280"
        artist = "Cliff Childs"
        flavorText = "Each cryptolith twists the plane's mana, bending its flow to a singular purpose."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16793435-8eb3-4d57-8683-de1120eb46b6.jpg?1783937697"
    }
}
