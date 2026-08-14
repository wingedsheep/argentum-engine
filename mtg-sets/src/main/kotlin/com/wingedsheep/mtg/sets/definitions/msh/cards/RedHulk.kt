package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Red Hulk — Marvel Super Heroes #149
 * {4}{R}{R} · Legendary Creature — Gamma Berserker Villain · 6/7
 *
 * Reach, trample
 * Enrage — Whenever Red Hulk is dealt damage, put a +1/+1 counter on him. When you do, he deals
 * damage equal to the number of +1/+1 counters on him to any other target.
 *
 * Implementation notes:
 *  - "Enrage" is an ability word (italic flavor, no rules meaning), so it lives in the trigger's
 *    description rather than as a keyword — the Raphael, Ninja Destroyer precedent.
 *    [Triggers.TakesDamage] is the SELF-bound "is dealt damage" trigger, any source, combat or not.
 *  - "put a +1/+1 counter on him. When you do, …" is a [ReflexiveTriggerEffect] with
 *    `optional = false`: the counter is mandatory, and placing it creates a reflexive triggered
 *    ability (CR 603.12) that chooses its target as it goes on the stack. That ordering matters —
 *    the reflexive ability's damage is counted *after* the new counter is on, so a first hit on an
 *    otherwise-uncountered Red Hulk deals 1.
 *  - The damage amount is [DynamicAmounts.countersOnSelf] over
 *    [CounterTypeFilter.PlusOnePlusOne] read at resolution (CR 608.2), not a snapshot, so counters
 *    added in response are included.
 *  - "any other target" is [TargetOther] wrapping [AnyTarget] — the Screaming Nemesis idiom; Red
 *    Hulk can't ping himself into an infinite Enrage loop.
 */
val RedHulk = card("Red Hulk") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Gamma Berserker Villain"
    power = 6
    toughness = 7
    oracleText = "Reach, trample\n" +
        "Enrage — Whenever Red Hulk is dealt damage, put a +1/+1 counter on him. When you do, he " +
        "deals damage equal to the number of +1/+1 counters on him to any other target."

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.TakesDamage
        effect = ReflexiveTriggerEffect(
            // "put a +1/+1 counter on him"
            action = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            optional = false,
            // "When you do, he deals damage equal to the number of +1/+1 counters on him to any
            //  other target."
            reflexiveEffect = Effects.DealDamage(
                amount = DynamicAmounts.countersOnSelf(CounterTypeFilter.PlusOnePlusOne),
                target = EffectTarget.ContextTarget(0),
                damageSource = EffectTarget.Self,
            ),
            reflexiveTargetRequirements = listOf(TargetOther(AnyTarget())),
            descriptionOverride = "Put a +1/+1 counter on him. When you do, he deals damage equal " +
                "to the number of +1/+1 counters on him to any other target.",
        )
        description = "Enrage — Whenever Red Hulk is dealt damage, put a +1/+1 counter on him. " +
            "When you do, he deals damage equal to the number of +1/+1 counters on him to any " +
            "other target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Vilhelmas Banys"
        flavorText = "To finally defeat the Hulk, General \"Thunderbolt\" Ross became what he " +
            "hated most."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e25468e2-17c6-47b2-8eb6-fcb8ae6f4c17.jpg?1783902924"
    }
}
