package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Political Triumph — Marvel Super Heroes #31
 * {W} · Enchantment — Plan
 *
 * Whenever a creature you control enters, scry 1 and put a plan counter on this enchantment.
 * When the fourth plan counter is put on this enchantment, sacrifice it, draw a card, and put a
 * +1/+1 counter on each creature you control.
 *
 * Modeling notes:
 *  - The accumulator is an ANY-bound enters trigger over `GameObjectFilter.Creature.youControl()`
 *    (the enchantment itself is not a creature, so no OTHER binding is needed).
 *  - "When the **fourth** plan counter is put on this enchantment" composes from existing
 *    vocabulary: a SELF-bound [Triggers.countersPlacedOn] on [Counters.PLAN] gated by
 *    `triggerCondition = `[Conditions.SourceCounterCountAtLeast]`(PLAN, 4)`. The at-least gate is
 *    behaviourally exact for this cycle because the payoff **sacrifices its own source**, so the
 *    enchantment is gone before a fifth counter could ever land — the threshold can never fire
 *    twice. No dedicated "Nth counter" trigger event is needed.
 *  - "A +1/+1 counter on each creature you control" is [Effects.ForEachInGroup] over a
 *    [GroupFilter] with the counter applied to `EffectTarget.Self` per iterated creature (the
 *    Cathars' Crusade shape) — not a target.
 */
val PoliticalTriumph = card("Political Triumph") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Plan"
    oracleText = "Whenever a creature you control enters, scry 1 and put a plan counter on this " +
        "enchantment.\n" +
        "When the fourth plan counter is put on this enchantment, sacrifice it, draw a card, and " +
        "put a +1/+1 counter on each creature you control."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.Composite(
            Effects.Scry(1),
            Effects.AddCounters(Counters.PLAN, 1, EffectTarget.Self),
        )
        description = "Whenever a creature you control enters, scry 1 and put a plan counter on " +
            "this enchantment."
    }

    triggeredAbility {
        trigger = Triggers.countersPlacedOn(
            filter = GameObjectFilter.Any,
            counterType = Counters.PLAN,
            firstTimeEachTurn = false,
            binding = TriggerBinding.SELF,
        )
        triggerCondition = Conditions.SourceCounterCountAtLeast(Counters.PLAN, 4)
        effect = Effects.Composite(
            Effects.SacrificeTarget(EffectTarget.Self),
            Effects.DrawCards(1),
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl()),
                AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            ),
        )
        description = "When the fourth plan counter is put on this enchantment, sacrifice it, " +
            "draw a card, and put a +1/+1 counter on each creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Monztre"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/dec3dd36-b8ca-432b-8973-d37c6efc4c1a.jpg?1783902968"
    }
}
