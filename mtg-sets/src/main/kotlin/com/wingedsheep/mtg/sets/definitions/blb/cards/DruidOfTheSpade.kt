package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Druid of the Spade
 * {2}{G}
 * Creature — Rabbit Druid
 * 2/3
 * As long as you control a token, this creature gets +2/+0 and has trample.
 */
val DruidOfTheSpade = card("Druid of the Spade") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Rabbit Druid"
    power = 2
    toughness = 3
    oracleText = "As long as you control a token, this creature gets +2/+0 and has trample."

    val controlAToken = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Token)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 0, GroupFilter.source()),
            condition = controlAToken
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.source()),
            condition = controlAToken
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Andrey Kuzinskiy"
        flavorText = "The druid brought the best out of the soil and, in turn, the best out of the fluffle."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b485cf7-bad0-4824-9ba7-cb112ce4769f.jpg?1721426796"
    }
}
