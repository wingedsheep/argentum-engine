package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Enduring Scalelord
 * {4}{G}{W}
 * Creature — Dragon
 * 4/4
 * Flying
 * Whenever one or more +1/+1 counters are put on another creature you control, you may put a
 * +1/+1 counter on this creature.
 *
 * The per-permanent counters-placed trigger (`batch = false`, the default) fires once for each
 * other creature that receives counters in one event, never once per counter — matching the
 * ruling below. The "may" is the optional trigger, which is what breaks the two-Scalelord loop.
 */
val EnduringScalelord = card("Enduring Scalelord") {
    manaCost = "{4}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever one or more +1/+1 counters are put on another creature you control, " +
        "you may put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        optional = true
        effect = Effects.AddCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Clint Cearley"
        flavorText = "Only the sun that beats down upon Arashin's walls could shine more brightly."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dfdfff3-0ef8-4e9a-b6bd-a0e11cbdf22e.jpg?1783938572"
        ruling(
            "2015-02-25",
            "A creature entering the battlefield with one or more +1/+1 counters on it will cause " +
                "Enduring Scalelord's ability to trigger."
        )
        ruling(
            "2015-02-25",
            "If +1/+1 counters are put on multiple creatures you control (other than Enduring Scalelord) " +
                "at the same time, Enduring Scalelord's ability will trigger once for each of those creatures."
        )
        ruling(
            "2015-02-25",
            "If you control two Enduring Scalelords, putting a +1/+1 counter on one of them will cause the " +
                "ability of the other one to trigger. When this ability resolves, you'll put a +1/+1 counter " +
                "on the other Scalelord. This will cause the ability of the first one to trigger. This loop " +
                "will repeat until you choose not to put a +1/+1 counter on one of the Enduring Scalelords."
        )
    }
}
