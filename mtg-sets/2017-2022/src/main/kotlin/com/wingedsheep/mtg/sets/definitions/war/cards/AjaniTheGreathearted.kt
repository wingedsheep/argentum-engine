package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani, the Greathearted — War of the Spark #184
 * {2}{G}{W} · Legendary Planeswalker — Ajani · Loyalty 5
 *
 * Creatures you control have vigilance.
 * +1: You gain 3 life.
 * −2: Put a +1/+1 counter on each creature you control and a loyalty counter on each other
 * planeswalker you control.
 *
 * The −2 fans out twice over the permanents present at resolution: a planeswalker that is also a
 * creature gets both counters, and Ajani himself gets a +1/+1 counter only if he is a creature
 * (both per the 2019-05-03 rulings).
 */
val AjaniTheGreathearted = card("Ajani, the Greathearted") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 5
    oracleText = "Creatures you control have vigilance.\n" +
        "+1: You gain 3 life.\n" +
        "−2: Put a +1/+1 counter on each creature you control and a loyalty counter on each other planeswalker you control."

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter(GameObjectFilter.Creature.youControl()))
    }

    loyaltyAbility(+1) {
        effect = Effects.GainLife(3)
    }

    loyaltyAbility(-2) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        ) then Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Planeswalker.youControl(), excludeSelf = true),
            Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6cc78151-8cb0-4521-9674-fb0c67e24a17.jpg?1783933403"
        ruling(
            "2019-05-03",
            "If a planeswalker you control is also a creature (most likely because it's Gideon), that " +
                "planeswalker receives both a +1/+1 counter and a loyalty counter as Ajani's last ability resolves."
        )
        ruling(
            "2019-05-03",
            "If Ajani is somehow a creature as his last ability resolves, he'll get a +1/+1 counter."
        )
    }
}
