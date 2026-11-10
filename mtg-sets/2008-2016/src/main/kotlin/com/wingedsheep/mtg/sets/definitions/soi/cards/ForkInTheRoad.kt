package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fork in the Road
 * {1}{G}
 * Sorcery
 * Search your library for up to two basic land cards and reveal them. Put one into your hand
 * and the other into your graveyard. Then shuffle.
 *
 * Cultivate's shape: pick up to two basics, then pick which found card goes to hand — the
 * remainder goes to the graveyard. If only one basic is found, it is the one put into hand.
 */
val ForkInTheRoad = card("Fork in the Road") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to two basic land cards and reveal them. Put one into your hand and the other into your graveyard. Then shuffle."
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
            val (toHandCards, toGraveyardCards) = chooseExactlySplit(
                1,
                from = found,
                selectedLabel = "Into your hand",
                remainderLabel = "Into your graveyard",
                prompt = "Choose which basic land goes to your hand; the other goes to your graveyard."
            )
            toHand(toHandCards, revealed = true)
            toGraveyard(toGraveyardCards)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "205"
        artist = "Jung Park"
        flavorText = "\"This is as far as I go.\"\n—Beckram, Skiltfolk escort"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a7ad701-65c3-494a-8986-b4ea1bab46bc.jpg?1783937731"
    }
}
