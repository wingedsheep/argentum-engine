package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Goblin Grenade
 * {R}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a Goblin.
 * Goblin Grenade deals 5 damage to any target.
 *
 * The sacrifice filter is *permanent* with the Goblin subtype, not creature — per the ruling, a
 * kindred enchantment such as Boggart Shenanigans is a legal sacrifice.
 */
val GoblinGrenade = card("Goblin Grenade") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a Goblin.\nGoblin Grenade deals 5 damage to any target."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Permanent.withSubtype("Goblin")))

    spell {
        val t = target("any target", AnyTarget())
        effect = Effects.DealDamage(5, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56a"
        artist = "Ron Spencer"
        flavorText = "\"According to accepted theory, the Grenade held some kind of flammable mixture and was carried to its target by a hapless Goblin.\"\n—*Sarpadian Empires, vol. IV*"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8837eaba-9602-4f63-9897-85583fcdcf51.jpg?1783947894"
        ruling("2011-09-22", "Players can only respond once Goblin Grenade has been cast and all its costs have been paid. No one can try and destroy the Goblin to prevent you from casting Goblin Grenade.")
        ruling("2011-09-22", "The Goblin you sacrifice to cast Goblin Grenade doesn't have to be a creature. For example, you could sacrifice Boggart Shenanigans (a kindred enchantment with the subtype Goblin).")
        ruling("2004-10-04", "You can't sacrifice more than one Goblin to get a greater effect.")
    }
}
