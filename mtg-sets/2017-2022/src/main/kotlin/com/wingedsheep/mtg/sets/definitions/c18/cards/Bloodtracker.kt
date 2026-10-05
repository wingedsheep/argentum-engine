package com.wingedsheep.mtg.sets.definitions.c18.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodtracker — Commander 2018 #14
 * {3}{B} · Creature — Vampire Wizard · 2/2
 *
 * Flying
 * {B}, Pay 2 life: Put a +1/+1 counter on this creature.
 * When this creature leaves the battlefield, draw a card for each +1/+1 counter on it.
 *
 * The leaves trigger reads [DynamicAmounts.lastKnownPlusOneCounters] — the +1/+1 counters it had
 * as it left (CR 603.10), the same shape as Marketback Walker's dies trigger but for any
 * destination (exile, bounce, graveyard).
 */
val Bloodtracker = card("Bloodtracker") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Wizard"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "{B}, Pay 2 life: Put a +1/+1 counter on this creature.\n" +
        "When this creature leaves the battlefield, draw a card for each +1/+1 counter on it."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.PayLife(2))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "{B}, Pay 2 life: Put a +1/+1 counter on this creature."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.DrawCards(DynamicAmounts.lastKnownPlusOneCounters())
        description = "When this creature leaves the battlefield, draw a card for each +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Magali Villeneuve"
        flavorText = "\"Flee all you like. The further you run the more firmly I feel your heartbeat.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca3519c4-4d8c-4caf-bc93-6e6160a3d5b6.jpg?1783934340"
        ruling(
            "2018-07-13",
            "If enough -1/-1 counters are put on Bloodtracker at the same time to make its toughness 0 or less, " +
                "the number of +1/+1 counters on it before it got any -1/-1 counters will be used to determine " +
                "how many cards you draw. For example, if there are three +1/+1 counters on Bloodtracker and it " +
                "gets six -1/-1 counters, you'll draw three cards. That's because Bloodtracker's triggered " +
                "ability checks the creature's existence just before it leaves the battlefield, and it still " +
                "has all those counters on it at that point."
        )
    }
}
