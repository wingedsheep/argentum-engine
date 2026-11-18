package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wall of Limbs
 * {2}{B}
 * Creature — Zombie Wall
 * 0/3
 * Defender
 * Whenever you gain life, put a +1/+1 counter on this creature.
 * {5}{B}{B}, Sacrifice this creature: Target player loses X life, where X is this creature's power.
 */
val WallOfLimbs = card("Wall of Limbs") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Wall"
    power = 0
    toughness = 3
    oracleText = "Defender (This creature can't attack.)\n" +
        "Whenever you gain life, put a +1/+1 counter on this creature.\n" +
        "{5}{B}{B}, Sacrifice this creature: Target player loses X life, where X is this creature's power."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{5}{B}{B}"),
            Costs.SacrificeSelf
        )
        val t = target(Targets.Player)
        // X is the power as it last existed on the battlefield, +1/+1 counters included
        // (sacrifice-time snapshot), since the wall has already left play.
        effect = Effects.LoseLife(DynamicAmounts.sourcePower(), t)
        description = "{5}{B}{B}, Sacrifice this creature: Target player loses X life, " +
            "where X is this creature's power."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66d46b81-3131-4561-beca-ccf4a0973610.jpg?1783939180"
        ruling(
            "2014-07-18",
            "The ability triggers just once for each life-gaining event, whether it's 1 life from " +
                "Soulmender or 8 life from Meditation Puzzle."
        )
        ruling(
            "2014-07-18",
            "Use Wall of Limbs's power as it last existed on the battlefield, including any +1/+1 " +
                "counters it had, to determine the value of X."
        )
    }
}
