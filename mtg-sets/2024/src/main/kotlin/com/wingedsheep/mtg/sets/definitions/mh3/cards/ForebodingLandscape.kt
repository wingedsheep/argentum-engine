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
 * Foreboding Landscape — Modern Horizons 3 #221. One of MH3's Sultai-wedge cycling fetch lands:
 * taps for {C}, sacrifices to fetch a basic Swamp/Forest/Island tapped, cycles for {B}{G}{U}.
 */
val ForebodingLandscape = card("Foreboding Landscape") {
    typeLine = "Land"
    colorIdentity = "BGU"
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Sacrifice this land: Search your library for a basic Swamp, Forest, or Island card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Cycling {B}{G}{U} ({B}{G}{U}, Discard this card: Draw a card.)"

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
                listOf(Subtype.SWAMP, Subtype.FOREST, Subtype.ISLAND)
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.cycling("{B}{G}{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "221"
        artist = "Erikas Perl"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57fb0fa7-0c5c-4a75-9461-c51403c30282.jpg?1783911239"
    }
}
