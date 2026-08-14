package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Claim the Kingdom — Marvel Super Heroes #163
 * {1}{G} · Enchantment — Plan
 *
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on target creature you
 * control and a plan counter on this enchantment.
 * When the fourth plan counter is put on this enchantment, sacrifice it. When you do, put an
 * indestructible counter on target creature you control.
 *
 * Modeling notes:
 *  - "Landfall" is an ability word (flavor only); the trigger itself is [Triggers.LandYouControlEnters].
 *  - "When the **fourth** plan counter is put on this enchantment" composes from existing
 *    vocabulary: a SELF-bound [Triggers.countersPlacedOn] on [Counters.PLAN] gated by
 *    `triggerCondition = `[Conditions.SourceCounterCountAtLeast]`(PLAN, 4)`. The at-least gate is
 *    behaviourally exact here because the payoff **sacrifices its own source**, so the enchantment
 *    is gone before a fifth counter could ever land — the threshold can never fire twice. No
 *    dedicated "Nth counter" trigger event is needed.
 *  - "Sacrifice it. When you do, …" is a mandatory [ReflexiveTriggerEffect] (`optional = false`);
 *    the reflexive ability picks its own target creature as it goes on the stack (CR 603.12).
 *    [Counters.INDESTRUCTIBLE] is a keyword counter (CR 122.1b), granted through the state
 *    projection's keyword-counter map.
 */
val ClaimTheKingdom = card("Claim the Kingdom") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Plan"
    oracleText = "Landfall — Whenever a land you control enters, put a +1/+1 counter on target " +
        "creature you control and a plan counter on this enchantment.\n" +
        "When the fourth plan counter is put on this enchantment, sacrifice it. When you do, put " +
        "an indestructible counter on target creature you control."

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature),
            Effects.AddCounters(Counters.PLAN, 1, EffectTarget.Self),
        )
        description = "Landfall — Whenever a land you control enters, put a +1/+1 counter on " +
            "target creature you control and a plan counter on this enchantment."
    }

    triggeredAbility {
        trigger = Triggers.countersPlacedOn(
            filter = GameObjectFilter.Any,
            counterType = Counters.PLAN,
            firstTimeEachTurn = false,
            binding = TriggerBinding.SELF,
        )
        triggerCondition = Conditions.SourceCounterCountAtLeast(Counters.PLAN, 4)
        effect = ReflexiveTriggerEffect(
            action = Effects.SacrificeTarget(EffectTarget.Self),
            optional = false,
            reflexiveEffect = Effects.AddCounters(
                Counters.INDESTRUCTIBLE,
                1,
                EffectTarget.ContextTarget(0),
            ),
            reflexiveTargetRequirements = listOf(Targets.CreatureYouControl),
            descriptionOverride = "Sacrifice this enchantment. When you do, put an indestructible " +
                "counter on target creature you control.",
        )
        description = "When the fourth plan counter is put on this enchantment, sacrifice it. " +
            "When you do, put an indestructible counter on target creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf13bfb1-5b44-4363-8de9-ece234233870.jpg?1783902921"
    }
}
