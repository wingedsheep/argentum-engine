package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Graveyard Marshal
 * {B}{B}
 * Creature — Zombie Soldier
 * 3/2
 * {2}{B}, Exile a creature card from your graveyard: Create a tapped 2/2 black Zombie creature token.
 */
val GraveyardMarshal = card("Graveyard Marshal") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Soldier"
    power = 3
    toughness = 2
    oracleText = "{2}{B}, Exile a creature card from your graveyard: Create a tapped 2/2 black Zombie creature token."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.ExileFromGraveyard(1, GameObjectFilter.Creature),
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/e/d/ed08aac1-9e82-4049-9926-618f15222024.jpg?1783934469",
        )
        description = "{2}{B}, Exile a creature card from your graveyard: Create a tapped 2/2 black Zombie creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Mark Behm"
        flavorText = "\"Rise and shine, my dears. We have work to do.\"\n—Isareth the Awakener"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dc5f62c-2d29-4a94-8501-13dc6a93c5a1.jpg?1783934570"
    }
}
