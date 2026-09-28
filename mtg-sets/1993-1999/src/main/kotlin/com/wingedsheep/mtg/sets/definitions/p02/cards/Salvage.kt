package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Salvage
 * {G}
 * Sorcery
 * Put target card from your graveyard on top of your library.
 *
 * Portal Second Age is the card's earliest real-expansion printing, so the canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives here.
 */
val Salvage = card("Salvage") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put target card from your graveyard on top of your library."

    spell {
        val t = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOnTopOfLibrary(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Keith Parkinson"
        flavorText = "\"What was taken shall be restored.\"\n—Arathel, elvish queen"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3012e10-566b-447c-bb0a-dfc38c8e0fdf.jpg?1783946448"
    }
}
