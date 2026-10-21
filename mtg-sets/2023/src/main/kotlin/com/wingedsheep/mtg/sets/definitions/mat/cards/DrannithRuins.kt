package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Drannith Ruins
 *
 * Land
 *
 * {T}: Add {C}.
 * {2}, {T}: Put two +1/+1 counters on target non-Human creature that entered this turn.
 */
val DrannithRuins = card("Drannith Ruins") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{2}, {T}: Put two +1/+1 counters on target non-Human creature that entered this turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.notSubtype(Subtype("Human")).enteredThisTurn())
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Martin de Diego Sádaba"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c07dda8-06dd-499a-9825-dc6b9a73e455.jpg?1783916506"
    }
}
