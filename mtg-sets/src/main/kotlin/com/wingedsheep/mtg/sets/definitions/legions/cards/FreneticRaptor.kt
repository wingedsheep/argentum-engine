package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlockForCreatureGroup
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Frenetic Raptor
 * {5}{R}
 * Creature — Dinosaur Beast
 * 6/6
 * Beasts can't block.
 */
val FreneticRaptor = card("Frenetic Raptor") {
    manaCost = "{5}{R}"
    typeLine = "Creature — Dinosaur Beast"
    power = 6
    toughness = 6
    oracleText = "Beasts can't block."

    staticAbility {
        ability = CantBlockForCreatureGroup(
            GroupFilter(GameObjectFilter.Creature.withSubtype("Beast"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Daren Bader"
        flavorText = "How do you stop a raptor from charging? No, seriously! Help! —Blarg, goblin jester"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f6bc3c0-2d6e-4a09-84c4-b26a352186bb.jpg?1562923949"
    }
}
