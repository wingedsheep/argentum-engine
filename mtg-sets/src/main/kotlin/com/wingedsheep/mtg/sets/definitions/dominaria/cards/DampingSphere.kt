package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DampLandManaProduction
import com.wingedsheep.sdk.scripting.IncreaseSpellCostByPlayerSpellsCast

/**
 * Damping Sphere
 * {2}
 * Artifact
 * If a land is tapped for two or more mana, it produces {C} instead of any other type and amount.
 * Each spell a player casts costs {1} more to cast for each other spell that player has cast this turn.
 */
val DampingSphere = card("Damping Sphere") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "If a land is tapped for two or more mana, it produces {C} instead of any other type and amount.\n" +
        "Each spell a player casts costs {1} more to cast for each other spell that player has cast this turn."

    staticAbility {
        ability = DampLandManaProduction
    }

    staticAbility {
        ability = IncreaseSpellCostByPlayerSpellsCast()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Adam Paquette"
        flavorText = "A Thran relic, it has spent ten thousand years doing absolutely nothing."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5c7d16b-8f4e-42b9-be24-3cb091932d7c.jpg?1562740759"
    }
}
