package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * HELIOS One — Fallout #149
 * Land · Rare
 *
 * {T}: Add {C}.
 * {1}, {T}: You get {E} (an energy counter).
 * {3}, {T}, Pay X {E}, Sacrifice this land: Destroy target nonland permanent with mana value X.
 * Activate only as a sorcery.
 *
 * "Pay X {E}" is a [Costs.PayPlayerCounters] over [DynamicAmounts.xValue], so X is announced at
 * activation (capped by your energy) and the target's mana value must equal it — the same shape
 * as Chthonian Nightmare.
 */
val HeliosOne = card("HELIOS One") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}: You get {E} (an energy counter).\n" +
        "{3}, {T}, Pay X {E}, Sacrifice this land: Destroy target nonland permanent with mana value X. " +
        "Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.PayPlayerCounters(CounterType.ENERGY, DynamicAmounts.xValue()),
            Costs.SacrificeSelf,
        )
        timing = TimingRule.SorcerySpeed
        val permanent = target(TargetFilter.NonlandPermanent.manaValueEqualsX())
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "149"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4102d28e-437b-440b-bf9b-8b4f6fb85a6c.jpg?1783912373"
        ruling("2024-03-08", "If a permanent has {X} in its mana cost, X is 0 when determining its mana value.")
        ruling(
            "2024-03-08",
            "A token that's not a copy of something else normally won't have a mana cost. Anything " +
                "without a mana cost normally has a mana value of 0."
        )
    }
}
