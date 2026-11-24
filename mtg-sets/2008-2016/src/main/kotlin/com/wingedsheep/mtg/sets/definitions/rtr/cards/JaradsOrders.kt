package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jarad's Orders
 * {2}{B}{G}
 * Sorcery
 * Search your library for up to two creature cards and reveal them. Put one into your hand
 * and the other into your graveyard. Then shuffle.
 *
 * Fork in the Road's shape: pick up to two creatures, then pick which found card goes to hand —
 * the remainder goes to the graveyard. If only one creature is found, it is the one put into hand.
 */
val JaradsOrders = card("Jarad's Orders") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to two creature cards and reveal them. Put one into your hand and the other into your graveyard. Then shuffle."
    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Creature),
                search = true
            )
            val found = chooseUpTo(
                2,
                from = searchable,
                prompt = "Search your library for up to two creature cards"
            )
            val (toHandCards, toGraveyardCards) = chooseExactlySplit(
                1,
                from = found,
                selectedLabel = "Into your hand",
                remainderLabel = "Into your graveyard",
                prompt = "Choose which creature card goes to your hand; the other goes to your graveyard."
            )
            toHand(toHandCards, revealed = true)
            toGraveyard(toGraveyardCards)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Svetlin Velinov"
        flavorText = "\"The Izzet are searching for something. Discern what or die trying.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c59171ce-7dc6-4dd9-a124-3c2c3028d93d.jpg?1783940337"
    }
}
