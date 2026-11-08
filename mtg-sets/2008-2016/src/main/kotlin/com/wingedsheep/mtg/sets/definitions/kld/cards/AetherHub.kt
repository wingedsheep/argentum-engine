package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Aether Hub — Kaladesh #242
 * Land
 *
 * When this land enters, you get {E} (an energy counter).
 * {T}: Add {C}.
 * {T}, Pay {E}: Add one mana of any color.
 *
 * The energy mana ability is the Servant of the Conduit shape.
 */
val AetherHub = card("Aether Hub") {
    typeLine = "Land"
    oracleText = "When this land enters, you get {E} (an energy counter).\n" +
        "{T}: Add {C}.\n" +
        "{T}, Pay {E}: Add one mana of any color."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 1))
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "242"
        artist = "Sam Burley"
        flavorText = "It powers the dreams of Ghirapur."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25ea04d8-5d85-49d3-8d8d-7fe123d0ed6c.jpg?1783937144"
    }
}
