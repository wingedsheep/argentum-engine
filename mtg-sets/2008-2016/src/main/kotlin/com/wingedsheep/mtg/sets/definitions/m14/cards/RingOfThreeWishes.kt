package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ring of Three Wishes
 * {5}
 * Artifact
 * This artifact enters with three wish counters on it.
 * {5}, {T}, Remove a wish counter from this artifact: Search your library for a card, put that card
 * into your hand, then shuffle.
 */
val RingOfThreeWishes = card("Ring of Three Wishes") {
    manaCost = "{5}"
    typeLine = "Artifact"
    oracleText = "This artifact enters with three wish counters on it.\n" +
        "{5}, {T}, Remove a wish counter from this artifact: Search your library for a card, put that card into your hand, then shuffle."

    replacementEffect(EntersWithCounters(counterType = CounterType.WISH, count = 3, selfOnly = true))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.WISH, 1))
        effect = Patterns.Library.searchLibrary(filter = GameObjectFilter.Any, destination = SearchDestination.HAND)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "216"
        artist = "Mark Winters"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/219ab03a-2b3b-4eef-8a42-2cbe793d2f33.jpg?1783939894"
    }
}
