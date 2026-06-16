package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Orcish Mechanics
 * {2}{R}
 * Creature — Orc
 * 1/1
 * {T}, Sacrifice an artifact: This creature deals 2 damage to any target.
 */
val OrcishMechanics = card("Orcish Mechanics") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc"
    power = 1
    toughness = 1
    oracleText = "{T}, Sacrifice an artifact: This creature deals 2 damage to any target."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Artifact))
        val t = target("any target", Targets.Any)
        effect = Effects.DealDamage(2, t)
        description = "{T}, Sacrifice an artifact: This creature deals 2 damage to any target."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e34fc6b-5f00-4a22-9ee2-afc1caf99961.jpg?1562914705"
    }
}
