package com.wingedsheep.mtg.sets.definitions.ice.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Brainstorm
 * {U}
 * Instant
 * Draw three cards, then put two cards from your hand on top of your library in any order.
 *
 * Draw + put-back is one atomic resolution — the Gather → Select → Move pipeline composes with
 * the draw so both halves happen without a priority window between them (matches Harmonized
 * Trio's "Brainstorm" prepare spell, see [com.wingedsheep.mtg.sets.definitions.sos.cards.HarmonizedTrio]).
 */
val Brainstorm = card("Brainstorm") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw three cards, then put two cards from your hand on top of your library in any order."
    spell {
        effect = Effects.Composite(
            Effects.DrawCards(3),
            Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
                val putBack = chooseExactly(2, hand)
                toLibraryTop(putBack)
            },
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Christopher Rush"
        flavorText = "\"I reeled from the blow, and then suddenly, I knew exactly what to do. Within moments, victory was mine.\"\n—Gustha Ebbasdotter,\nKjeldoran Royal Mage"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d42d7aa-7f53-4cfc-842a-086aab2448d1.jpg?1783947518"
    }
}
