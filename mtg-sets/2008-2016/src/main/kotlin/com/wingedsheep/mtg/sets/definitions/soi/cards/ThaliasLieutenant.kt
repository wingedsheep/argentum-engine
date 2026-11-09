package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thalia's Lieutenant
 * {1}{W}
 * Creature — Human Soldier
 * 1/1
 *
 * When this creature enters, put a +1/+1 counter on each other Human you control.
 * Whenever another Human you control enters, put a +1/+1 counter on this creature.
 *
 * The ETB is [Effects.ForEachInGroup] with `excludeSelf` for "each other" (as on Vampire
 * Socialite); the second ability is Champion of the Parish's trigger.
 */
val ThaliasLieutenant = card("Thalia's Lieutenant") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, put a +1/+1 counter on each other Human you control.\n" +
        "Whenever another Human you control enters, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN).youControl(),
                excludeSelf = true,
            ),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "When this creature enters, put a +1/+1 counter on each other Human you control."
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN).youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever another Human you control enters, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe162594-2332-4816-a9e1-57eb19b12651.jpg?1783937806"
        ruling(
            "2016-04-08",
            "If Thalia's Lieutenant enters the battlefield at the same time as another Human, each of " +
                "Thalia's Lieutenant's abilities will trigger. You'll put a +1/+1 counter on both cards.",
        )
    }
}
