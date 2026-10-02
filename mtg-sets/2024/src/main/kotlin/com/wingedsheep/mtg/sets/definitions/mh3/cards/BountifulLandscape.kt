package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Bountiful Landscape — Modern Horizons 3 #217. One of MH3's Temur-wedge cycling fetch lands:
 * taps for {C}, sacrifices to fetch a basic Forest/Island/Mountain tapped, cycles for {G}{U}{R}.
 */
val BountifulLandscape = card("Bountiful Landscape") {
    typeLine = "Land"
    colorIdentity = "GUR"
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Sacrifice this land: Search your library for a basic Forest, Island, or Mountain card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Cycling {G}{U}{R} ({G}{U}{R}, Discard this card: Draw a card.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withAnyOfSubtypes(
                listOf(Subtype.FOREST, Subtype.ISLAND, Subtype.MOUNTAIN)
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.cycling("{G}{U}{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "217"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b277752b-430a-4f09-8a98-b72f813dd52e.jpg?1783911240"
    }
}
