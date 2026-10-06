package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clockwork Hydra — Time Spiral #253
 * {5} · Artifact Creature — Hydra · 0/0
 *
 * This creature enters with four +1/+1 counters on it.
 * Whenever this creature attacks or blocks, remove a +1/+1 counter from it. If you do, it deals
 * 1 damage to any target.
 * {T}: Put a +1/+1 counter on this creature.
 *
 * "Attacks or blocks" is two triggered abilities sharing one effect (the Merfolk Skyscout shape);
 * each picks its own target as it goes on the stack. The removal is mandatory, but "if you do"
 * makes the damage depend on it actually happening: an [Effects.If] on the Hydra still having a
 * +1/+1 counter gates remove-then-damage, so a Hydra that has left the battlefield (or has no
 * counter left) deals no damage — exactly the 2021 ruling. An illegal target fizzles the whole
 * ability, so no counter comes off either.
 */
private fun clockworkHydraPing(target: EffectTarget) = Effects.If(
    condition = Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE),
    then = Effects.RemoveCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
        Effects.DealDamage(1, target),
)

val ClockworkHydra = card("Clockwork Hydra") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Hydra"
    power = 0
    toughness = 0
    oracleText = "This creature enters with four +1/+1 counters on it.\n" +
        "Whenever this creature attacks or blocks, remove a +1/+1 counter from it. " +
        "If you do, it deals 1 damage to any target.\n" +
        "{T}: Put a +1/+1 counter on this creature."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 4,
            selfOnly = true,
        )
    )

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(Targets.Any)
        effect = clockworkHydraPing(t)
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        val t = target(Targets.Any)
        effect = clockworkHydraPing(t)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "{T}: Put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54522c50-5333-47ac-ba3e-d87c599404c4.jpg?1783943198"
        ruling(
            "2021-03-19",
            "If Clockwork Hydra leaves the battlefield while its trigger is on the stack, you can't " +
                "remove a +1/+1 counter from it, so it won't deal damage."
        )
        ruling(
            "2021-03-19",
            "If the chosen target is an illegal target by the time Clockwork Hydra's triggered ability " +
                "tries to resolve, the ability doesn't resolve. You don't remove a counter from Clockwork Hydra."
        )
    }
}
