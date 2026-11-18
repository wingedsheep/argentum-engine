package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Font of Ire
 * {1}{R}
 * Enchantment
 *
 * {3}{R}, Sacrifice this enchantment: It deals 5 damage to target player or planeswalker.
 *
 * "It" is the sacrificed Font: the ability's source deals the damage, using its last known
 * information once it has left the battlefield.
 */
val FontOfIre = card("Font of Ire") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "{3}{R}, Sacrifice this enchantment: It deals 5 damage to target player or planeswalker."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{R}"), Costs.SacrificeSelf)
        val t = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(5, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Zack Stella"
        flavorText = "Drink deep, and find your vengeance fulfilled."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41509414-9faf-472d-8d38-391d9594cd56.jpg?1783939424"
    }
}
