package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fasting
 * {W}
 * Enchantment
 * At the beginning of your upkeep, put a hunger counter on this enchantment. Then destroy this
 * enchantment if it has five or more hunger counters on it.
 * If you would begin your draw step, you may skip that step instead. If you do, you gain 2 life.
 * When you draw a card, destroy this enchantment.
 *
 * The counter clock and the draw trigger are ordinary. The middle clause is the one worth reading
 * carefully.
 *
 * **Known simplification.** The printed clause is a replacement on *beginning the draw step*, so
 * the choice is made at that moment. Here it is offered one step earlier, as an optional upkeep
 * trigger that arms `SkipNextDrawStep` and gains the 2 life. Nothing can happen between the two
 * points that changes the decision — the only thing separating a player's upkeep from their draw
 * step is priority passes, and the life gain lands in the same turn either way — but the divergence
 * is real and is written down rather than hidden: an effect that cares *when* the 2 life arrived,
 * or one that removes Fasting with the trigger on the stack, would notice.
 *
 * Modelling it properly needs the draw step itself to be able to stop and ask, which is a
 * draw-phase feature rather than a card; this is the shape to replace when a second card wants it.
 */
val Fasting = card("Fasting") {
    manaCost = "{W}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, put a hunger counter on this enchantment. Then " +
        "destroy this enchantment if it has five or more hunger counters on it.\nIf you would " +
        "begin your draw step, you may skip that step instead. If you do, you gain 2 life.\n" +
        "When you draw a card, destroy this enchantment."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.Composite(
            Effects.AddCounters(Counters.HUNGER, 1, EffectTarget.Self),
            // Checked after the counter goes on, so the fifth upkeep is the last one.
            ConditionalEffect(
                condition = Conditions.SourceCounterCountAtLeast(Counters.HUNGER, 5),
                effect = Effects.Destroy(EffectTarget.Self),
            ),
        )
        description = "At the beginning of your upkeep, put a hunger counter on this enchantment. " +
            "Then destroy this enchantment if it has five or more hunger counters on it."
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        optional = true
        effect = Effects.Composite(
            Effects.SkipNextDrawStep(EffectTarget.Controller),
            Effects.GainLife(2),
        )
        description = "If you would begin your draw step, you may skip that step instead. If you " +
            "do, you gain 2 life."
    }

    triggeredAbility {
        trigger = Triggers.YouDraw
        effect = Effects.Destroy(EffectTarget.Self)
        description = "When you draw a card, destroy this enchantment."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8da35f9f-e72c-4154-a212-7de98f84ad7d.jpg?1783947948"
    }
}
