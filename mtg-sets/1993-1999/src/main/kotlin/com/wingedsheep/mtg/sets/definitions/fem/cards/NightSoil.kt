package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Night Soil
 * {G}{G}
 * Enchantment
 * {1}, Exile two creature cards from a single graveyard: Create a 1/1 green Saproling creature token.
 *
 * "A single graveyard" is what makes this more than "exile two creature cards": the pool is every
 * player's graveyard, but both cards must come out of the *same* one, so a board with one creature
 * card in each graveyard pays nothing. Each card is exiled by its own owner, not by the activating
 * player.
 */
val NightSoil = card("Night Soil") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "{1}, Exile two creature cards from a single graveyard: Create a 1/1 green Saproling creature token."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.ExileFromSingleGraveyard(count = 2, filter = GameObjectFilter.Creature)
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "71a"
        artist = "Sandra Everingham"
        flavorText = "Some said killing the Thallids only encouraged them."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cda6d18-d4b1-4b8a-a72e-f90115adf4c3.jpg?1783947886"
    }
}
