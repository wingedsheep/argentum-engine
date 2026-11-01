package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bioessence Hydra — War of the Spark #186
 * {3}{G}{U} · Creature — Hydra Mutant · 4/4
 *
 * Trample
 * This creature enters with a +1/+1 counter on it for each loyalty counter on planeswalkers you
 * control.
 * Whenever one or more loyalty counters are put on planeswalkers you control, put that many +1/+1
 * counters on this creature.
 *
 * The trigger is the batch template: one event that puts loyalty counters on several
 * planeswalkers fires it once, and "that many" is the batch total. A planeswalker entering with
 * loyalty counters fires it too (rulings), including one entering alongside the Hydra — whose
 * counters the entry count doesn't see.
 */
val BioessenceHydra = card("Bioessence Hydra") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Hydra Mutant"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "This creature enters with a +1/+1 counter on it for each loyalty counter on planeswalkers you control.\n" +
        "Whenever one or more loyalty counters are put on planeswalkers you control, put that many +1/+1 counters on this creature."

    keywords(Keyword.TRAMPLE)

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Planeswalker)
                .totalCounters(CounterType.LOYALTY)
        )
    )

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Planeswalker.youControl())
            .getsCounters(CounterType.LOYALTY, batch = true)
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.triggerCountersPlaced(),
            EffectTarget.Self
        )
        description = "Whenever one or more loyalty counters are put on planeswalkers you control, " +
            "put that many +1/+1 counters on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb01a161-49a1-407c-8410-6933bc8f7b6a.jpg?1783933402"
    }
}
