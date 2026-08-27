package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ghirapur Aether Grid
 * {2}{R}
 * Enchantment
 * Tap two untapped artifacts you control: This enchantment deals 1 damage to any target.
 */
val GhirapurAetherGrid = card("Ghirapur Aether Grid") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Tap two untapped artifacts you control: This enchantment deals 1 damage to any target."

    activatedAbility {
        cost = Costs.TapPermanents(count = 2, filter = GameObjectFilter.Artifact)
        val t = target("target", Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Cynthia Sheppard"
        flavorText = "The city of Ghirapur is a living thing, and living things defend themselves."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e4a0c29-a759-465f-9136-9d709b6f30fc.jpg?1783938330"

        ruling("2015-06-22", "You may tap any two untapped artifacts you control, including artifact creatures that haven't been under your control continuously since the beginning of your most recent turn.")
    }
}
