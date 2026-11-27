package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Amass the Components
 * {3}{U}
 * Sorcery
 * Draw three cards, then put a card from your hand on the bottom of your library.
 *
 * Brainstorm's shape with the put-back sent to the bottom: the draw and the Gather → Select →
 * Move pipeline resolve as one, so the card is chosen from the hand *after* the three draws.
 */
val AmassTheComponents = card("Amass the Components") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw three cards, then put a card from your hand on the bottom of your library."

    spell {
        effect = Effects.DrawCards(3) then
            Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
                val putBack = chooseExactly(1, hand)
                toLibraryBottom(putBack)
            }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Matt Stewart"
        flavorText = "\"I knew I'd find a use for all that newt spittle!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5b48d60-fc99-4d21-9293-4f7ce1c02928.jpg?1783940723"

        ruling(
            "2017-11-17",
            "If you cast Amass the Components with fewer than three cards in your library, you'll draw the " +
                "remaining cards in your library, put a card from your hand on the bottom of your library, " +
                "then lose the game for drawing a card from a library with no cards in it the next time " +
                "state-based actions are performed."
        )
    }
}
