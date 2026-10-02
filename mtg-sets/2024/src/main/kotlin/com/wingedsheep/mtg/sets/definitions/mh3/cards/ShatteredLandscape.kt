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
 * Shattered Landscape — Modern Horizons 3 #226. One of MH3's Mardu-wedge cycling fetch lands:
 * taps for {C}, sacrifices to fetch a basic Mountain/Plains/Swamp tapped, cycles for {R}{W}{B}.
 */
val ShatteredLandscape = card("Shattered Landscape") {
    typeLine = "Land"
    colorIdentity = "RWB"
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Sacrifice this land: Search your library for a basic Mountain, Plains, or Swamp card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Cycling {R}{W}{B} ({R}{W}{B}, Discard this card: Draw a card.)"

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
                listOf(Subtype.MOUNTAIN, Subtype.PLAINS, Subtype.SWAMP)
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    keywordAbility(KeywordAbility.cycling("{R}{W}{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "226"
        artist = "Erikas Perl"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3da28c7-6e92-439d-a163-91682d4f11dc.jpg?1783911237"
    }
}
