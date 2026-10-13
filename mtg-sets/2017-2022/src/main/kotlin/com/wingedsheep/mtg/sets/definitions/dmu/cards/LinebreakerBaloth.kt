package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.Enlist
import com.wingedsheep.sdk.scripting.GameObjectFilter

val LinebreakerBaloth = card("Linebreaker Baloth") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 5
    oracleText = "Enlist (As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature's until end of turn.)\nThis creature can't be blocked by creatures with power 2 or less."

    staticAbility { ability = Enlist }
    staticAbility {
        ability = CantBeBlockedBy(blockerFilter = GameObjectFilter.Creature.powerAtMost(2))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Brent Hollowell"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15cc59c7-6478-4f84-898a-7e0fed10f004.jpg?1783921300"
        ruling("2022-09-09", "Once a creature with power 3 or greater has blocked this creature, changing the power of the blocking creature won't cause this creature to become unblocked.")
    }
}
