package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Planar Genesis
 * {G}{U}
 * Instant
 *
 * Look at the top four cards of your library. You may put a land card from among them onto the
 * battlefield tapped. If you don't, put a card from among them into your hand. Put the rest on
 * the bottom of your library in a random order.
 *
 * "If you don't" keys off the land choice itself: when no land is put onto the battlefield
 * (declined, or none among the four), the player instead puts any one card into hand.
 */
val PlanarGenesis = card("Planar Genesis") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Look at the top four cards of your library. You may put a land card from among them " +
        "onto the battlefield tapped. If you don't, put a card from among them into your hand. Put the " +
        "rest on the bottom of your library in a random order."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(4))
            val land = chooseUpTo(
                1,
                from = looked,
                filter = GameObjectFilter.Land,
                showAllCards = true,
                prompt = "You may put a land card onto the battlefield tapped",
                selectedLabel = "Put onto the battlefield tapped",
                remainderLabel = "Don't put a land onto the battlefield"
            )
            move(land, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            val rest = exclude(looked, land)
            ifNotEmpty(land) {
                toLibraryBottom(rest, order = CardOrder.Random)
            } orElse {
                val (toHand, others) = chooseExactlySplit(
                    1,
                    from = rest,
                    prompt = "Put a card into your hand",
                    selectedLabel = "Put into your hand",
                    remainderLabel = "Put on the bottom in a random order"
                )
                toHand(toHand)
                toLibraryBottom(others, order = CardOrder.Random)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Liiga Smilshkalne"
        flavorText = "Out of nothing, something was born."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fc9a10b-c9f9-4129-a671-ced0917ce78b.jpg?1783911246"
    }
}
