package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tribute to the World Tree
 * {G}{G}{G}
 * Enchantment
 *
 * Whenever a creature you control enters, draw a card if its power is 3 or greater.
 * Otherwise, put two +1/+1 counters on it.
 *
 * The power check is not an intervening "if" — it's read as the ability resolves, so a pump
 * in response changes which branch happens.
 */
val TributeToTheWorldTree = card("Tribute to the World Tree") {
    manaCost = "{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control enters, draw a card if its power is 3 or greater. " +
        "Otherwise, put two +1/+1 counters on it."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.triggeringPower(),
                ComparisonOperator.GTE,
                3
            ),
            then = Effects.DrawCards(1),
            otherwise = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.TriggeringEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Kristina Carroll"
        flavorText = "\"The Tree was the heart of our world. It made me what I am and pulled me back from the " +
            "brink of death. I won't allow it to be forgotten.\"\n—Esika, god of the Tree"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0cdeaba-fc21-44e6-bf99-aa1ff379401b.jpg?1783916960"
    }
}
