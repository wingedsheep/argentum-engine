package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Primal Adversary — Innistrad: Midnight Hunt #194
 * {2}{G} · Creature — Wolf · 4/3
 *
 * Trample
 * When this creature enters, you may pay {1}{G} any number of times. When you pay this cost one
 * or more times, put that many +1/+1 counters on this creature, then up to that many target lands
 * you control become 3/3 Wolf creatures with haste that are still lands.
 *
 * The repeated payment is [Effects.PayRepeatedly] under an optional reflexive trigger (CR 603.12),
 * as on Spectral Adversary: targets for the reflexive half are chosen after the payment, capped by
 * [DynamicAmounts.timesPaid]. On resolution the Adversary gets its counters, then each chosen land
 * permanently becomes a 3/3 Wolf creature with haste, keeping its land types and abilities.
 */
val PrimalAdversary = card("Primal Adversary") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    power = 4
    toughness = 3
    oracleText = "Trample\n" +
        "When this creature enters, you may pay {1}{G} any number of times. When you pay this cost " +
        "one or more times, put that many +1/+1 counters on this creature, then up to that many " +
        "target lands you control become 3/3 Wolf creatures with haste that are still lands."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayRepeatedly("{1}{G}"),
            optional = true,
            reflexiveEffect = Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.timesPaid(),
                EffectTarget.Self
            ) then Effects.ForEachTarget(
                Effects.BecomeCreature(
                    target = EffectTarget.ContextTarget(0),
                    power = 3,
                    toughness = 3,
                    keywords = setOf(Keyword.HASTE),
                    creatureTypes = setOf("Wolf"),
                    duration = Duration.Permanent
                )
            ),
            reflexiveTargetRequirements = listOf(
                TargetObject(
                    filter = TargetFilter.LandYouControl,
                    optional = true,
                    dynamicMaxCount = DynamicAmounts.timesPaid()
                )
            ),
            descriptionOverride = "You may pay {1}{G} any number of times. When you pay this cost " +
                "one or more times, put that many +1/+1 counters on this creature, then up to that " +
                "many target lands you control become 3/3 Wolf creatures with haste that are still lands."
        )
        description = "When this creature enters, you may pay {1}{G} any number of times. When you " +
            "pay this cost one or more times, put that many +1/+1 counters on this creature, then " +
            "up to that many target lands you control become 3/3 Wolf creatures with haste that are " +
            "still lands."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "194"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf3ce5c5-ea31-4f6d-84dd-430f52c49c4e.jpg?1783925576"
        ruling(
            "2021-09-24",
            "Any land that becomes a Wolf creature this way will retain any supertypes, card types, " +
                "subtypes, and abilities it had."
        )
        ruling(
            "2021-09-24",
            "After you pay the {1}{G} cost one or more times, a second ability triggers and you " +
                "choose the targets for it. Players may respond to the new ability at that point. As " +
                "that ability resolves, Primal Adversary gets its +1/+1 counters and the target " +
                "lands become Wolf creatures."
        )
    }
}
