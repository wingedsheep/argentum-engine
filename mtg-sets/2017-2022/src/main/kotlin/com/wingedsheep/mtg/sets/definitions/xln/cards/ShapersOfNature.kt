package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Shapers of Nature
 * {1}{G}{U}
 * Creature — Merfolk Shaman
 * 3/3
 *
 * {3}{G}: Put a +1/+1 counter on target creature.
 * {2}{U}, Remove a +1/+1 counter from a creature you control: Draw a card.
 */
val ShapersOfNature = card("Shapers of Nature") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Merfolk Shaman"
    power = 3
    toughness = 3
    oracleText = "{3}{G}: Put a +1/+1 counter on target creature.\n" +
        "{2}{U}, Remove a +1/+1 counter from a creature you control: Draw a card."

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
        description = "Put a +1/+1 counter on target creature."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{U}"),
            Costs.RemovePlusOnePlusOneCounters(GameObjectFilter.Creature.youControl(), 1)
        )
        effect = Effects.DrawCards(1)
        description = "Draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Chris Seaman"
        flavorText = "Shapers adapt nature to fit their needs and then return it to the way it was, " +
            "leaving no trace of their passing."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7aa682fb-f5b6-41c1-9428-6043b0b76744.jpg?1783935710"
    }
}
