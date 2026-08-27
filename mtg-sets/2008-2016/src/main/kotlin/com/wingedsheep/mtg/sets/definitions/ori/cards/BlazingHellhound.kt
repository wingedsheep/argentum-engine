package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Blazing Hellhound
 * {2}{B}{R}
 * Creature — Elemental Dog
 * 4/3
 * {1}, Sacrifice another creature: This creature deals 1 damage to any target.
 */
val BlazingHellhound = card("Blazing Hellhound") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Elemental Dog"
    power = 4
    toughness = 3
    oracleText = "{1}, Sacrifice another creature: This creature deals 1 damage to any target."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        val t = target("target", Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Eric Velhagen"
        flavorText = "It tears the flesh from your bones and then swallows the ash with its fiery maw."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/332769b1-1eb5-4c77-8317-27addc28650b.jpg?1783938315"
    }
}
