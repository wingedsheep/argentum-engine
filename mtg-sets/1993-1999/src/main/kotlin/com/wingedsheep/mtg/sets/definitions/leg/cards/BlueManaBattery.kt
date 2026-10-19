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
 * Blue Mana Battery
 * {4}
 * Artifact
 * {2}, {T}: Put a charge counter on this artifact.
 * {T}, Remove any number of charge counters from this artifact: Add {U}, then add an additional
 * {U} for each charge counter removed this way.
 *
 * The mana ability is the storage-land shape (a self-scoped variable counter-removal cost whose X
 * feeds the mana produced) plus the one {U} the battery always makes, so X = 0 still yields {U}.
 */
val BlueManaBattery = card("Blue Mana Battery") {
    manaCost = "{4}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Put a charge counter on this artifact.\n" +
        "{T}, Remove any number of charge counters from this artifact: Add {U}, then add an additional {U} for each charge counter removed this way."

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
        effect = Effects.AddMana(Color.BLUE, DynamicAmounts.xValue() + 1)
        description = "{T}, Remove any number of charge counters from this artifact: Add {U}, then add an additional {U} for each charge counter removed this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "275"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35393661-2c53-46f0-bb33-2390d552b060.jpg?1783948029"
    }
}
