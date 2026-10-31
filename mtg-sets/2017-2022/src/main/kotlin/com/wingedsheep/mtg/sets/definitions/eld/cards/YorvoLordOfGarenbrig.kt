package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yorvo, Lord of Garenbrig — Throne of Eldraine #185
 * {G}{G}{G} Legendary Creature — Giant Noble 0/0
 *
 * Yorvo enters with four +1/+1 counters on it.
 * Whenever another green creature you control enters, put a +1/+1 counter on Yorvo. Then if that
 * creature's power is greater than Yorvo's power, put another +1/+1 counter on Yorvo.
 *
 * The "then if" is a resolution-time check sequenced *after* the first counter (ruling 2019-10-04),
 * so it is an [Effects.If] chained behind the first [Effects.AddCounters], not an intervening-if.
 * Both powers are read live as the gate resolves; "that creature's power" falls back to last-known
 * information when the entering creature has left the battlefield (second ruling).
 */
val YorvoLordOfGarenbrig = card("Yorvo, Lord of Garenbrig") {
    manaCost = "{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Giant Noble"
    power = 0
    toughness = 0
    oracleText = "Yorvo enters with four +1/+1 counters on it.\n" +
        "Whenever another green creature you control enters, put a +1/+1 counter on Yorvo. " +
        "Then if that creature's power is greater than Yorvo's power, put another +1/+1 counter on Yorvo."

    replacementEffect(EntersWithCounters(count = 4, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.withColor(Color.GREEN).youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.triggeringPower(),
                    ComparisonOperator.GT,
                    DynamicAmounts.sourcePower(),
                ),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae2998a1-1713-467e-a08e-0efd8720aa5b.jpg?1783932599"
        ruling(
            "2019-10-04",
            "Yorvo's triggered ability compares the power of the entering green creature only after putting a " +
                "+1/+1 counter on Yorvo. If that counter causes Yorvo's power to be greater than or equal to the " +
                "entering creature's power, it won't get a second +1/+1 counter."
        )
        ruling(
            "2019-10-04",
            "If the entering green creature dies before Yorvo's triggered ability resolves, use its power as it " +
                "last existed on the battlefield to determine whether Yorvo gets a second +1/+1 counter."
        )
    }
}
