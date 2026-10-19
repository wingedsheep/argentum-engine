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
 * Black Mana Battery
 * {4}
 * Artifact
 * {2}, {T}: Put a charge counter on this artifact.
 * {T}, Remove any number of charge counters from this artifact: Add {B}, then add an additional
 * {B} for each charge counter removed this way.
 *
 * The mana ability is the storage-land shape (a self-scoped variable counter-removal cost whose X
 * feeds the mana produced) plus the one {B} the battery always makes, so X = 0 still yields {B}.
 */
val BlackManaBattery = card("Black Mana Battery") {
    manaCost = "{4}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Put a charge counter on this artifact.\n" +
        "{T}, Remove any number of charge counters from this artifact: Add {B}, then add an additional {B} for each charge counter removed this way."

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
        effect = Effects.AddMana(Color.BLACK, DynamicAmounts.xValue() + 1)
        description = "{T}, Remove any number of charge counters from this artifact: Add {B}, then add an additional {B} for each charge counter removed this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "274"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0c66e64-e357-457d-8302-b3a1fc0c56ce.jpg?1783948029"
    }
}
