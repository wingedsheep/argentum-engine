package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bristling Hydra — Kaladesh #147
 * {2}{G}{G} · Creature — Hydra · 4/3
 *
 * When this creature enters, you get {E}{E}{E} (three energy counters).
 * Pay {E}{E}{E}: Put a +1/+1 counter on this creature. It gains hexproof until end of turn.
 */
val BristlingHydra = card("Bristling Hydra") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, you get {E}{E}{E} (three energy counters).\n" +
        "Pay {E}{E}{E}: Put a +1/+1 counter on this creature. It gains hexproof until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 3)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self)
        description = "Pay {E}{E}{E}: Put a +1/+1 counter on this creature. It gains hexproof until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bf38096-05bc-4734-b33c-eb1e8f2986de.jpg?1783937182"
    }
}
