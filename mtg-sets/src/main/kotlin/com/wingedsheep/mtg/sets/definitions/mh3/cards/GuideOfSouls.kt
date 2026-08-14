package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Guide of Souls
 * {W}
 * Creature — Human Cleric
 * 1/2
 *
 * Whenever another creature you control enters, you gain 1 life and get {E} (an energy counter).
 * Whenever you attack, you may pay {E}{E}{E}. When you do, put two +1/+1 counters and a flying
 * counter on target attacking creature. It becomes an Angel in addition to its other types.
 *
 * The second ability is a genuine reflexive trigger (CR 603.12, per the 2024-06-07 ruling): the
 * "may pay {E}{E}{E}" isn't targeted itself — a *second* triggered ability fires when the payment
 * is made, and *that* ability chooses the attacking creature, so opponents get a response window
 * between the payment and the target lock. Modeled with [ReflexiveTriggerEffect] rather than a
 * [com.wingedsheep.sdk.scripting.effects.GatedEffect]/`Gate.MayPay` ("If you do") gate, which
 * would target at trigger time instead. [Effects.PayFixedCounters] is the all-or-nothing action
 * half — paying a partial amount for a partial effect isn't legal (ruling), so it fails outright
 * rather than clamping, and [com.wingedsheep.engine.handlers.effects.composite.ReflexiveTriggerEffectExecutor.isActionFeasible]
 * checks the energy total before ever offering the "may pay" prompt. The Angel type change uses
 * [Effects.AddCreatureType]'s default `Duration.Permanent` — per ruling it lasts indefinitely,
 * unaffected by cleanup or Guide of Souls leaving the battlefield.
 */
val GuideOfSouls = card("Guide of Souls") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 2
    oracleText = "Whenever another creature you control enters, you gain 1 life and get {E} (an " +
        "energy counter).\nWhenever you attack, you may pay {E}{E}{E}. When you do, put two " +
        "+1/+1 counters and a flying counter on target attacking creature. It becomes an Angel " +
        "in addition to its other types."

    triggeredAbility {
        trigger = Triggers.OtherCreatureEnters
        effect = Effects.GainLife(1).then(Effects.GetEnergy(1))
    }

    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = ReflexiveTriggerEffect(
            action = Effects.PayFixedCounters(Counters.ENERGY, 3),
            optional = true,
            reflexiveEffect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.ContextTarget(0))
                .then(Effects.AddCounters(Counters.FLYING, 1, EffectTarget.ContextTarget(0)))
                .then(Effects.AddCreatureType("Angel", EffectTarget.ContextTarget(0))),
            reflexiveTargetRequirements = listOf(Targets.AttackingCreature),
            descriptionOverride = "You may pay {E}{E}{E}. When you do, put two +1/+1 counters " +
                "and a flying counter on target attacking creature. It becomes an Angel in " +
                "addition to its other types."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Ryan Valle"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76c3cad2-1e25-4abe-878d-9194de6fcc27.jpg?1783911300"

        ruling(
            "2024-06-07",
            "You don't choose a target for Guide of Souls's last ability at the time it " +
                "triggers. Rather, a second \"reflexive\" ability triggers when you pay " +
                "{E}{E}{E} this way. You choose a target for that ability as it goes on the " +
                "stack. Each player may respond to this triggered ability as normal."
        )
        ruling(
            "2024-06-07",
            "The type-changing effect of the reflexive triggered ability lasts indefinitely. " +
                "It doesn't wear off during the cleanup step or when Guide of Souls leaves the " +
                "battlefield."
        )
        ruling(
            "2024-06-07",
            "Some triggered abilities state that you \"may pay\" a certain amount of {E}. You " +
                "can't pay that amount multiple times to multiply the effect. You simply " +
                "choose whether or not to pay that amount of {E} as the ability resolves."
        )
    }
}
