package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Solar Transformer
 * {2}
 * Artifact
 * This artifact enters tapped.
 * When this artifact enters, you get {E}{E}{E} (three energy counters).
 * {T}: Add {C}.
 * {T}, Pay {E}: Add one mana of any color.
 */
val SolarTransformer = card("Solar Transformer") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "When this artifact enters, you get {E}{E}{E} (three energy counters).\n" +
        "{T}: Add {C}.\n" +
        "{T}, Pay {E}: Add one mana of any color."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    activatedAbility {
        cost = Costs.Tap
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
        collectorNumber = "211"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b87315b3-d3a3-4eef-8a20-90587b36f551.jpg?1783911243"
    }
}
