package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Molten Vortex
 * {R}
 * Enchantment
 * {R}, Discard a land card: This enchantment deals 2 damage to any target.
 */
val MoltenVortex = card("Molten Vortex") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "{R}, Discard a land card: This enchantment deals 2 damage to any target."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Discard(GameObjectFilter.Land))
        val t = target("target", Targets.Any)
        effect = Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Philip Straub"
        flavorText = "If you can't take the heat . . . well, that's going to be a problem."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d99fd073-c249-4cd2-9d71-be417c88c493.jpg?1783938327"
    }
}
