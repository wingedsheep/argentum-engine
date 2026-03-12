package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dread Shade
 * {B}{B}{B}
 * Creature — Shade
 * 3/3
 * {B}: Dread Shade gets +1/+1 until end of turn.
 */
val DreadShade = card("Dread Shade") {
    manaCost = "{B}{B}{B}"
    typeLine = "Creature — Shade"
    power = 3
    toughness = 3
    oracleText = "{B}: Dread Shade gets +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "G-host Lee"
        flavorText = "\"The forest surrounding the Vess estate became the Caligo Morass, a vast bog stalked by horrors too terrible to name.\" —\"The Fall of the House of Vess\""
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46b8b1d7-9d2b-4943-bb3b-238a6333ce93.jpg?1562734973"
    }
}
