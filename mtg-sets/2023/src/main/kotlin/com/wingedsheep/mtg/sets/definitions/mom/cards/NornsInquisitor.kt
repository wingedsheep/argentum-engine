package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Norn's Inquisitor
 * {1}{W}
 * Creature — Phyrexian Knight
 * 1/1
 *
 * When this creature enters, incubate 2. (Create an Incubator token with two +1/+1 counters on it
 * and "{2}: Transform this token." It transforms into a 0/0 Phyrexian artifact creature.)
 * Whenever a permanent you control transforms into a Phyrexian, put a +1/+1 counter on it.
 */
val NornsInquisitor = card("Norn's Inquisitor") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Knight"
    oracleText = "When this creature enters, incubate 2. (Create an Incubator token with two +1/+1 counters " +
        "on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)\n" +
        "Whenever a permanent you control transforms into a Phyrexian, put a +1/+1 counter on it."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Incubate(2)
    }

    // "Transforms into a Phyrexian" is a filter on the face that's up after the transform, in
    // either direction (ruling 2023-04-14).
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl().withSubtype("Phyrexian")).transforms()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Denis Zhbankov"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d387b608-ba4e-49c8-bd81-37594290a352.jpg?1783917054"
        ruling("2023-04-14", "Only transforming double-faced permanents (including transforming double-faced cards and Incubator tokens) can transform. A face-up permanent turning face down doesn't count as transforming, nor does a face-down permanent turning face up.")
        ruling("2023-04-14", "The last ability of Norn's Inquisitor will trigger if a permanent you control transforms in either direction, going from front face up to back face up or vice versa, as long as it's a Phyrexian after doing so.")
    }
}
