package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Rampart Smasher — Throne of Eldraine #213 (canonical printing)
 * {R/G}{R/G}{R/G}{R/G}
 * Creature — Giant
 * 5/5
 *
 * This creature can't be blocked by Knights or Walls.
 */
val RampartSmasher = card("Rampart Smasher") {
    manaCost = "{R/G}{R/G}{R/G}{R/G}"
    colorIdentity = "RG"
    typeLine = "Creature — Giant"
    oracleText = "This creature can't be blocked by Knights or Walls."
    power = 5
    toughness = 5

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withAnySubtype("Knight", "Wall"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Emrah Elmasli"
        flavorText = "\"He has strength and courage—just way, way too much of both.\"\n" +
            "—Syr Faren, the Hengehammer"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fb9a632-a56b-48fb-bc24-14572b6a8a55.jpg?1783932589"
    }
}
