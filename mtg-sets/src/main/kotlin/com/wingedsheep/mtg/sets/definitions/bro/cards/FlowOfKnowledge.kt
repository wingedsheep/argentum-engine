package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Flow of Knowledge
 * {4}{U}
 * Instant
 * Draw a card for each Island you control, then discard two cards.
 */
val FlowOfKnowledge = card("Flow of Knowledge") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw a card for each Island you control, then discard two cards."

    spell {
        effect = Effects.Pipeline {
            // Draw a card for each Island you control
            run(Effects.DrawCards(DynamicAmounts.landsWithSubtype(Subtype("Island"))))
            // Discard two cards: gather hand → select two → move to graveyard
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You), name = "hand")
            val discarded = chooseExactly(2, from = hand, prompt = "Choose two cards to discard", name = "discarded")
            move(discarded, CardDestination.ToZone(Zone.GRAVEYARD, Player.You), moveType = MoveType.Discard)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Tuan Duong Chu"
        flavorText = "\"Focus your mind. Do you hold a piece of the ocean or a thousand drops of rain?\"\n—Hurkyl"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ea316f3-4a68-4cd4-a388-da9d0455d0a9.jpg?1674420499"
    }
}
