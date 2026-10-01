package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serum Sovereign
 * {4}{U}
 * Creature — Phyrexian Sphinx
 * 4/4
 *
 * Flying
 * Whenever you cast a noncreature spell, put an oil counter on this creature.
 * {U}, Remove an oil counter from this creature: Draw a card, then scry 2.
 */
val SerumSovereign = card("Serum Sovereign") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Phyrexian Sphinx"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you cast a noncreature spell, put an oil counter on this creature.\n" +
        "{U}, Remove an oil counter from this creature: Draw a card, then scry 2."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.RemoveCounterFromSelf(CounterType.OIL, 1))
        effect = Effects.DrawCards(1) then Effects.Scry(2)
        description = "{U}, Remove an oil counter from this creature: Draw a card, then scry 2."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "405"
        artist = "Chris Rallis"
        flavorText = "A thousand riddles, each with a single answer: \"The glory of Phyrexia.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70d562aa-8b49-40a3-a5c1-3af8afa98edc.jpg?1783917920"
    }
}
