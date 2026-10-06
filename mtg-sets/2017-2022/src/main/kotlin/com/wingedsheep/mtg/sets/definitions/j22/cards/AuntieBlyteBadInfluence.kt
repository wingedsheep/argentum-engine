package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Auntie Blyte, Bad Influence — Jumpstart 2022 #30
 * {2}{R} · Legendary Creature — Devil Advisor · 2/2
 *
 * Flying
 * Whenever a source you control deals damage to you, put that many +1/+1 counters on Auntie Blyte.
 * {1}{R}, {T}, Remove X +1/+1 counters from Auntie Blyte: It deals X damage to any target.
 *
 * The trigger is the source-restricted damage-to-you trigger (Farsight Mask's shape with
 * `youControl()` in place of `opponentControls()`), firing once per damage instance; "that many" is the
 * triggering damage amount (Sun Droplet's read). Auntie's own ability aimed at you is a source you
 * control, so it feeds itself back. The activated ability is Cruel Sadist's remove-X-from-self cost,
 * which caps X at the +1/+1 counters Auntie currently has.
 */
val AuntieBlyteBadInfluence = card("Auntie Blyte, Bad Influence") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Devil Advisor"
    oracleText = "Flying\n" +
        "Whenever a source you control deals damage to you, put that many +1/+1 counters on Auntie Blyte.\n" +
        "{1}{R}, {T}, Remove X +1/+1 counters from Auntie Blyte: It deals X damage to any target."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.isDealtDamage(GameObjectFilter.Any.youControl())
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.Self,
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}"),
            Costs.Tap,
            Costs.RemoveXCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, self = true),
        )
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), t)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "30"
        artist = "Tatiana Kirgetova"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85c18d01-62cd-45c2-94b8-a63a4239311d.jpg?1783919186"
    }
}
