package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ramunap Hydra — Hour of Devastation #130 (canonical printing)
 * {3}{G} · Creature — Snake Hydra · 3/3
 *
 * Reach, vigilance, trample
 * This creature gets +1/+1 as long as you control a Desert.
 * This creature gets +1/+1 as long as there is a Desert card in your graveyard.
 *
 * Two independent layer-7c self [ModifyStats] boosts, each gated by its own
 * [ConditionalStaticAbility] — unlike the set's either-half Desert cards, meeting both
 * conditions gives +2/+2.
 */
val RamunapHydra = card("Ramunap Hydra") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Hydra"
    power = 3
    toughness = 3
    oracleText = "Reach, vigilance, trample\n" +
        "This creature gets +1/+1 as long as you control a Desert.\n" +
        "This creature gets +1/+1 as long as there is a Desert card in your graveyard."

    keywords(Keyword.REACH, Keyword.VIGILANCE, Keyword.TRAMPLE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, GroupFilter.source()),
            condition = Conditions.ControlPermanentOfType(Subtype.DESERT),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(1, 1, GroupFilter.source()),
            condition = Conditions.GraveyardContainsSubtype(Subtype.DESERT),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Mike Burns"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/953f3722-2126-4eb3-a3d8-cff4b0703a9c.jpg?1783936014"
        ruling("2017-07-14", "Unlike other cards in this set that care only whether you control a Desert or there is a Desert card in your graveyard, Ramunap Hydra's abilities reward you for meeting both conditions. If you control one or more Deserts and have one or more Desert cards in your graveyard, it'll get +2/+2.")
    }
}
