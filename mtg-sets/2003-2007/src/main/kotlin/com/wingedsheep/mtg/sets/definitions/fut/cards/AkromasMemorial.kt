package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Akroma's Memorial — Future Sight #159
 * {7} · Legendary Artifact
 *
 * One continuous grant per keyword and per protection color, all over the same
 * "creatures you control" group, so each is re-evaluated against projected state.
 */
val AkromasMemorial = card("Akroma's Memorial") {
    manaCost = "{7}"
    typeLine = "Legendary Artifact"
    oracleText = "Creatures you control have flying, first strike, vigilance, trample, haste, " +
        "and protection from black and from red."

    staticAbility { ability = GrantKeyword(Keyword.FLYING, GroupFilter.AllCreaturesYouControl) }
    staticAbility { ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.AllCreaturesYouControl) }
    staticAbility { ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.AllCreaturesYouControl) }
    staticAbility { ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.AllCreaturesYouControl) }
    staticAbility { ability = GrantKeyword(Keyword.HASTE, GroupFilter.AllCreaturesYouControl) }
    staticAbility { ability = GrantProtection(Color.BLACK, GroupFilter.AllCreaturesYouControl) }
    staticAbility { ability = GrantProtection(Color.RED, GroupFilter.AllCreaturesYouControl) }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Dan Murayama Scott"
        flavorText = "\"No rest. No mercy. No matter what.\"\n—Memorial inscription"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2cb3bdf-dff5-4285-ad67-ae7ca84c9fbc.jpg?1783943092"
    }
}
