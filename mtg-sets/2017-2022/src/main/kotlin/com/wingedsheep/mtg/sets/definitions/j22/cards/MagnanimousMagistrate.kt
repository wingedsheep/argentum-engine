package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Magnanimous Magistrate {5}{W}
 * Creature — Human Advisor
 * 3/4
 *
 * This creature enters with five reprieve counters on it.
 * Whenever another nontoken creature you control dies, if its mana value was 1 or greater, you
 * may remove that many reprieve counters from this creature. If you do, return that card to the
 * battlefield under its owner's control.
 *
 * The removal is the cost of a `MayPay` gate, so it is all-or-nothing: with fewer reprieve
 * counters than the dead creature's mana value, no "yes" is offered and nothing returns (the
 * card's ruling). The mana value is the card's, read in the graveyard — the same number it had
 * on the battlefield, since a creature's mana value comes from its printed cost.
 */
val MagnanimousMagistrate = card("Magnanimous Magistrate") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Advisor"
    power = 3
    toughness = 4
    oracleText = "This creature enters with five reprieve counters on it.\n" +
        "Whenever another nontoken creature you control dies, if its mana value was 1 or greater, " +
        "you may remove that many reprieve counters from this creature. If you do, return that card " +
        "to the battlefield under its owner's control."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.REPRIEVE,
            count = 5,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.nontoken().youControl()).dies()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.triggeringManaValue(), ComparisonOperator.GTE, 1
        )
        effect = Effects.MayPay(
            cost = Effects.RemoveCounters(
                CounterType.REPRIEVE, DynamicAmounts.triggeringManaValue(), EffectTarget.Self
            ),
            then = Effects.Move(EffectTarget.TriggeringEntity, Zone.BATTLEFIELD)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Marie Magny"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6649e6c-bbd8-43db-9f3f-a24b32aed4e4.jpg?1783919194"
    }
}
