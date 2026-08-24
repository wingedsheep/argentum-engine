package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Goblin Warrens
 * {2}{R}
 * Enchantment
 * {2}{R}, Sacrifice two Goblins: Create three 1/1 red Goblin creature tokens.
 *
 * The Goblin art is registered on [com.wingedsheep.mtg.sets.definitions.fem.FallenEmpiresSet].
 */
val GoblinWarrens = card("Goblin Warrens") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "{2}{R}, Sacrifice two Goblins: Create three 1/1 red Goblin creature tokens."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{R}"),
            Costs.SacrificeMultiple(2, GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN))
        )
        effect = Effects.CreateToken(
            count = 3,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Dan Frazier"
        flavorText = "\"Goblins bred underground, their numbers hidden from the enemy until it was too late.\"\n—*Sarpadian Empires, vol. IV*"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbec4aa5-3319-43dc-8347-5633edbd7018.jpg?1783947892"
    }
}
