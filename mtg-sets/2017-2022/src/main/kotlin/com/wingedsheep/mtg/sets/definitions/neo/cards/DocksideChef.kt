package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Dockside Chef — Kamigawa: Neon Dynasty #93 (canonical printing)
 * {B} · Enchantment Creature — Human Citizen · 1/2
 *
 * {1}{B}, Sacrifice an artifact or creature: Draw a card.
 */
val DocksideChef = card("Dockside Chef") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Human Citizen"
    power = 1
    toughness = 2
    oracleText = "{1}{B}, Sacrifice an artifact or creature: Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Sacrifice(GameObjectFilter.Artifact or GameObjectFilter.Creature))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Steven Belledin"
        flavorText = "The squirming is how you know it's fresh."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d80100c3-c81e-4084-8dfe-f8610637fd91.jpg?1783923888"
    }
}
