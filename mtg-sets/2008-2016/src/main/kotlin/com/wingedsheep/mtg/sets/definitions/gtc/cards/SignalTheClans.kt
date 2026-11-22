package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Signal the Clans {R}{G}
 * Instant
 *
 * Search your library for three creature cards and reveal them. If you reveal three cards with
 * different names, choose one of them at random and put that card into your hand. Shuffle the
 * rest into your library.
 *
 * The searched cards never leave the library unless one is picked, so "shuffle the rest into your
 * library" is the closing shuffle. Finding fewer than three, or two sharing a name, leaves
 * [DynamicAmounts.distinctNamesIn] below three and nothing goes to hand.
 */
val SignalTheClans = card("Signal the Clans") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Instant"
    oracleText = "Search your library for three creature cards and reveal them. If you reveal three " +
        "cards with different names, choose one of them at random and put that card into your hand. " +
        "Shuffle the rest into your library."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Creature),
                search = true
            )
            val found = chooseUpTo(3, from = searchable, prompt = "Search for three creature cards")
            reveal(found)
            ifThen(Conditions.CompareAmounts(DynamicAmounts.distinctNamesIn(found), ComparisonOperator.GTE, 3)) {
                toHand(chooseRandom(1, from = found), revealed = true)
            }
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Dave Kendall"
        flavorText = "Gruul clanfolk obey few rules, but all feel bound to answer the call of the horn."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34261992-db62-49e3-95bc-1b2960868de4.jpg?1783940101"
    }
}
