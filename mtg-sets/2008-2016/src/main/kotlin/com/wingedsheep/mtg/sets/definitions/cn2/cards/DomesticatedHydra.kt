package com.wingedsheep.mtg.sets.definitions.cn2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

val DomesticatedHydra = card("Domesticated Hydra") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 3
    toughness = 3
    oracleText = "{X}{G}{G}{G}: Monstrosity X. (If this creature isn't monstrous, put X +1/+1 counters on it and it becomes monstrous.)\nAs long as this creature is monstrous, it has trample."

    activatedAbility {
        cost = Costs.Mana("{X}{G}{G}{G}")
        effect = Effects.Monstrosity(DynamicAmounts.xValue())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.source())
        condition = Conditions.SourceIsMonstrous
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Mathias Kollros"
        flavorText = "\"Sit. Fetch. Devastate.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23be85ac-2d9c-4261-aa55-4421e4a35d8b.jpg?1783937345"
    }
}
