package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Woodland Bellower
 * {4}{G}{G}
 * Creature — Beast
 * 6/5
 *
 * When this creature enters, you may search your library for a nonlegendary green creature card
 * with mana value 3 or less, put it onto the battlefield, then shuffle.
 *
 * The "may" gates the whole search, so declining also skips the shuffle.
 */
val WoodlandBellower = card("Woodland Bellower") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    oracleText = "When this creature enters, you may search your library for a nonlegendary green creature card " +
        "with mana value 3 or less, put it onto the battlefield, then shuffle."
    power = 6
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.nonlegendary().withColor(Color.GREEN).manaValueAtMost(3),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "207"
        artist = "Jasper Sandner"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a706d4bb-0b44-4e43-b340-7de799c086b8.jpg?1783938315"
        ruling("2015-06-22", "If a card has {X} in its mana cost, X is 0.")
    }
}
