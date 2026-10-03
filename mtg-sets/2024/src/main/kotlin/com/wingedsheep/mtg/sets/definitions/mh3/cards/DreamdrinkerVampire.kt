package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dreamdrinker Vampire
 * {1}{B}
 * Creature — Vampire
 * 2/1
 *
 * Lifelink
 * {1}{B}: Adapt 1.
 * Whenever one or more +1/+1 counters are put on this creature, it gains menace until end of turn.
 *
 * Adapt is checked on resolution (the ability can always be activated); the menace trigger fires
 * for +1/+1 counters from any source, not just adapt.
 */
val DreamdrinkerVampire = card("Dreamdrinker Vampire") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    oracleText = "Lifelink\n" +
        "{1}{B}: Adapt 1. (If this creature has no +1/+1 counters on it, put a +1/+1 counter on it.)\n" +
        "Whenever one or more +1/+1 counters are put on this creature, it gains menace until end of turn."
    power = 2
    toughness = 1

    keywords(Keyword.LIFELINK)

    // {1}{B}: Adapt 1.
    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.If(
            Conditions.Not(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        effect = Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Steve Ellis"
        flavorText = "The dream she tastes will be your last."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/961a3a53-344b-44d5-bc8e-f43437e6b558.jpg?1783911282"
        ruling("2024-06-07", "Once Dreamdrinker Vampire becomes blocked by a creature, giving it menace by putting one or more +1/+1 counters on it won't cause it to become unblocked.")
        ruling("2024-06-07", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
        ruling("2024-06-07", "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.")
    }
}
