package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Enter the Infinite
 * {8}{U}{U}{U}{U}
 * Sorcery
 *
 * Draw cards equal to the number of cards in your library, then put a card from your hand on top
 * of your library. You have no maximum hand size until your next turn.
 */
val EnterTheInfinite = card("Enter the Infinite") {
    manaCost = "{8}{U}{U}{U}{U}"
    typeLine = "Sorcery"
    colorIdentity = "U"
    oracleText = "Draw cards equal to the number of cards in your library, then put a card from your hand " +
        "on top of your library. You have no maximum hand size until your next turn."

    spell {
        effect = Effects.DrawCards(DynamicAmounts.zone(Player.You, Zone.LIBRARY).count()) then
            Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
                val putBack = chooseExactly(1, hand, prompt = "Choose a card to put on top of your library")
                toLibraryTop(putBack)
            } then
            Effects.RemoveMaximumHandSize(duration = Duration.UntilYourNextTurn)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "34"
        artist = "Terese Nielsen"
        flavorText = "\"Don't just have an idea—have all of them.\"\n—Niv-Mizzet"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/612beb8f-2ab1-4a8b-84c5-c47d19d400ab.jpg?1783940138"
    }
}
