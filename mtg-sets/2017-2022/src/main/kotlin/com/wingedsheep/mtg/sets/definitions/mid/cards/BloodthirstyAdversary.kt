package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Bloodthirsty Adversary — Innistrad: Midnight Hunt #129
 * {1}{R} · Creature — Vampire · 2/2
 *
 * Haste
 * When this creature enters, you may pay {2}{R} any number of times. When you pay this cost one
 * or more times, put that many +1/+1 counters on this creature, then exile up to that many target
 * instant and/or sorcery cards with mana value 3 or less from your graveyard and copy them. You
 * may cast any number of the copies without paying their mana costs.
 *
 * The repeated payment is [Effects.PayRepeatedly] under an optional reflexive trigger (CR 603.12),
 * as on the other MID Adversaries: the reflexive half's graveyard targets are chosen after the
 * payment, capped by [DynamicAmounts.timesPaid]. On resolution the Adversary gets its counters,
 * then the still-legal targets are exiled, copied in exile, and any number of the copies may be
 * cast for free during the resolution (Arcane Bombardment's tail). The originals stay in exile;
 * uncast copies cease to exist (CR 707.10a SBA).
 */
val BloodthirstyAdversary = card("Bloodthirsty Adversary") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "When this creature enters, you may pay {2}{R} any number of times. When you pay this cost " +
        "one or more times, put that many +1/+1 counters on this creature, then exile up to that " +
        "many target instant and/or sorcery cards with mana value 3 or less from your graveyard and " +
        "copy them. You may cast any number of the copies without paying their mana costs."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayRepeatedly("{2}{R}"),
            optional = true,
            reflexiveEffect = Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.timesPaid(),
                EffectTarget.Self
            ) then Effects.Pipeline {
                val targeted = gather(CardSource.ChosenTargets)
                exile(targeted)
                val copies = copyCards(targeted)
                run(Effects.CastAnyNumberFromCollectionWithoutPayingCost(from = copies))
            },
            reflexiveTargetRequirements = listOf(
                TargetObject(
                    filter = TargetFilter.InstantOrSorceryInYourGraveyard.manaValueAtMost(3),
                    optional = true,
                    dynamicMaxCount = DynamicAmounts.timesPaid()
                )
            ),
            descriptionOverride = "You may pay {2}{R} any number of times. When you pay this cost " +
                "one or more times, put that many +1/+1 counters on this creature, then exile up to " +
                "that many target instant and/or sorcery cards with mana value 3 or less from your " +
                "graveyard and copy them. You may cast any number of the copies without paying " +
                "their mana costs."
        )
        description = "When this creature enters, you may pay {2}{R} any number of times. When you " +
            "pay this cost one or more times, put that many +1/+1 counters on this creature, then " +
            "exile up to that many target instant and/or sorcery cards with mana value 3 or less " +
            "from your graveyard and copy them. You may cast any number of the copies without " +
            "paying their mana costs."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "129"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed557e51-32bd-476f-99f8-d7b1738495c5.jpg?1783925605"
        ruling(
            "2021-09-24",
            "Whether or not you cast the copies this way, the original cards will remain in exile."
        )
        ruling(
            "2021-09-24",
            "Copies cast this way are cast during the resolution of the ability. You don't have to " +
                "follow normal timing requirements and you can't wait and cast them later."
        )
        ruling(
            "2021-09-24",
            "After you pay the {2}{R} cost one or more times, a second ability triggers and you " +
                "choose the targets for it. Players may respond to the new ability at that point. As " +
                "that ability resolves, Bloodthirsty Adversary gets its counters and the target cards " +
                "are exiled."
        )
    }
}
