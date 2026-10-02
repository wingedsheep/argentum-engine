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
 * Perilous Landscape — Modern Horizons 3 #223. One of MH3's Jeskai-wedge cycling fetch lands:
 * taps for {C}, sacrifices to fetch a basic Island/Mountain/Plains tapped, cycles for {U}{R}{W}.
 */
val PerilousLandscape = card("Perilous Landscape") {
    typeLine = "Land"
    colorIdentity = "URW"
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Sacrifice this land: Search your library for a basic Island, Mountain, or Plains card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Cycling {U}{R}{W} ({U}{R}{W}, Discard this card: Draw a card.)"

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
                listOf(Subtype.ISLAND, Subtype.MOUNTAIN, Subtype.PLAINS)
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.cycling("{U}{R}{W}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "223"
        artist = "Alayna Danner"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b0bd07e-cf80-4d64-af29-f4cec6632b3e.jpg?1783911239"
    }
}
