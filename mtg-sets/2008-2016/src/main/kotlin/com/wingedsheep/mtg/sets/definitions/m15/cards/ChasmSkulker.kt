package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chasm Skulker
 * {2}{U}
 * Creature — Squid Horror
 * 1/1
 * Whenever you draw a card, put a +1/+1 counter on this creature.
 * When this creature dies, create X 1/1 blue Squid creature tokens with islandwalk, where X is the
 * number of +1/+1 counters on this creature.
 *
 * `Triggers.you.draws()` fires once per card drawn (first ruling). The dies trigger counts the
 * +1/+1 counters from last-known information, as Hangarback Walker does (second ruling).
 */
val ChasmSkulker = card("Chasm Skulker") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Squid Horror"
    power = 1
    toughness = 1
    oracleText = "Whenever you draw a card, put a +1/+1 counter on this creature.\n" +
        "When this creature dies, create X 1/1 blue Squid creature tokens with islandwalk, where X is the " +
        "number of +1/+1 counters on this creature. (They can't be blocked as long as defending player " +
        "controls an Island.)"

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = DynamicAmounts.lastKnownPlusOneCounters(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Squid"),
            keywords = setOf(Keyword.ISLANDWALK),
            imageUri = "https://cards.scryfall.io/normal/front/8/c/8c366546-6da9-4daa-b0c3-1401ace5568c.jpg?1783939142",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13cf5b23-9427-4313-8af8-abf2f3800948.jpg?1783939194"
        ruling(
            "2014-07-18",
            "If you draw multiple cards, the first ability will trigger that many times. Each of these abilities " +
                "will cause a +1/+1 counter to be put on Chasm Skulker.",
        )
        ruling(
            "2014-07-18",
            "If enough -1/-1 counters are put on Chasm Skulker at the same time to make its toughness 0 or less, " +
                "the number of +1/+1 counters on it before it got any -1/-1 counters will be used to determine " +
                "how many Squid tokens you get.",
        )
    }
}
