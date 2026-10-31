package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jace's Projection
 * {2}{U}{U}
 * Creature — Wizard Illusion
 * 2/2
 * Whenever you draw a card, put a +1/+1 counter on this creature.
 * {3}{U}: Put a loyalty counter on target Jace planeswalker.
 */
val JacesProjection = card("Jace's Projection") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Wizard Illusion"
    power = 2
    toughness = 2
    oracleText = "Whenever you draw a card, put a +1/+1 counter on this creature.\n" +
        "{3}{U}: Put a loyalty counter on target Jace planeswalker."

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        val jace = target(TargetFilter.Planeswalker.withSubtype("Jace"))
        effect = Effects.AddCounters(CounterType.LOYALTY, 1, jace)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "272"
        artist = "Darek Zabrocki"
        flavorText = "\"Pardon me if I seem distracted. Part of me is reconnoitering the Tenth District.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4d35a34-01b7-41e1-8491-a6589175d027.jpg?1783933359"
    }
}
