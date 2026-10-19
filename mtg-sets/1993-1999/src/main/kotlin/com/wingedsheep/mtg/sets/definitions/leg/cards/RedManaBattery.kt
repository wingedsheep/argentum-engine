package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Red Mana Battery
 * {4}
 * Artifact
 * {2}, {T}: Put a charge counter on this artifact.
 * {T}, Remove any number of charge counters from this artifact: Add {R}, then add an additional
 * {R} for each charge counter removed this way.
 *
 * The mana ability is the storage-land shape (a self-scoped variable counter-removal cost whose X
 * feeds the mana produced) plus the one {R} the battery always makes, so X = 0 still yields {R}.
 */
val RedManaBattery = card("Red Mana Battery") {
    manaCost = "{4}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Put a charge counter on this artifact.\n" +
        "{T}, Remove any number of charge counters from this artifact: Add {R}, then add an additional {R} for each charge counter removed this way."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        description = "{2}, {T}: Put a charge counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveXCounters(counterType = CounterType.CHARGE, self = true),
        )
        manaAbility = true
        effect = Effects.AddMana(Color.RED, DynamicAmounts.xValue() + 1)
        description = "{T}, Remove any number of charge counters from this artifact: Add {R}, then add an additional {R} for each charge counter removed this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "291"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/363cc5d6-70f8-4a3c-92bd-8f49774bdce2.jpg?1783948026"
    }
}
