package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Rewrite History — Marvel Super Heroes #71
 * {2}{U} · Enchantment — Plan
 *
 * Whenever one or more creatures you control become tapped, draw a card, then discard a card and
 * put a plan counter on this enchantment.
 * When the fourth plan counter is put on this enchantment, sacrifice it. When you do, return up to
 * two target instant and/or sorcery cards from your graveyard to your hand.
 *
 * Modeling notes:
 *  - "One or more creatures you control become tapped" is the batching [Triggers.OneOrMoreBecomeTapped]
 *    (CR 603.2c): tapping several creatures at once (attacking, convoke, crew) fires it exactly once.
 *  - "When the **fourth** plan counter is put on this enchantment" composes from existing
 *    vocabulary: a SELF-bound [Triggers.countersPlacedOn] on [Counters.PLAN] gated by
 *    `triggerCondition = `[Conditions.SourceCounterCountAtLeast]`(PLAN, 4)`. The at-least gate is
 *    behaviourally exact here because the payoff **sacrifices its own source**, so the enchantment
 *    is gone before a fifth counter could ever land — the threshold can never fire twice. No
 *    dedicated "Nth counter" trigger event is needed.
 *  - "Sacrifice it. When you do, …" is a mandatory [ReflexiveTriggerEffect] (`optional = false`):
 *    the sacrifice is the action and the return is a genuine second stack object (CR 603.12) whose
 *    up-to-two targets are chosen as it goes on the stack.
 */
val RewriteHistory = card("Rewrite History") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Plan"
    oracleText = "Whenever one or more creatures you control become tapped, draw a card, then " +
        "discard a card and put a plan counter on this enchantment.\n" +
        "When the fourth plan counter is put on this enchantment, sacrifice it. When you do, " +
        "return up to two target instant and/or sorcery cards from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.OneOrMoreBecomeTapped(GameObjectFilter.Creature.youControl())
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.Discard(1),
            Effects.AddCounters(Counters.PLAN, 1, EffectTarget.Self),
        )
        description = "Whenever one or more creatures you control become tapped, draw a card, " +
            "then discard a card and put a plan counter on this enchantment."
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
            reflexiveEffect = ForEachTargetEffect(
                effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)),
            ),
            reflexiveTargetRequirements = listOf(
                TargetObject(
                    count = 2,
                    optional = true,
                    filter = TargetFilter.InstantOrSorceryInYourGraveyard,
                ),
            ),
            descriptionOverride = "Sacrifice this enchantment. When you do, return up to two " +
                "target instant and/or sorcery cards from your graveyard to your hand.",
        )
        description = "When the fourth plan counter is put on this enchantment, sacrifice it. " +
            "When you do, return up to two target instant and/or sorcery cards from your " +
            "graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Allen Douglas"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91fdf444-3f41-4b8b-b9f5-f3ae3d903649.jpg?1783902952"
    }
}
