package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Consulate Turret
 * {3}
 * Artifact
 * {T}: You get {E} (an energy counter).
 * {T}, Pay {E}{E}{E}: This artifact deals 2 damage to target player or planeswalker.
 */
val ConsulateTurret = card("Consulate Turret") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: You get {E} (an energy counter).\n" +
        "{T}, Pay {E}{E}{E}: This artifact deals 2 damage to target player or planeswalker."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.GetEnergy(1)
        description = "{T}: You get {E}."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 3))
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(2, victim)
        description = "{T}, Pay {E}{E}{E}: This artifact deals 2 damage to target player or planeswalker."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Eric Deschamps"
        flavorText = "\"Our watchtowers are well positioned. Why don't we arm them?\"\n—Enforcer-Chief Ranaj"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0c55b5b-58fd-4340-b089-8e271633e8dd.jpg?1783936730"
    }
}
