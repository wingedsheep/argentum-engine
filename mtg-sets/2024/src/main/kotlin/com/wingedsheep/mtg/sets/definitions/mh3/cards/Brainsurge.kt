package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Brainsurge
 * {2}{U}
 * Instant
 * Draw four cards, then put two cards from your hand on top of your library in any order.
 *
 * Same shape as Brainstorm: the draw and the put-back resolve atomically, and the two cards
 * are chosen from the whole hand (2024-06-07 ruling), then ordered on top.
 */
val Brainsurge = card("Brainsurge") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw four cards, then put two cards from your hand on top of your library in any order."
    spell {
        effect = Effects.DrawCards(4) then
            Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
                val putBack = chooseExactly(2, hand)
                toLibraryTop(putBack)
            }
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Liiga Smilshkalne"
        flavorText = "In the Cosmos, secrets swirl and spark like windborne embers, and only Alrund can capture them."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed48f805-b57c-4d7f-a3c2-d16ae71bce2d.jpg?1783911293"
        ruling("2024-06-07", "The two cards you put on top of your library can be from the four you just drew or ones that were already in your hand.")
    }
}
