package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sage of Hours — Journey into Nyx #50 (canonical printing)
 * {1}{U}
 * Creature — Human Wizard
 * 1/1
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on it.
 * Remove all +1/+1 counters from this creature: For each five counters removed this way, take an
 * extra turn after this one.
 *
 * "For each five counters removed this way" is the cost-removed count divided by five, rounded
 * down (twelve counters → two extra turns, three → none), repeating the single extra turn.
 */
val SageOfHours = card("Sage of Hours") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on it.\n" +
        "Remove all +1/+1 counters from this creature: For each five counters removed this way, take an extra turn after this one."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on it."
    }

    activatedAbility {
        cost = Costs.RemoveAllCountersFromSelf(CounterType.PLUS_ONE_PLUS_ONE)
        effect = Effects.Repeat(DynamicAmounts.countersRemovedAsCost() / 5, Effects.TakeExtraTurn())
        description = "Remove all +1/+1 counters from this creature: For each five counters removed this way, " +
            "take an extra turn after this one."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "50"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cb55caf-fc95-40dc-80af-5144f94333ee.jpg?1783939443"
        ruling("2014-04-26", "You must remove all +1/+1 counters from Sage of Hours to activate its ability, even if the number of counters isn't a multiple of five. For example, if you remove twelve counters to activate the ability, you'll take two extra turns. If you remove three counters, you won't take any extra turns.")
        ruling("2014-04-26", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2014-04-26", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2014-04-26", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
