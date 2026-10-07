package com.wingedsheep.mtg.sets.definitions.cn2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

val SinuousVermin = card("Sinuous Vermin") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat Horror"
    power = 2
    toughness = 2
    oracleText = "{3}{B}{B}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)\nAs long as this creature is monstrous, it has menace. (It can't be blocked except by two or more creatures.)"

    activatedAbility {
        cost = Costs.Mana("{3}{B}{B}")
        effect = Effects.Monstrosity(3)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, GroupFilter.source())
        condition = Conditions.SourceIsMonstrous
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Jason Kang"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e50f5505-fcbb-4a84-bcf6-950362d83201.jpg?1783937351"

        ruling("2016-08-23", "Once Sinuous Vermin has been legally blocked by one creature, activating the monstrosity ability to give it menace won’t change or undo that block.")
    }
}
