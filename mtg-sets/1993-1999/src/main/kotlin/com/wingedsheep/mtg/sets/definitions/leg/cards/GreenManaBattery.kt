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
 * Green Mana Battery
 * {4}
 * Artifact
 * {2}, {T}: Put a charge counter on this artifact.
 * {T}, Remove any number of charge counters from this artifact: Add {G}, then add an additional
 * {G} for each charge counter removed this way.
 *
 * The mana ability is the storage-land shape (a self-scoped variable counter-removal cost whose X
 * feeds the mana produced) plus the one {G} the battery always makes, so X = 0 still yields {G}.
 */
val GreenManaBattery = card("Green Mana Battery") {
    manaCost = "{4}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Put a charge counter on this artifact.\n" +
        "{T}, Remove any number of charge counters from this artifact: Add {G}, then add an additional {G} for each charge counter removed this way."

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
        effect = Effects.AddMana(Color.GREEN, DynamicAmounts.xValue() + 1)
        description = "{T}, Remove any number of charge counters from this artifact: Add {G}, then add an additional {G} for each charge counter removed this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "279"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4671fa01-4a9e-4cd9-8154-b0d45e11b702.jpg?1783948028"
    }
}
