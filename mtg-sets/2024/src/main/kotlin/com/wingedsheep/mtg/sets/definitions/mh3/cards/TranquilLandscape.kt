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
 * Tranquil Landscape — Modern Horizons 3 #231. One of MH3's Bant-shard cycling fetch lands:
 * taps for {C}, sacrifices to fetch a basic Forest/Plains/Island tapped, cycles for {G}{W}{U}.
 */
val TranquilLandscape = card("Tranquil Landscape") {
    typeLine = "Land"
    colorIdentity = "GWU"
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Sacrifice this land: Search your library for a basic Forest, Plains, or Island card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Cycling {G}{W}{U} ({G}{W}{U}, Discard this card: Draw a card.)"

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
                listOf(Subtype.FOREST, Subtype.PLAINS, Subtype.ISLAND)
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.cycling("{G}{W}{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "231"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/113f48b9-a972-4e2c-af95-05ab078e01f2.jpg?1783911233"
    }
}
