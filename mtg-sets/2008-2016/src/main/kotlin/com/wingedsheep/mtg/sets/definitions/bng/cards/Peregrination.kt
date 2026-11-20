package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Peregrination
 * {3}{G}
 * Sorcery
 * Search your library for up to two basic land cards, reveal those cards, and put one onto the
 * battlefield tapped and the other into your hand. Shuffle, then scry 1.
 *
 * Cultivate's search-and-split pipeline (pick up to two basics, then pick the one that enters
 * tapped; the remainder goes to hand), followed by scry 1 after the shuffle.
 */
val Peregrination = card("Peregrination") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to two basic land cards, reveal those cards, and put one onto the " +
        "battlefield tapped and the other into your hand. Shuffle, then scry 1. (Look at the top card of your " +
        "library. You may put that card on the bottom.)"
    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                search = true
            )
            val found = chooseUpTo(
                2,
                from = searchable,
                prompt = "Search your library for up to two basic land cards"
            )
            val (toBattlefield, toHandCards) = chooseExactlySplit(
                1,
                from = found,
                selectedLabel = "Onto the battlefield tapped",
                remainderLabel = "Into your hand",
                prompt = "Choose which basic land enters the battlefield tapped; the other goes to your hand."
            )
            move(
                toBattlefield,
                CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                revealed = true
            )
            toHand(toHandCards, revealed = true)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        } then Effects.Scry(1)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03835e3e-9fd1-47c7-86a5-a5481420f2d3.jpg?1783939532"
        ruling(
            "2014-02-01",
            "You can choose to search for only one basic land card with Peregrination. If you do, you'll put that card onto the battlefield tapped."
        )
    }
}
