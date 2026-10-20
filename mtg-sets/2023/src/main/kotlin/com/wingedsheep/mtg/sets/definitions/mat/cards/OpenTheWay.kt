package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Open the Way
 * {X}{G}{G}
 * Sorcery
 *
 * X can't be greater than the number of players in the game.
 * Reveal cards from the top of your library until you reveal X land cards. Put those land cards
 * onto the battlefield tapped and the rest on the bottom of your library in a random order.
 *
 * The cap is `xMaximum`, read only as X is announced (the ruling: players leaving the game in
 * response don't change how many lands you find). The reveal mirrors The Ring Goes South.
 */
val OpenTheWay = card("Open the Way") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "X can't be greater than the number of players in the game.\n" +
        "Reveal cards from the top of your library until you reveal X land cards. Put those land " +
        "cards onto the battlefield tapped and the rest on the bottom of your library in a random order."

    spell {
        xMaximum = DynamicAmounts.playerCount(Player.Each)
        effect = Effects.Pipeline {
            val (_, allRevealed) = gatherUntilMatch(
                GameObjectFilter.Land,
                player = Player.You,
                count = DynamicAmounts.xValue()
            )
            reveal(allRevealed)
            move(
                allRevealed,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped),
                filter = GameObjectFilter.Land
            )
            move(
                allRevealed,
                CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Bottom),
                order = CardOrder.Random,
                filter = GameObjectFilter.Nonland
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e72a911e-8291-48ca-bfc6-8907e3b57011.jpg?1783916520"
    }
}
