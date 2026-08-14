package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Talion's Messenger
 * {2}{U}
 * Creature — Faerie Noble
 * 1/3
 *
 * Flying
 * Whenever you attack with one or more Faeries, draw a card, then discard a card. When you discard
 * a card this way, put a +1/+1 counter on target Faerie you control.
 *
 * [Triggers.YouAttackWithFilter] fires once per declare-attackers step, not once per attacker —
 * "one or more Faeries" is a batch, so attacking with three Faeries loots once. The Messenger
 * itself is a Faerie and satisfies its own trigger when it attacks.
 *
 * "Draw a card, then discard a card. When you discard a card this way, …" is the exact shape The
 * Ancient One prints: a plain draw followed by a [ReflexiveTriggerEffect] whose *action* is the
 * discard pipeline. `optional = false` because the discard is mandatory, and wrapping it as a
 * reflexive trigger rather than a third composite step is what gets the timing and the fizzle case
 * right — the counter is a genuine trigger that goes on the stack after the loot finishes, its
 * target is chosen then (not at attack time), and if there was nothing to discard the reflexive
 * trigger never fires at all.
 *
 * The target is [TargetPermanent] filtered to the Faerie subtype rather than [TargetFilter.Creature]:
 * the oracle says "target Faerie you control", and Eldraine prints noncreature Faeries. Any Faerie
 * you control is legal, not just one that attacked.
 */
val TalionsMessenger = card("Talion's Messenger") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Noble"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever you attack with one or more Faeries, draw a card, then discard a card. When you " +
        "discard a card this way, put a +1/+1 counter on target Faerie you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(GameObjectFilter.Any.withSubtype("Faerie"))
        effect = Effects.Composite(
            Effects.DrawCards(1, EffectTarget.Controller),
            ReflexiveTriggerEffect(
                action = Patterns.Hand.discardCards(1),
                optional = false,
                reflexiveEffect = Effects.AddCounters(
                    Counters.PLUS_ONE_PLUS_ONE,
                    1,
                    EffectTarget.ContextTarget(0),
                ),
                reflexiveTargetRequirements = listOf(
                    TargetPermanent(
                        filter = TargetFilter.Permanent.withSubtype("Faerie").youControl()
                    )
                ),
            ),
        )
        description = "Whenever you attack with one or more Faeries, draw a card, then discard a " +
            "card. When you discard a card this way, put a +1/+1 counter on target Faerie you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Marta Nael"
        flavorText = "\"The Kindly Lord does not issue invitations to their court lightly. " +
            "I suggest you accept.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35fb0640-5b04-4687-b863-46a8b8d36809.jpg?1783915114"
    }
}
