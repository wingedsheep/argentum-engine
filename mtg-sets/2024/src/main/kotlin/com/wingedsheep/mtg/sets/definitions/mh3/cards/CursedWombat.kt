package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cursed Wombat {B}{G}
 * Creature — Nightmare Wombat
 * 2/3
 * {2}{B}{G}: Adapt 2.
 * Permanents you control have "Whenever one or more +1/+1 counters are put on this permanent, put
 * an additional +1/+1 counter on it. This ability triggers only once each turn."
 *
 * Adapt is the zero-counter gate over AddCounters (CR 701.46a). The granted ability is a real
 * triggered ability on each permanent (not a replacement), capped per permanent per turn via
 * `oncePerTurn`.
 */
val CursedWombat = card("Cursed Wombat") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Nightmare Wombat"
    oracleText = "{2}{B}{G}: Adapt 2. (If this creature has no +1/+1 counters on it, put two +1/+1 counters on it.)\n" +
        "Permanents you control have \"Whenever one or more +1/+1 counters are put on this permanent, put an " +
        "additional +1/+1 counter on it. This ability triggers only once each turn.\""
    power = 2
    toughness = 3

    // {2}{B}{G}: Adapt 2.
    activatedAbility {
        cost = Costs.Mana("{2}{B}{G}")
        effect = Effects.If(
            Conditions.Not(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
        )
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                oncePerTurn = true
                description = "Whenever one or more +1/+1 counters are put on this permanent, put an " +
                    "additional +1/+1 counter on it. This ability triggers only once each turn."
            },
            filter = GroupFilter.AllPermanentsYouControl,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a09a8de1-98b3-49e0-82f6-d90f1048de44.jpg?1783911252"
    }
}
