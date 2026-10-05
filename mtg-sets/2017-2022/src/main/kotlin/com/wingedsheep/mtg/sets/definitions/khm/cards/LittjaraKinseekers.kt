package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Littjara Kinseekers
 * {3}{U}
 * Creature — Shapeshifter
 * 2/4
 * Changeling (This card is every creature type.)
 * When this creature enters, if you control three or more creatures that share a creature type,
 * put a +1/+1 counter on this creature, then scry 1.
 *
 * "Three or more creatures that share a creature type" is the largest creature-type tribe among
 * your creatures ([DynamicAmounts.largestSharedCreatureTypeCount], projected, so changelings count
 * toward every tribe) being at least three. It is an intervening "if": checked when the creature
 * enters and again on resolution.
 */
val LittjaraKinseekers = card("Littjara Kinseekers") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    oracleText = "Changeling (This card is every creature type.)\n" +
        "When this creature enters, if you control three or more creatures that share a creature type, " +
        "put a +1/+1 counter on this creature, then scry 1."
    power = 2
    toughness = 4

    keywords(Keyword.CHANGELING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.largestSharedCreatureTypeCount(),
            ComparisonOperator.GTE,
            3,
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Tyler Walpole"
        flavorText = "Like calls to like, and none are forgotten."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c119f836-0707-49e2-b6d4-25f849d054a4.jpg?1783928261"
        ruling("2021-02-05", "Three creatures share a creature type if there's at least one creature type all three have, no matter what other creature types they have. For example, Littjara Kinseekers (which has all creature types), an Elf Warrior, and an Elf Wizard share a creature type because they're all Elves.")
        ruling("2021-02-05", "If you don't control three or more creatures that share a creature type immediately after Littjara Kinseekers enters the battlefield, its ability doesn't trigger. If you don't control three or more as the ability resolves, you won't put a +1/+1 counter on Littjara Kinseekers or scry 1. The three shared-type creatures you control when the ability resolves don't have to be the same three you controlled when the ability triggered.")
        ruling("2021-02-05", "You put just one +1/+1 counter on Littjara Kinseekers and scry 1, no matter how many extra trios of creatures that share a creature type you control.")
    }
}
