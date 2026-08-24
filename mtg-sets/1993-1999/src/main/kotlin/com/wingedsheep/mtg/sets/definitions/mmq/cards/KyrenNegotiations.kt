package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kyren Negotiations
 * {2}{R}{R}
 * Enchantment
 */
val KyrenNegotiations = card("Kyren Negotiations") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Tap an untapped creature you control: This enchantment deals 1 damage to target player or planeswalker."

    activatedAbility {
        cost = Costs.TapPermanents(count = 1, filter = GameObjectFilter.Creature)
        val victim = target("target", Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(1, victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Scott Hampton"
        flavorText = "Kyren goblins always speak in questions and never allow wrong answers."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c263a17-bbc2-433e-93f8-72e57b818322.jpg"
    }
}
