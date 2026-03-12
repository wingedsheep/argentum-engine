package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBySubtype
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Juggernaut
 * {4}
 * Artifact Creature — Juggernaut
 * 5/3
 * Juggernaut attacks each combat if able.
 * Juggernaut can't be blocked by Walls.
 */
val Juggernaut = card("Juggernaut") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Juggernaut"
    power = 5
    toughness = 3
    oracleText = "Juggernaut attacks each combat if able.\nJuggernaut can't be blocked by Walls."

    staticAbility {
        ability = MustAttack()
    }

    staticAbility {
        ability = CantBeBlockedBySubtype("Wall")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Jonas De Ro"
        flavorText = "\"Urza's machines have a splendid habit of excavating themselves.\" —Rona, disciple of Gix"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfebeab7-44cf-4895-bdd5-04cbb2c700d7.jpg?1562744216"
    }
}
