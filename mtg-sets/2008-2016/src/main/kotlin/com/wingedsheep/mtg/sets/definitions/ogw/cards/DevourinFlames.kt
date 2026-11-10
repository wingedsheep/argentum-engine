package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Devour in Flames
 * {2}{R}
 * Sorcery
 * As an additional cost to cast this spell, return a land you control to its owner's hand.
 * Devour in Flames deals 5 damage to target creature or planeswalker.
 */
val DevourinFlames = card("Devour in Flames") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, return a land you control to its owner's hand.\n" +
        "Devour in Flames deals 5 damage to target creature or planeswalker."

    additionalCost(Costs.additional.ReturnToHand(GameObjectFilter.Land))

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(5, victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Svetlin Velinov"
        flavorText = "Ob Nixilis had prepared for every eventuality—except Chandra Nalaar."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/432dd78b-9520-4c28-8699-d7ff9c477070.jpg"
    }
}
