package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dream Harvest
 * {5}{U/B}{U/B}
 * Sorcery
 *
 * Each opponent exiles cards from the top of their library until they have
 * exiled cards with total mana value 5 or greater this way. Until end of turn,
 * you may cast cards exiled this way without paying their mana costs.
 */
val DreamHarvest = card("Dream Harvest") {
    manaCost = "{5}{U/B}{U/B}"
    typeLine = "Sorcery"
    oracleText = "Each opponent exiles cards from the top of their library until they have exiled cards with total mana value 5 or greater this way. Until end of turn, you may cast cards exiled this way without paying their mana costs."

    spell {
        effect = CompositeEffect(
            listOf(
                Effects.ExileLibraryUntilManaValue(
                    players = Player.EachOpponent,
                    threshold = 5,
                    storeAs = "exiled"
                ),
                GrantMayPlayFromExileEffect("exiled"),
                GrantPlayWithoutPayingCostEffect("exiled")
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "216"
        artist = "Ben Hill"
        flavorText = "Faeries swarm to snoring boggarts like vultures to rotting meat."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4ebc0e7-02d7-4d38-9376-a39963e6d3fa.jpg?1767952434"
        ruling("2025-11-17", "If a card in a library has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
        ruling("2025-11-17", "Since you are using an alternative cost to cast the spells, you can't pay any other alternative costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory additional costs, you must pay those.")
        ruling("2025-11-17", "If a spell you cast has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
