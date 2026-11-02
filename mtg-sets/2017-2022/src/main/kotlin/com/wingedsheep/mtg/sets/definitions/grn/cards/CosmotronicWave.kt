package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Cosmotronic Wave
 * {3}{R}
 * Sorcery
 * Cosmotronic Wave deals 1 damage to each creature your opponents control.
 * Creatures your opponents control can't block this turn.
 */
val CosmotronicWave = card("Cosmotronic Wave") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Cosmotronic Wave deals 1 damage to each creature your opponents control. Creatures your opponents control can't block this turn."

    spell {
        effect = Patterns.Group.dealDamageToAll(1, GroupFilter.AllCreaturesOpponentsControl) then
            Effects.CantBlockGroup(GroupFilter.AllCreaturesOpponentsControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Titus Lunter"
        flavorText = "\"This sort of innovation occurs when you set out to improve an electric tea kettle and misplace a decimal.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69c5bafa-8cd8-4158-98e0-46dc74c027c0.jpg?1783934165"
    }
}
