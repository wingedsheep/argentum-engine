package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Relief Captain — Oath of the Gatewatch #32 (canonical printing)
 * {2}{W}{W} · Creature — Kor Knight Ally · 3/2
 *
 * When this creature enters, support 3. (Put a +1/+1 counter on each of up to three other target creatures.)
 *
 * Support on a permanent targets *other* creatures (any controller): Expedition Raptor's ETB shape.
 */
val ReliefCaptain = card("Relief Captain") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Knight Ally"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, support 3. (Put a +1/+1 counter on each of up to three " +
        "other target creatures.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.OtherCreature, count = 3, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        )
        description = "When this creature enters, support 3 — put a +1/+1 counter on each of up " +
            "to three other target creatures."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Anastasia Ovchinnikova"
        flavorText = "Sometimes gestures of support are small. Sometimes not."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ecacff4-87b9-4cc1-bfbe-019cfda82d17.jpg?1783937924"
        ruling("2016-01-22", "You can't put more than one +1/+1 counter on any one target using the support action.")
        ruling("2016-01-22", "Support can target a creature you don't control.")
        ruling("2016-01-22", "If a spell with support has other abilities that target creatures, those abilities and the support ability can target the same creature.")
    }
}
