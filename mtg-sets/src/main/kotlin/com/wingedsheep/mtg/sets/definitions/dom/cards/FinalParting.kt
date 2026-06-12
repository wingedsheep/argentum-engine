package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects

/**
 * Final Parting
 * {3}{B}{B}
 * Sorcery
 *
 * Search your library for two cards. Put one into your hand and the other
 * into your graveyard. Then shuffle.
 */
val FinalParting = card("Final Parting") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Search your library for two cards. Put one into your hand and the other into your graveyard. Then shuffle."

    spell {
        effect = Effects.Pipeline {
            // Gather all cards from library
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any),
                name = "searchable"
            )
            // Select exactly 2 cards
            val found = chooseUpTo(
                2, from = searchable,
                prompt = "Search your library for two cards",
                name = "found"
            )
            // From the 2 selected, choose 1 to put in hand (remainder goes to graveyard)
            val (toHand, toGraveyard) = chooseExactlySplit(
                1, from = found,
                prompt = "Choose a card to put into your hand",
                name = "toHand",
                remainderName = "toGraveyard"
            )
            // Move chosen card to hand
            move(
                toHand,
                destination = CardDestination.ToZone(Zone.HAND)
            )
            // Move the other to graveyard
            move(
                toGraveyard,
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            )
            // Shuffle library
            run(ShuffleLibraryEffect())
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Eric Deschamps"
        flavorText = "\"Sleep now, brother. That is the one gift I can give you.\" — Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de8803f6-9efa-4323-b8c5-29bdd5a48f9a.jpg?1562744124"
    }
}
