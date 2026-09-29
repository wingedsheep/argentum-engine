package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elvish Vatkeeper
 * {1}{B}{G}
 * Creature — Phyrexian Elf
 * 3/3
 *
 * When this creature enters, incubate 2.
 * {5}: Transform target Incubator token you control. Double the number of +1/+1 counters on it.
 *
 * The transform happens first, then the doubling (per the ruling), so the counters land on the
 * Phyrexian artifact creature. Doubling is "put as many +1/+1 counters on it as it already has".
 */
val ElvishVatkeeper = card("Elvish Vatkeeper") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Phyrexian Elf"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, incubate 2. (Create an Incubator token with two +1/+1 " +
        "counters on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact " +
        "creature.)\n" +
        "{5}: Transform target Incubator token you control. Double the number of +1/+1 counters on it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Incubate(2)
    }

    activatedAbility {
        cost = Costs.Mana("{5}")
        val incubator = target(
            TargetFilter(GameObjectFilter.Permanent.withSubtype("Incubator").token().youControl())
        )
        effect = Effects.Transform(incubator) then
            Effects.AddDynamicCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmounts.countersOn(incubator, CounterType.PLUS_ONE_PLUS_ONE),
                target = incubator
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3227c07-9ef2-46a6-9b4f-bbd6ef12f4a5.jpg?1783916955"
        ruling("2023-04-14", "The target Incubator token transforms before its +1/+1 counters are doubled, so anything that cares about +1/+1 counters being placed on a creature will see them being placed on the Phyrexian artifact creature.")
    }
}
