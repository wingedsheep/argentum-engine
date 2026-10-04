package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Furnace Skullbomb
 * {1}
 * Artifact
 *
 * {1}, Sacrifice this artifact: Draw a card.
 * {1}{R}, Sacrifice this artifact: Put two oil counters on target artifact or creature you control.
 * Draw a card. Activate only as a sorcery.
 */
val FurnaceSkullbomb = card("Furnace Skullbomb") {
    manaCost = "{1}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "{1}, Sacrifice this artifact: Draw a card.\n" +
        "{1}{R}, Sacrifice this artifact: Put two oil counters on target artifact or creature you control. " +
        "Draw a card. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        val permanent = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature).youControl())
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.SacrificeSelf)
        effect = Effects.AddCounters(CounterType.OIL, 2, permanent) then
            Effects.DrawCards(1)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "228"
        artist = "Matt Forsyth"
        flavorText = "It burns with the passion of an unbound creator."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb530f9d-cf35-48a7-8711-f74b3d9a35a7.jpg?1783917992"
    }
}
