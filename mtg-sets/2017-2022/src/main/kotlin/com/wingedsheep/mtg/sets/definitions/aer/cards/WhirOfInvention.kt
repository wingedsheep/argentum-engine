package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Whir of Invention
 * {X}{U}{U}{U}
 * Instant
 *
 * Improvise
 * Search your library for an artifact card with mana value X or less, put it onto the
 * battlefield, then shuffle.
 *
 * The Chord of Calling shape with improvise in place of convoke: improvise pays generic mana of
 * the total cost, the announced X included (the card's own ruling).
 */
val WhirOfInvention = card("Whir of Invention") {
    manaCost = "{X}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\n" +
        "Search your library for an artifact card with mana value X or less, put it onto the " +
        "battlefield, then shuffle."

    keywords(Keyword.IMPROVISE)

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.manaValueAtMostX(),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Christine Choi"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/0279fd3c-9252-4958-9d7a-5f33aa25907e.jpg?1783936767"
        ruling("2017-02-09", "When using improvise to cast a spell with {X} in its mana cost, first choose the value for X. That choice, plus any cost increases or decreases, will determine the spell's total cost. Then you can tap artifacts you control to help pay that cost. For example, if you cast Whir of Invention (a spell with improvise and mana cost {X}{U}{U}{U}) and choose X to be 3, the total cost is {3}{U}{U}{U}. If you tap two artifacts, you'll have to pay {1}{U}{U}{U}.")
        ruling("2017-02-09", "Improvise can't pay for {W}, {U}, {B}, {R}, {G}, or {C} mana symbols in a spell's total cost.")
        ruling("2017-02-09", "Improvise doesn't change a spell's mana cost or mana value.")
    }
}
