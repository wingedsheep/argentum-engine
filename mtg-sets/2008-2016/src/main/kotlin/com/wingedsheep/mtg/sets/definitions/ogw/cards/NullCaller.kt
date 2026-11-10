package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Null Caller
 * {3}{B}
 * Creature — Vampire Shaman
 * 2/4
 * {3}{B}, Exile a creature card from your graveyard: Create a tapped 2/2 black Zombie creature token.
 */
val NullCaller = card("Null Caller") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Shaman"
    power = 2
    toughness = 4
    oracleText = "{3}{B}, Exile a creature card from your graveyard: Create a tapped 2/2 black Zombie creature token."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{B}"),
            Costs.ExileFromGraveyard(1, GameObjectFilter.Creature),
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            tapped = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Darek Zabrocki"
        flavorText = "Zendikar's defenders include both healers and vampires. One way or another, the fallen will return to the fight."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4afe773b-2987-4001-a0fd-9dca414a9129.jpg"
    }
}
