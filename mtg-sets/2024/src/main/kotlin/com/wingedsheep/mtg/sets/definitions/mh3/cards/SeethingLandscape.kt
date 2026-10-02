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
 * Seething Landscape — Modern Horizons 3 #225. One of MH3's Grixis-shard cycling fetch lands:
 * taps for {C}, sacrifices to fetch a basic Island/Swamp/Mountain tapped, cycles for {U}{B}{R}.
 */
val SeethingLandscape = card("Seething Landscape") {
    typeLine = "Land"
    colorIdentity = "UBR"
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Sacrifice this land: Search your library for a basic Island, Swamp, or Mountain card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Cycling {U}{B}{R} ({U}{B}{R}, Discard this card: Draw a card.)"

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
                listOf(Subtype.ISLAND, Subtype.SWAMP, Subtype.MOUNTAIN)
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.cycling("{U}{B}{R}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "225"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/661fc907-7003-45c6-820c-9616e9a71c30.jpg?1783911238"
    }
}
