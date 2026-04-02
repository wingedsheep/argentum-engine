package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shalai, Voice of Plenty
 * {3}{W}
 * Legendary Creature — Angel
 * 3/4
 * Flying
 * You, planeswalkers you control, and other creatures you control have hexproof.
 * {4}{G}{G}: Put a +1/+1 counter on each creature you control.
 */
val ShalaiVoiceOfPlenty = card("Shalai, Voice of Plenty") {
    manaCost = "{3}{W}"
    typeLine = "Legendary Creature — Angel"
    power = 3
    toughness = 4
    oracleText = "Flying\nYou, planeswalkers you control, and other creatures you control have hexproof.\n{4}{G}{G}: Put a +1/+1 counter on each creature you control."

    keywords(Keyword.FLYING)

    // You have hexproof
    staticAbility {
        ability = GrantHexproofToController
    }

    // Planeswalkers you control have hexproof
    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.HEXPROOF,
            filter = GroupFilter.PlaneswalkersYouControl
        )
    }

    // Other creatures you control have hexproof
    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.HEXPROOF,
            filter = GroupFilter.OtherCreaturesYouControl
        )
    }

    // {4}{G}{G}: Put a +1/+1 counter on each creature you control
    activatedAbility {
        cost = Costs.Mana("{4}{G}{G}")
        effect = ForEachInGroupEffect(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db827ee7-6f2e-4e10-aac0-120fc2b69fbd.jpg?1562743987"
    }
}
