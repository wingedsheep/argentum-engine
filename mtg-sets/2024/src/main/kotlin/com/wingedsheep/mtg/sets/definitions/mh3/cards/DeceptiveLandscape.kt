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
 * Deceptive Landscape — Modern Horizons 3 #219. One of MH3's Abzan-wedge cycling fetch lands:
 * taps for {C}, sacrifices to fetch a basic Plains/Swamp/Forest tapped, cycles for {W}{B}{G}.
 */
val DeceptiveLandscape = card("Deceptive Landscape") {
    typeLine = "Land"
    colorIdentity = "WBG"
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Sacrifice this land: Search your library for a basic Plains, Swamp, or Forest card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Cycling {W}{B}{G} ({W}{B}{G}, Discard this card: Draw a card.)"

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
                listOf(Subtype.PLAINS, Subtype.SWAMP, Subtype.FOREST)
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.cycling("{W}{B}{G}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "219"
        artist = "Erikas Perl"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2ae6828e-ff19-45db-8b59-61616353491f.jpg?1783911239"
    }
}
