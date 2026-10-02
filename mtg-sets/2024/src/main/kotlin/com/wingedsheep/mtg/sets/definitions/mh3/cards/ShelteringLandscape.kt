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
 * Sheltering Landscape — Modern Horizons 3 #227. One of MH3's Naya-shard cycling fetch lands:
 * taps for {C}, sacrifices to fetch a basic Mountain/Forest/Plains tapped, cycles for {R}{G}{W}.
 */
val ShelteringLandscape = card("Sheltering Landscape") {
    typeLine = "Land"
    colorIdentity = "RGW"
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Sacrifice this land: Search your library for a basic Mountain, Forest, or Plains card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Cycling {R}{G}{W} ({R}{G}{W}, Discard this card: Draw a card.)"

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
                listOf(Subtype.MOUNTAIN, Subtype.FOREST, Subtype.PLAINS)
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.cycling("{R}{G}{W}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "227"
        artist = "Erikas Perl"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fe070f4-8877-4280-b8fd-869f3ac34ab6.jpg?1783911238"
    }
}
