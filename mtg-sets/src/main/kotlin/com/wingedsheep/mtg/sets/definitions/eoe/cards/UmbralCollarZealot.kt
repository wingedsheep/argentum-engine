package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Umbral Collar Zealot
 * {1}{B}
 * Creature — Human Cleric
 * Sacrifice another creature or artifact: Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 * 3/2
 */
val UmbralCollarZealot = card("Umbral Collar Zealot") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 2
    oracleText = "Sacrifice another creature or artifact: Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature.or(GameObjectFilter.Artifact))
        effect = LibraryPatterns.surveil(1)
        description = "Sacrifice another creature or artifact: Surveil 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Dmitry Burmak"
        flavorText = "\"All things will fall to the Zero Point. By accepting this truth, my path is known and my fate is certain.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbcc1d84-9772-475d-924a-75bb54c9bc20.jpg?1752947051"
    }
}
