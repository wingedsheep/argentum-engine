package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Slagdrill Scrapper
 * {R}
 * Artifact Creature — Robot Scout
 * {2}, {T}, Sacrifice another artifact or land: Draw a card.
 */
val SlagdrillScrapper = card("Slagdrill Scrapper") {
    manaCost = "{R}"
    typeLine = "Artifact Creature — Robot Scout"
    oracleText = "{2}, {T}, Sacrifice another artifact or land: Draw a card."
    power = 1
    toughness = 2

    // Activated ability: {2}, {T}, Sacrifice another artifact or land: Draw a card
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeAnother(
                GameObjectFilter.Artifact.or(GameObjectFilter.Land)
            )
        )
        
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "160"
        artist = "Edgar Sánchez Hidalgo"
        flavorText = "\"Who cares if it rips the asteroid apart? We got what we needed.\"\n—Viya, Kav moxite baron"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/155dcf54-8fdb-4715-97dc-4eb5d3d80d78.jpg?1752947202"
    }
}
