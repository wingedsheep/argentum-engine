package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Blazing Volley — Amonkhet #119
 * {R} · Sorcery
 *
 * Blazing Volley deals 1 damage to each creature your opponents control.
 */
val BlazingVolley = card("Blazing Volley") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Blazing Volley deals 1 damage to each creature your opponents control."

    spell {
        effect = Patterns.Group.dealDamageToAll(1, GroupFilter.AllCreaturesOpponentsControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Zezhou Chen"
        flavorText = "The arrows fell like fiery rain, and for a moment, the sky itself seemed to burn."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba450179-4591-4e8a-b6ca-66cbef1817f2.jpg?1783936495"
    }
}
