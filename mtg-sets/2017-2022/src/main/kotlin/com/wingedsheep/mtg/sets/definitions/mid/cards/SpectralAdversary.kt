package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Spectral Adversary — Innistrad: Midnight Hunt #77
 * {1}{U} · Creature — Spirit · 2/1
 *
 * Flash
 * Flying
 * When this creature enters, you may pay {1}{U} any number of times. When you pay this cost one
 * or more times, put that many +1/+1 counters on this creature, then up to that many other target
 * artifacts, creatures, and/or enchantments phase out.
 *
 * The repeated payment is [Effects.PayRepeatedly] under an optional reflexive trigger (CR 603.12):
 * targets for the reflexive half are chosen after the payment, capped by
 * [DynamicAmounts.timesPaid] read off the action half's carried pipeline (as Miasma Demon caps
 * by its discard count). On resolution the Adversary gets its counters, then each chosen target
 * phases out.
 */
val SpectralAdversary = card("Spectral Adversary") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "Flash\nFlying\n" +
        "When this creature enters, you may pay {1}{U} any number of times. When you pay this cost " +
        "one or more times, put that many +1/+1 counters on this creature, then up to that many " +
        "other target artifacts, creatures, and/or enchantments phase out."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayRepeatedly("{1}{U}"),
            optional = true,
            reflexiveEffect = Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.timesPaid(),
                EffectTarget.Self
            ) then Effects.ForEachTarget(Effects.PhaseOut(EffectTarget.ContextTarget(0))),
            reflexiveTargetRequirements = listOf(
                TargetObject(
                    filter = TargetFilter.ArtifactCreatureOrEnchantment.other(),
                    optional = true,
                    dynamicMaxCount = DynamicAmounts.timesPaid()
                )
            ),
            descriptionOverride = "You may pay {1}{U} any number of times. When you pay this cost " +
                "one or more times, put that many +1/+1 counters on this creature, then up to that " +
                "many other target artifacts, creatures, and/or enchantments phase out."
        )
        description = "When this creature enters, you may pay {1}{U} any number of times. When you " +
            "pay this cost one or more times, put that many +1/+1 counters on this creature, then " +
            "up to that many other target artifacts, creatures, and/or enchantments phase out."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "77"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c67fe04-814f-4051-bd13-7c8ee40a6c08.jpg?1783925630"
        ruling(
            "2021-09-24",
            "After you pay the {1}{U} cost one or more times, a second ability triggers and you " +
                "choose the targets for it. Players may respond to the new ability at that point. As " +
                "that ability resolves, Spectral Adversary gets its +1/+1 counters and the target " +
                "permanents phase out."
        )
    }
}
