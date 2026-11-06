package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aethertorch Renegade — Kaladesh #106
 * {2}{R} · Creature — Human Rogue · 1/2
 *
 * When this creature enters, you get {E}{E}{E}{E} (four energy counters).
 * {T}, Pay {E}{E}: This creature deals 1 damage to target creature.
 * {T}, Pay eight {E}: This creature deals 6 damage to target player or planeswalker.
 */
val AethertorchRenegade = card("Aethertorch Renegade") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, you get {E}{E}{E}{E} (four energy counters).\n" +
        "{T}, Pay {E}{E}: This creature deals 1 damage to target creature.\n" +
        "{T}, Pay eight {E}: This creature deals 6 damage to target player or planeswalker."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(4)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 2))
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(1, t)
        description = "{T}, Pay {E}{E}: This creature deals 1 damage to target creature."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 8))
        val t = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(6, t)
        description = "{T}, Pay eight {E}: This creature deals 6 damage to target player or planeswalker."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5c443e1-fd13-4627-85a9-8e9a340a2786.jpg?1783937198"
    }
}
