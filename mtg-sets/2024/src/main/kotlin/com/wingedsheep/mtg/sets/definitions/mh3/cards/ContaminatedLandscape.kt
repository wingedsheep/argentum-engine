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
 * Contaminated Landscape — Modern Horizons 3 #218. One of MH3's Esper-shard cycling fetch lands:
 * taps for {C}, sacrifices to fetch a basic Plains/Island/Swamp tapped, cycles for {W}{U}{B}.
 */
val ContaminatedLandscape = card("Contaminated Landscape") {
    typeLine = "Land"
    colorIdentity = "WUB"
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Sacrifice this land: Search your library for a basic Plains, Island, or Swamp card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Cycling {W}{U}{B} ({W}{U}{B}, Discard this card: Draw a card.)"

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
                listOf(Subtype.PLAINS, Subtype.ISLAND, Subtype.SWAMP)
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.cycling("{W}{U}{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "218"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2312c49-1627-47ad-8113-78a999a97d8d.jpg?1783911240"
    }
}
