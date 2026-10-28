package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tolarian Winds
 * {1}{U}
 * Instant
 * Discard all the cards in your hand, then draw that many cards.
 *
 * Queen Kayla bin-Kroog's discard-then-draw pipeline: the hand is gathered as the spell resolves
 * (Tolarian Winds itself is on the stack, so it isn't counted), discarded, and its size drawn.
 */
val TolarianWinds = card("Tolarian Winds") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Discard all the cards in your hand, then draw that many cards."

    spell {
        effect = Effects.Pipeline {
            val discarded = gather(CardSource.FromZone(Zone.HAND, Player.You))
            move(discarded, CardDestination.ToZone(Zone.GRAVEYARD, Player.You), moveType = MoveType.Discard)
            run(Effects.DrawCards(discarded.count))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Lawrence Snelly"
        flavorText = "Afterward, Tolaria's winds were like the whispers of lost wizards, calling for life."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c29399d-0a59-4dcb-9bd4-f31eea3f39f9.jpg?1783946353"
        ruling("2004-10-04", "It counts how many cards you discard. Since this card will not be in your hand at that time, this card is not counted.")
    }
}
