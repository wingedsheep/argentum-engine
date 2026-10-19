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
 * White Mana Battery
 * {4}
 * Artifact
 * {2}, {T}: Put a charge counter on this artifact.
 * {T}, Remove any number of charge counters from this artifact: Add {W}, then add an additional
 * {W} for each charge counter removed this way.
 *
 * The mana ability is the storage-land shape (a self-scoped variable counter-removal cost whose X
 * feeds the mana produced) plus the one {W} the battery always makes, so X = 0 still yields {W}.
 */
val WhiteManaBattery = card("White Mana Battery") {
    manaCost = "{4}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Put a charge counter on this artifact.\n" +
        "{T}, Remove any number of charge counters from this artifact: Add {W}, then add an additional {W} for each charge counter removed this way."

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
        effect = Effects.AddMana(Color.WHITE, DynamicAmounts.xValue() + 1)
        description = "{T}, Remove any number of charge counters from this artifact: Add {W}, then add an additional {W} for each charge counter removed this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "299"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35fbbe41-d21b-4028-905f-054c44d30eb2.jpg?1783948024"
    }
}
