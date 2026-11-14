package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mage-Ring Network
 * Land
 * {T}: Add {C}.
 * {1}, {T}: Put a storage counter on this land.
 * {T}, Remove any number of storage counters from this land: Add {C} for each storage counter
 * removed this way.
 *
 * The storage-land shape (Bottomless Vault, Red Mana Battery): the last ability is a mana ability
 * whose self-scoped variable counter-removal cost feeds its X back into the colorless mana produced.
 */
val MageRingNetwork = card("Mage-Ring Network") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}: Put a storage counter on this land.\n" +
        "{T}, Remove any number of storage counters from this land: Add {C} for each storage counter removed this way."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.STORAGE, 1, EffectTarget.Self)
        description = "{1}, {T}: Put a storage counter on this land."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveXCounters(counterType = CounterType.STORAGE, self = true),
        )
        manaAbility = true
        effect = Effects.AddColorlessMana(DynamicAmounts.xValue())
        description = "{T}, Remove any number of storage counters from this land: Add {C} for each storage counter removed this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Jung Park"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/4988180b-a374-43fd-b299-fb61c781df2e.jpg?1783938306"
    }
}
