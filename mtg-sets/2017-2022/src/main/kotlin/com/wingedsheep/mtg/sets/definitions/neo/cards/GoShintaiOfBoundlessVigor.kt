package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Go-Shintai of Boundless Vigor — Kamigawa: Neon Dynasty #187 (canonical printing)
 * {1}{G} · Legendary Enchantment Creature — Shrine · 1/1
 *
 * Trample
 * At the beginning of your end step, you may pay {1}. When you do, put a +1/+1 counter on target
 * Shrine for each Shrine you control.
 *
 * "When you do" is a reflexive trigger (CR 603.12): the {1} is paid as the end-step trigger
 * resolves, and only then does the counter ability go on the stack and choose its target. The target
 * is any Shrine permanent, creature or not; the Shrine count is read when the reflexive ability
 * resolves and includes the Go-Shintai itself.
 */
val GoShintaiOfBoundlessVigor = card("Go-Shintai of Boundless Vigor") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Enchantment Creature — Shrine"
    power = 1
    toughness = 1
    oracleText = "Trample\nAt the beginning of your end step, you may pay {1}. When you do, put a " +
        "+1/+1 counter on target Shrine for each Shrine you control."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{1}"),
            descriptionOverride = "You may pay {1}. When you do, put a +1/+1 counter on target Shrine " +
                "for each Shrine you control."
        ) {
            val shrine = target(TargetFilter.Permanent.withSubtype("Shrine"))
            effect = Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count(),
                shrine,
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Johannes Voss"
        flavorText = "It seeks those with untamed spirits and offers wild strength."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/691016a1-8630-423c-9a57-061593d170fd.jpg?1783923848"
        ruling(
            "2022-02-18",
            "Go-Shintai of Boundless Vigor's reflexive triggered ability may target a Shrine which isn't currently a creature.",
        )
        ruling(
            "2022-02-18",
            "Count the number of Shrines you control as the reflexive triggered ability resolves to determine how many +1/+1 counters to put on the target Shrine.",
        )
    }
}
