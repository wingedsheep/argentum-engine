package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thraxodemon
 * {1}{B}
 * Creature — Demon
 * 2/2
 * {3}, {T}, Sacrifice another creature or artifact: Draw a card.
 */
val Thraxodemon = card("Thraxodemon") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 2
    toughness = 2
    oracleText = "{3}, {T}, Sacrifice another creature or artifact: Draw a card."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.CreatureOrArtifact)
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Nino Is"
        flavorText = "Urza and Mishra weren't the only denizens of Terisiare to covet the powerstones' energy."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc59ca3b-d417-4cf2-99a3-89816bf2bd09.jpg"
    }
}
